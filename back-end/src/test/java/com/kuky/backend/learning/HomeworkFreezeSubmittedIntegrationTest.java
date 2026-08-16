package com.kuky.backend.learning;

import com.kuky.backend.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomeworkFreezeSubmittedIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private String adminEmail;
    private String studentEmail;
    private UUID studentId;
    private UUID pendingStudentId;
    private String pendingEmail;
    private UUID assignmentId;
    private UUID questionId;
    private UUID optionCorrectId;
    private UUID optionWrongId;
    private UUID optionExtraId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        adminEmail = "admin-freeze-" + UUID.randomUUID() + "@kuky.es";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (gen_random_uuid(), ?, 'hash', 'ACTIVE', 'ADMIN', true)",
                adminEmail);

        studentId = UUID.randomUUID();
        studentEmail = "student-freeze-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                studentId, studentEmail);

        pendingStudentId = UUID.randomUUID();
        pendingEmail = "pending-freeze-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                pendingStudentId, pendingEmail);

        assignmentId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_assignments (id, title, instructions, published, format, homework_type, sort_order, content_revised_at)
                VALUES (?, 'Gramática', 'Elige', true, 'EXERCISE', 'READ', 0, ?)
                """, assignmentId, Timestamp.from(Instant.parse("2026-08-15T10:00:00Z")));

        questionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_questions (id, assignment_id, position, kind, prompt, structure_json)
                VALUES (?, ?, 0, 'SINGLE_CHOICE', '¿Capital?', '{}'::jsonb)
                """, questionId, assignmentId);

        optionCorrectId = UUID.randomUUID();
        optionWrongId = UUID.randomUUID();
        optionExtraId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_question_options (id, question_id, position, label, is_correct)
                VALUES (?, ?, 0, 'Madrid', true)
                """, optionCorrectId, questionId);
        jdbcTemplate.update("""
                INSERT INTO homework_question_options (id, question_id, position, label, is_correct)
                VALUES (?, ?, 1, 'Lisboa', false)
                """, optionWrongId, questionId);
        jdbcTemplate.update("""
                INSERT INTO homework_question_options (id, question_id, position, label, is_correct)
                VALUES (?, ?, 2, 'París', false)
                """, optionExtraId, questionId);

        jdbcTemplate.update(
                "INSERT INTO homework_targets (assignment_id, user_id) VALUES (?, ?)",
                assignmentId, studentId);
        jdbcTemplate.update(
                "INSERT INTO homework_targets (assignment_id, user_id) VALUES (?, ?)",
                assignmentId, pendingStudentId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM homework_submissions WHERE assignment_id = ?", assignmentId);
        jdbcTemplate.update("DELETE FROM homework_targets WHERE assignment_id = ?", assignmentId);
        jdbcTemplate.update("DELETE FROM homework_assignments WHERE id = ?", assignmentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", pendingStudentId);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", adminEmail);
    }

    @Test
    void submitWritesSnapshot_liveEditDoesNotChangeResult_staleTokenConflicts() throws Exception {
        String token = Instant.parse("2026-08-15T10:00:00Z").toString();

        mockMvc.perform(put("/api/v1/learning/homework/" + assignmentId + "/answers")
                        .with(authentication(student()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":"%s","selectedOptionIds":["%s"]}],"contentRevisedAt":"%s"}
                                """.formatted(questionId, optionCorrectId, token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRADED"))
                .andExpect(jsonPath("$.scorePercent").value(100));

        String snapshot = jdbcTemplate.queryForObject(
                "SELECT assignment_snapshot::text FROM homework_submissions WHERE assignment_id = ? AND user_id = ?",
                String.class, assignmentId, studentId);
        assertNotNull(snapshot);
        Integer score = jdbcTemplate.queryForObject(
                "SELECT score_percent FROM homework_submissions WHERE assignment_id = ? AND user_id = ?",
                Integer.class, assignmentId, studentId);
        assertEquals(100, score);

        mockMvc.perform(put("/api/v1/admin/homework/" + assignmentId)
                        .with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(liveEditBody("¿Capital de Francia?", "París", optionExtraId)))
                .andExpect(status().isOk());

        String snapshotAfter = jdbcTemplate.queryForObject(
                "SELECT assignment_snapshot::text FROM homework_submissions WHERE assignment_id = ? AND user_id = ?",
                String.class, assignmentId, studentId);
        assertEquals(snapshot, snapshotAfter);
        Integer scoreAfter = jdbcTemplate.queryForObject(
                "SELECT score_percent FROM homework_submissions WHERE assignment_id = ? AND user_id = ?",
                Integer.class, assignmentId, studentId);
        assertEquals(100, scoreAfter);

        Boolean extraRetired = jdbcTemplate.queryForObject(
                "SELECT retired FROM homework_question_options WHERE id = ?",
                Boolean.class, optionCorrectId);
        assertTrue(extraRetired);

        Integer linked = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM homework_answer_options ao
                JOIN homework_answers a ON a.id = ao.answer_id
                JOIN homework_submissions s ON s.id = a.submission_id
                WHERE s.assignment_id = ? AND s.user_id = ? AND ao.option_id = ?
                """, Integer.class, assignmentId, studentId, optionCorrectId);
        assertEquals(1, linked);

        mockMvc.perform(get("/api/v1/learning/homework/" + assignmentId)
                        .with(authentication(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].prompt").value("¿Capital?"))
                .andExpect(jsonPath("$.scorePercent").value(100))
                .andExpect(jsonPath("$.contentRevisedAt").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(get("/api/v1/learning/homework/" + assignmentId)
                        .with(authentication(pendingStudent())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].prompt").value("¿Capital de Francia?"))
                .andExpect(jsonPath("$.contentRevisedAt").isNotEmpty());

        mockMvc.perform(put("/api/v1/learning/homework/" + assignmentId + "/answers")
                        .with(authentication(pendingStudent()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":"%s","selectedOptionIds":["%s"]}],"contentRevisedAt":"%s"}
                                """.formatted(questionId, optionExtraId, token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("HOMEWORK_UPDATED"));

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homework_submissions WHERE assignment_id = ? AND user_id = ?",
                Integer.class, assignmentId, pendingStudentId));
    }

    @Test
    void dueDateOnlyDoesNotBumpRevisionToken() throws Exception {
        Timestamp beforeTs = jdbcTemplate.queryForObject(
                "SELECT content_revised_at FROM homework_assignments WHERE id = ?",
                Timestamp.class, assignmentId);
        Instant before = beforeTs.toInstant();

        mockMvc.perform(put("/api/v1/admin/homework/" + assignmentId + "/assignees/" + studentId + "/due-on")
                        .with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dueOn\": \"2026-09-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignees[?(@.userId=='" + studentId + "')].dueOn").value("2026-09-01"));

        mockMvc.perform(put("/api/v1/admin/homework/" + assignmentId + "/assignees")
                        .with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeIds":["%s","%s"],"dueOn":"2026-10-01"}
                                """.formatted(studentId, pendingStudentId)))
                .andExpect(status().isOk());

        java.sql.Date studentDue = jdbcTemplate.queryForObject(
                "SELECT due_on FROM homework_targets WHERE assignment_id = ? AND user_id = ?",
                java.sql.Date.class, assignmentId, studentId);
        assertEquals(java.sql.Date.valueOf("2026-09-01"), studentDue);

        Timestamp afterTs = jdbcTemplate.queryForObject(
                "SELECT content_revised_at FROM homework_assignments WHERE id = ?",
                Timestamp.class, assignmentId);
        Instant after = afterTs.toInstant();
        assertEquals(before, after);
    }

    @Test
    void unreferencedLeftoverOptionIsDeleted() throws Exception {
        mockMvc.perform(put("/api/v1/admin/homework/" + assignmentId)
                        .with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Gramática",
                                  "instructions": "Elige",
                                  "homeworkType": "READ",
                                  "level": null,
                                  "questions": [{
                                    "id": "%s",
                                    "kind": "SINGLE_CHOICE",
                                    "prompt": "¿Capital?",
                                    "options": [
                                      {"id": "%s", "label": "Madrid", "correct": true},
                                      {"id": "%s", "label": "Lisboa", "correct": false}
                                    ]
                                  }],
                                  "audioUrl": null,
                                  "audioFileId": null,
                                  "mediaSourceKind": null
                                }
                                """.formatted(questionId, optionCorrectId, optionWrongId)))
                .andExpect(status().isOk());

        Integer extra = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homework_question_options WHERE id = ?",
                Integer.class, optionExtraId);
        assertEquals(0, extra);
        assertFalse(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT retired FROM homework_question_options WHERE id = ?",
                Boolean.class, optionCorrectId)));
    }

    private String liveEditBody(String prompt, String correctLabel, UUID correctId) {
        UUID newWrong = UUID.randomUUID();
        return """
                {
                  "title": "Gramática",
                  "instructions": "Elige",
                  "homeworkType": "READ",
                  "level": null,
                  "questions": [{
                    "id": "%s",
                    "kind": "SINGLE_CHOICE",
                    "prompt": "%s",
                    "options": [
                      {"id": "%s", "label": "%s", "correct": true},
                      {"id": "%s", "label": "Lisboa", "correct": false}
                    ]
                  }],
                  "audioUrl": null,
                  "audioFileId": null,
                  "mediaSourceKind": null
                }
                """.formatted(questionId, prompt, correctId, correctLabel, newWrong);
    }

    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken(
                adminEmail, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private UsernamePasswordAuthenticationToken student() {
        return new UsernamePasswordAuthenticationToken(
                studentEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    private UsernamePasswordAuthenticationToken pendingStudent() {
        return new UsernamePasswordAuthenticationToken(
                pendingEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }
}
