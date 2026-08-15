package com.kuky.backend.notification;

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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
class NotificationUnseenIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private String adminEmail;
    private String studentEmail;
    private String otherStudentEmail;
    private UUID studentId;
    private UUID otherStudentId;
    private UUID assignmentId;
    private UUID questionId;
    private UUID optionCorrectId;
    private UUID submissionId;
    private UUID quizId;
    private UUID quizQuestionId;
    private UUID quizOptionId;
    private UUID unitId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        adminEmail = "admin-notify-" + UUID.randomUUID() + "@kuky.es";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (gen_random_uuid(), ?, 'hash', 'ACTIVE', 'ADMIN', true)",
                adminEmail);

        studentId = UUID.randomUUID();
        studentEmail = "student-notify-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                studentId, studentEmail);

        otherStudentId = UUID.randomUUID();
        otherStudentEmail = "other-notify-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                otherStudentId, otherStudentEmail);

        assignmentId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_assignments (id, title, instructions, published, format, homework_type, sort_order, content_revised_at)
                VALUES (?, 'Notify HW', 'Elige', true, 'EXERCISE', 'READ', 0, ?)
                """, assignmentId, Timestamp.from(Instant.parse("2026-08-15T10:00:00Z")));

        questionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_questions (id, assignment_id, position, kind, prompt, structure_json)
                VALUES (?, ?, 0, 'SINGLE_CHOICE', '¿Capital?', '{}'::jsonb)
                """, questionId, assignmentId);

        optionCorrectId = UUID.randomUUID();
        UUID optionWrongId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO homework_question_options (id, question_id, position, label, is_correct)
                VALUES (?, ?, 0, 'Madrid', true)
                """, optionCorrectId, questionId);
        jdbcTemplate.update("""
                INSERT INTO homework_question_options (id, question_id, position, label, is_correct)
                VALUES (?, ?, 1, 'Lisboa', false)
                """, optionWrongId, questionId);

        jdbcTemplate.update(
                "INSERT INTO homework_targets (assignment_id, user_id, student_seen_at) VALUES (?, ?, NOW())",
                assignmentId, studentId);
        jdbcTemplate.update(
                "INSERT INTO homework_targets (assignment_id, user_id, student_seen_at) VALUES (?, ?, NOW())",
                assignmentId, otherStudentId);

        unitId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO units (id, level, subject, position)
                VALUES (?, 'A1', 'Notificaciones', 0)
                """, unitId);

        quizId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO quizzes (id, title) VALUES (?, 'Notify Quiz')", quizId);
        quizQuestionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO quiz_questions (id, quiz_id, position, skill, kind, prompt, structure_json)
                VALUES (?, ?, 0, 'GRAMMAR', 'SINGLE_CHOICE', '¿Sí?', '{}'::jsonb)
                """, quizQuestionId, quizId);
        quizOptionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO quiz_question_options (id, question_id, position, label, is_correct)
                VALUES (?, ?, 0, 'sí', true)
                """, quizOptionId, quizQuestionId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM quiz_answers WHERE attempt_id IN (SELECT id FROM quiz_attempts WHERE quiz_id = ?)", quizId);
        jdbcTemplate.update("DELETE FROM quiz_attempts WHERE quiz_id = ?", quizId);
        jdbcTemplate.update("DELETE FROM quiz_assignees WHERE quiz_id = ?", quizId);
        jdbcTemplate.update("DELETE FROM quiz_question_options WHERE question_id = ?", quizQuestionId);
        jdbcTemplate.update("DELETE FROM quiz_questions WHERE quiz_id = ?", quizId);
        jdbcTemplate.update("DELETE FROM quizzes WHERE id = ?", quizId);
        jdbcTemplate.update("DELETE FROM unit_assignments WHERE unit_id = ?", unitId);
        jdbcTemplate.update("DELETE FROM units WHERE id = ?", unitId);
        jdbcTemplate.update("DELETE FROM homework_submissions WHERE assignment_id = ?", assignmentId);
        jdbcTemplate.update("DELETE FROM homework_targets WHERE assignment_id = ?", assignmentId);
        jdbcTemplate.update("DELETE FROM homework_assignments WHERE id = ?", assignmentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherStudentId);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", adminEmail);
    }

    @Test
    void homeworkSubmit_unseenThenMarkSeenOnExerciseResult_andListDoesNotMark() throws Exception {
        String token = Instant.parse("2026-08-15T10:00:00Z").toString();
        mockMvc.perform(put("/api/v1/learning/homework/" + assignmentId + "/answers")
                        .with(authentication(student()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":"%s","selectedOptionIds":["%s"]}],"contentRevisedAt":"%s"}
                                """.formatted(questionId, optionCorrectId, token)))
                .andExpect(status().isOk());

        Instant seenAt = jdbcTemplate.queryForObject(
                "SELECT teacher_seen_at FROM homework_submissions WHERE assignment_id = ? AND user_id = ?",
                Instant.class, assignmentId, studentId);
        assertNull(seenAt);

        mockMvc.perform(get("/api/v1/notifications/badges").with(authentication(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panel").value(true))
                .andExpect(jsonPath("$.homework").value(true))
                .andExpect(jsonPath("$.quiz").value(false));

        mockMvc.perform(get("/api/v1/admin/homework").with(authentication(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + assignmentId + "')].hasUnseenSubmissions").value(true));

        seenAt = jdbcTemplate.queryForObject(
                "SELECT teacher_seen_at FROM homework_submissions WHERE assignment_id = ? AND user_id = ?",
                Instant.class, assignmentId, studentId);
        assertNull(seenAt);

        submissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM homework_submissions WHERE assignment_id = ? AND user_id = ?",
                UUID.class, assignmentId, studentId);

        mockMvc.perform(get("/api/v1/admin/homework/submissions/" + submissionId + "/exercise-result")
                        .with(authentication(admin())))
                .andExpect(status().isOk());

        seenAt = jdbcTemplate.queryForObject(
                "SELECT teacher_seen_at FROM homework_submissions WHERE id = ?",
                Instant.class, submissionId);
        assertNotNull(seenAt);

        mockMvc.perform(get("/api/v1/notifications/badges").with(authentication(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homework").value(false))
                .andExpect(jsonPath("$.panel").value(false));
    }

    @Test
    void twoStudents_independentUnseen() throws Exception {
        String token = Instant.parse("2026-08-15T10:00:00Z").toString();
        String body = """
                {"answers":[{"questionId":"%s","selectedOptionIds":["%s"]}],"contentRevisedAt":"%s"}
                """.formatted(questionId, optionCorrectId, token);

        mockMvc.perform(put("/api/v1/learning/homework/" + assignmentId + "/answers")
                        .with(authentication(student()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/learning/homework/" + assignmentId + "/answers")
                        .with(authentication(otherStudent()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        UUID first = jdbcTemplate.queryForObject(
                "SELECT id FROM homework_submissions WHERE assignment_id = ? AND user_id = ?",
                UUID.class, assignmentId, studentId);
        mockMvc.perform(get("/api/v1/admin/homework/submissions/" + first + "/exercise-result")
                        .with(authentication(admin())))
                .andExpect(status().isOk());

        Instant otherSeen = jdbcTemplate.queryForObject(
                "SELECT teacher_seen_at FROM homework_submissions WHERE assignment_id = ? AND user_id = ?",
                Instant.class, assignmentId, otherStudentId);
        assertNull(otherSeen);

        mockMvc.perform(get("/api/v1/notifications/badges").with(authentication(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homework").value(true));
    }

    @Test
    void unitAssign_preservesSeen_andMarkSeenClearsLearning() throws Exception {
        mockMvc.perform(put("/api/v1/admin/units/" + unitId + "/assignees")
                        .with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + studentId + "\"]}"))
                .andExpect(status().isOk());

        Instant seen = jdbcTemplate.queryForObject(
                "SELECT student_seen_at FROM unit_assignments WHERE unit_id = ? AND user_id = ?",
                Instant.class, unitId, studentId);
        assertNull(seen);

        mockMvc.perform(get("/api/v1/notifications/badges").with(authentication(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learning").value(true));

        mockMvc.perform(put("/api/v1/admin/units/" + unitId + "/assignees")
                        .with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + studentId + "\",\"" + otherStudentId + "\"]}"))
                .andExpect(status().isOk());

        Instant stillNull = jdbcTemplate.queryForObject(
                "SELECT student_seen_at FROM unit_assignments WHERE unit_id = ? AND user_id = ?",
                Instant.class, unitId, studentId);
        assertNull(stillNull);
        Instant otherNull = jdbcTemplate.queryForObject(
                "SELECT student_seen_at FROM unit_assignments WHERE unit_id = ? AND user_id = ?",
                Instant.class, unitId, otherStudentId);
        assertNull(otherNull);

        mockMvc.perform(post("/api/v1/learning/units/" + unitId + "/seen")
                        .with(authentication(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unseen").value(false));

        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT student_seen_at FROM unit_assignments WHERE unit_id = ? AND user_id = ?",
                Instant.class, unitId, studentId));

        mockMvc.perform(put("/api/v1/admin/units/" + unitId + "/assignees")
                        .with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + studentId + "\"]}"))
                .andExpect(status().isOk());

        Instant preserved = jdbcTemplate.queryForObject(
                "SELECT student_seen_at FROM unit_assignments WHERE unit_id = ? AND user_id = ?",
                Instant.class, unitId, studentId);
        assertNotNull(preserved);

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM unit_assignments WHERE unit_id = ? AND user_id = ?",
                Integer.class, unitId, otherStudentId);
        assertEquals(0, remaining);
    }

    @Test
    void quizAssignAndOpen_marksStudentSeen_listDoesNot() throws Exception {
        mockMvc.perform(put("/api/v1/admin/quizzes/" + quizId + "/assignees")
                        .with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + studentId + "\"]}"))
                .andExpect(status().isOk());

        assertNull(jdbcTemplate.queryForObject(
                "SELECT student_seen_at FROM quiz_assignees WHERE quiz_id = ? AND user_id = ?",
                Instant.class, quizId, studentId));

        mockMvc.perform(get("/api/v1/quizzes").with(authentication(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizzes[0].unseen").value(true));

        assertNull(jdbcTemplate.queryForObject(
                "SELECT student_seen_at FROM quiz_assignees WHERE quiz_id = ? AND user_id = ?",
                Instant.class, quizId, studentId));

        mockMvc.perform(get("/api/v1/quizzes/" + quizId).with(authentication(student())))
                .andExpect(status().isOk());

        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT student_seen_at FROM quiz_assignees WHERE quiz_id = ? AND user_id = ?",
                Instant.class, quizId, studentId));
    }

    @Test
    void homeworkAssign_unseenThenMarkSeenOnOpen_listDoesNot() throws Exception {
        jdbcTemplate.update("DELETE FROM homework_targets WHERE assignment_id = ?", assignmentId);

        mockMvc.perform(put("/api/v1/admin/homework/" + assignmentId + "/assignees")
                        .with(authentication(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeIds\":[\"" + studentId + "\"]}"))
                .andExpect(status().isOk());

        assertNull(jdbcTemplate.queryForObject(
                "SELECT student_seen_at FROM homework_targets WHERE assignment_id = ? AND user_id = ?",
                Instant.class, assignmentId, studentId));

        mockMvc.perform(get("/api/v1/notifications/badges").with(authentication(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learning").value(true));

        mockMvc.perform(get("/api/v1/learning").with(authentication(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homework[?(@.id == '" + assignmentId + "')].unseen").value(true));

        assertNull(jdbcTemplate.queryForObject(
                "SELECT student_seen_at FROM homework_targets WHERE assignment_id = ? AND user_id = ?",
                Instant.class, assignmentId, studentId));

        mockMvc.perform(post("/api/v1/learning/homework/" + assignmentId + "/seen")
                        .with(authentication(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unseen").value(false));

        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT student_seen_at FROM homework_targets WHERE assignment_id = ? AND user_id = ?",
                Instant.class, assignmentId, studentId));

        mockMvc.perform(get("/api/v1/notifications/badges").with(authentication(student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learning").value(false));
    }

    @Test
    void userRole_hasNoLearningBadge() throws Exception {
        UUID userOnly = UUID.randomUUID();
        String email = "user-notify-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'USER', true)",
                userOnly, email);
        try {
            mockMvc.perform(get("/api/v1/notifications/badges")
                            .with(authentication(new UsernamePasswordAuthenticationToken(
                                    email, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.learning").value(false))
                    .andExpect(jsonPath("$.panel").value(false));
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userOnly);
        }
    }

    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken(
                adminEmail, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private UsernamePasswordAuthenticationToken student() {
        return new UsernamePasswordAuthenticationToken(
                studentEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    private UsernamePasswordAuthenticationToken otherStudent() {
        return new UsernamePasswordAuthenticationToken(
                otherStudentEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }
}
