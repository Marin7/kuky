package com.kuky.backend.admin;

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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Editing homework must not orphan existing submissions by wiping question/option ids.
 */
class HomeworkUpdatePreservesSubmissionsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private String adminEmail;
    private UUID studentId;
    private UUID assignmentId;
    private UUID questionId;
    private UUID optionCorrectId;
    private UUID optionWrongId;
    private UUID submissionId;
    private UUID answerId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        adminEmail = "admin-hw-preserve-" + UUID.randomUUID() + "@kuky.es";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (gen_random_uuid(), ?, 'hash', 'ACTIVE', 'ADMIN', true)",
                adminEmail);

        studentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                studentId, "student-hw-preserve-" + UUID.randomUUID() + "@example.com");

        assignmentId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_assignments (id, title, instructions, published, format, homework_type, sort_order)
                VALUES (?, 'Gramática', 'Elige', true, 'EXERCISE', 'READ', 0)
                """, assignmentId);

        questionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_questions (id, assignment_id, position, kind, prompt, structure_json)
                VALUES (?, ?, 0, 'SINGLE_CHOICE', '¿Capital?', '{}'::jsonb)
                """, questionId, assignmentId);

        optionCorrectId = UUID.randomUUID();
        optionWrongId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_question_options (id, question_id, position, label, is_correct)
                VALUES (?, ?, 0, 'Madrid', true)
                """, optionCorrectId, questionId);
        jdbcTemplate.update("""
                INSERT INTO homework_question_options (id, question_id, position, label, is_correct)
                VALUES (?, ?, 1, 'Lisboa', false)
                """, optionWrongId, questionId);

        submissionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_submissions (id, user_id, assignment_id, status, submitted_at, updated_at, score_percent)
                VALUES (?, ?, ?, 'GRADED', NOW(), NOW(), 100)
                """, submissionId, studentId, assignmentId);

        answerId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_answers (id, submission_id, question_id, score)
                VALUES (?, ?, ?, 1.000)
                """, answerId, submissionId, questionId);
        jdbcTemplate.update("""
                INSERT INTO homework_answer_options (answer_id, option_id) VALUES (?, ?)
                """, answerId, optionCorrectId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM homework_submissions WHERE assignment_id = ?", assignmentId);
        jdbcTemplate.update("DELETE FROM homework_assignments WHERE id = ?", assignmentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", adminEmail);
    }

    @Test
    void updateHomework_withSameQuestionIds_keepsAnswerLinks() throws Exception {
        String body = """
                {
                  "title": "Gramática actualizada",
                  "instructions": "Elige la correcta",
                  "homeworkType": "READ",
                  "level": null,
                  "questions": [{
                    "id": "%s",
                    "kind": "SINGLE_CHOICE",
                    "prompt": "¿Capital de España?",
                    "options": [
                      {"id": "%s", "label": "Madrid", "correct": true},
                      {"id": "%s", "label": "Lisboa", "correct": false}
                    ]
                  }],
                  "audioUrl": null,
                  "audioFileId": null,
                  "mediaSourceKind": null
                }
                """.formatted(questionId, optionCorrectId, optionWrongId);

        mockMvc.perform(put("/api/v1/admin/homework/" + assignmentId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                adminEmail, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        UUID linkedQuestion = jdbcTemplate.queryForObject(
                "SELECT question_id FROM homework_answers WHERE id = ?", UUID.class, answerId);
        assertEquals(questionId, linkedQuestion);

        Integer optionRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homework_answer_options WHERE answer_id = ? AND option_id = ?",
                Integer.class, answerId, optionCorrectId);
        assertEquals(1, optionRows);

        String prompt = jdbcTemplate.queryForObject(
                "SELECT prompt FROM homework_questions WHERE id = ?", String.class, questionId);
        assertEquals("¿Capital de España?", prompt);

        UUID stillThere = jdbcTemplate.queryForObject(
                "SELECT id FROM homework_question_options WHERE id = ?", UUID.class, optionCorrectId);
        assertNotNull(stillThere);
    }

    @Test
    void updateHomework_labelOnly_doesNotBumpContentRevisedAt() throws Exception {
        jdbcTemplate.update(
                "UPDATE homework_assignments SET content_revised_at = TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00' WHERE id = ?",
                assignmentId);

        String body = """
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
                  "mediaSourceKind": null,
                  "labels": ["Tema"]
                }
                """.formatted(questionId, optionCorrectId, optionWrongId);

        mockMvc.perform(put("/api/v1/admin/homework/" + assignmentId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                adminEmail, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        String revised = jdbcTemplate.queryForObject(
                "SELECT content_revised_at AT TIME ZONE 'UTC' FROM homework_assignments WHERE id = ?",
                String.class, assignmentId);
        org.junit.jupiter.api.Assertions.assertTrue(
                revised != null && revised.startsWith("2026-01-01"),
                "label-only save must not bump content_revised_at, was " + revised);

        String[] labels = jdbcTemplate.queryForObject(
                "SELECT labels FROM homework_assignments WHERE id = ?",
                (rs, n) -> {
                    java.sql.Array arr = rs.getArray("labels");
                    if (arr == null) return new String[0];
                    Object raw = arr.getArray();
                    if (raw instanceof String[] strings) return strings;
                    return java.util.Arrays.stream((Object[]) raw)
                            .map(Object::toString)
                            .toArray(String[]::new);
                },
                assignmentId);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{"Tema"}, labels);

        Number score = jdbcTemplate.queryForObject(
                "SELECT score_percent FROM homework_submissions WHERE id = ?", Number.class, submissionId);
        assertEquals(100, score.intValue());
    }
}
