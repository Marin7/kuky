package com.kuky.backend.learning;

import org.junit.jupiter.api.AfterEach;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class LearningControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private String testEmail;
    private UUID testAssignmentId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        testEmail = "test-learning-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (gen_random_uuid(), ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                testEmail);
        testAssignmentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO homework_assignments (id, title, instructions, published, format, sort_order) VALUES (?, 'Test assignment', 'Do the thing', true, 'MANUAL', 0)",
                testAssignmentId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update(
                "DELETE FROM homework_submissions WHERE assignment_id = ?",
                testAssignmentId);
        jdbcTemplate.update(
                "DELETE FROM homework_targets WHERE assignment_id = ?",
                testAssignmentId);
        jdbcTemplate.update("DELETE FROM homework_assignments WHERE id = ?", testAssignmentId);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", testEmail);
    }

    private static UsernamePasswordAuthenticationToken principal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    @Test
    void getOverview_notAuthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/learning"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOverview_authenticated_returnsStructure() throws Exception {
        mockMvc.perform(get("/api/v1/learning").with(authentication(principal(testEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentation").isArray())
                .andExpect(jsonPath("$.pastClasses").isArray())
                .andExpect(jsonPath("$.homework").isArray());
    }

    @Test
    void submitHomework_authenticated_flipsStatusToSubmitted() throws Exception {
        // Make the assignment visible to this student via homework_targets
        jdbcTemplate.update(
                "INSERT INTO homework_targets (assignment_id, user_id) SELECT ?, id FROM users WHERE email = ?",
                testAssignmentId, testEmail);

        mockMvc.perform(put("/api/v1/learning/homework/" + testAssignmentId)
                        .with(authentication(principal(testEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\":\"Mi respuesta de prueba.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.response").value("Mi respuesta de prueba."));

        // Persisted: a second GET shows the submitted status
        mockMvc.perform(get("/api/v1/learning").with(authentication(principal(testEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homework[?(@.id=='" + testAssignmentId + "')].status").value("SUBMITTED"));
    }

    @Test
    void submitHomework_unknownAssignment_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/learning/homework/" + UUID.randomUUID())
                        .with(authentication(principal(testEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ASSIGNMENT_NOT_FOUND"));
    }

    @Test
    void submitHomework_notAuthenticated_isRejected() throws Exception {
        mockMvc.perform(put("/api/v1/learning/homework/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
