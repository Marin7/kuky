package com.kuky.backend.admin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Covers the new teacher-review endpoints added to the existing HomeworkAdminController. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class HomeworkAdminControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private String adminEmail;
    private String studentEmail;
    private UUID studentId;
    private UUID assignmentId;
    private UUID submissionId;

    private static UsernamePasswordAuthenticationToken adminPrincipal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static UsernamePasswordAuthenticationToken studentPrincipal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        adminEmail = "admin-" + UUID.randomUUID() + "@kuky.es";
        studentEmail = "student-" + UUID.randomUUID() + "@example.com";

        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (gen_random_uuid(), ?, 'hash', 'ACTIVE', 'ADMIN', true)",
                adminEmail);

        studentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                studentId, studentEmail);

        assignmentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO homework_assignments (id, title, instructions, published, format, sort_order) VALUES (?, 'Escritura', 'Escribe algo', true, 'MANUAL', 0)",
                assignmentId);
        jdbcTemplate.update(
                "INSERT INTO homework_targets (id, assignment_id, user_id) VALUES (gen_random_uuid(), ?, ?)",
                assignmentId, studentId);

        submissionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_submissions (id, user_id, assignment_id, status, response_text, submitted_at, updated_at)
                VALUES (?, ?, ?, 'SUBMITTED', '[{"text":"Mi respuesta"}]', NOW(), NOW())
                """, submissionId, studentId, assignmentId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM homework_submissions WHERE assignment_id = ?", assignmentId);
        jdbcTemplate.update("DELETE FROM homework_targets WHERE assignment_id = ?", assignmentId);
        jdbcTemplate.update("DELETE FROM homework_assignments WHERE id = ?", assignmentId);
        jdbcTemplate.update("DELETE FROM users WHERE email IN (?, ?)", adminEmail, studentEmail);
    }

    @Test
    void reviewQueue_requiresAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/homework/submissions")
                        .with(authentication(studentPrincipal(studentEmail))))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewQueue_listsSubmittedManualSubmission() throws Exception {
        mockMvc.perform(get("/api/v1/admin/homework/submissions")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.submissionId=='" + submissionId + "')]").exists());
    }

    @Test
    void submissionDetail_returnsStudentAnswer() throws Exception {
        mockMvc.perform(get("/api/v1/admin/homework/submissions/" + submissionId)
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.response[0].text").value("Mi respuesta"));
    }

    @Test
    void submissionDetail_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/homework/submissions/" + UUID.randomUUID())
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SUBMISSION_NOT_FOUND"));
    }

    @Test
    void saveFeedback_happyPath_transitionsToReviewedAndLeavesQueue() throws Exception {
        mockMvc.perform(put("/api/v1/admin/homework/submissions/" + submissionId + "/feedback")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":[{\"text\":\"Muy bien\",\"color\":\"red\",\"highlight\":\"yellow\",\"strike\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWED"))
                .andExpect(jsonPath("$.feedback[0].text").value("Muy bien"))
                .andExpect(jsonPath("$.feedback[0].color").value("red"))
                .andExpect(jsonPath("$.feedback[0].highlight").value("yellow"))
                .andExpect(jsonPath("$.feedback[0].strike").value(true));

        mockMvc.perform(get("/api/v1/admin/homework/submissions")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.submissionId=='" + submissionId + "')]").doesNotExist());

        String reviewedAt = jdbcTemplate.queryForObject(
                "SELECT reviewed_at::text FROM homework_submissions WHERE id = ?", String.class, submissionId);
        Assertions.assertNotNull(reviewedAt);
    }

    @Test
    void saveFeedback_emptySegments_returns422() throws Exception {
        mockMvc.perform(put("/api/v1/admin/homework/submissions/" + submissionId + "/feedback")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":[]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void saveFeedback_alreadyReviewed_returns409() throws Exception {
        jdbcTemplate.update(
                "UPDATE homework_submissions SET status = 'REVIEWED', feedback = '[{\"text\":\"ya\"}]' WHERE id = ?",
                submissionId);

        mockMvc.perform(put("/api/v1/admin/homework/submissions/" + submissionId + "/feedback")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":[{\"text\":\"otra vez\"}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_REVIEWED"));
    }

    @Test
    void saveFeedback_notYetSubmitted_returns409() throws Exception {
        jdbcTemplate.update("UPDATE homework_submissions SET status = 'PENDING' WHERE id = ?", submissionId);

        mockMvc.perform(put("/api/v1/admin/homework/submissions/" + submissionId + "/feedback")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":[{\"text\":\"todavia no\"}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NOT_SUBMITTED"));
    }

    @Test
    void saveFeedback_requiresAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/admin/homework/submissions/" + submissionId + "/feedback")
                        .with(authentication(studentPrincipal(studentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":[{\"text\":\"x\"}]}"))
                .andExpect(status().isForbidden());
    }
}
