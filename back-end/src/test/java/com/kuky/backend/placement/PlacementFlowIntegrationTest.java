package com.kuky.backend.placement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * End-to-end checks for the placement flow against a real DB (local profile):
 * the universal login gate, the hidden answer key, the single-submission lock,
 * and the server-authoritative deadline (FR-006: a submit received after the
 * deadline is still accepted and grades exactly the answers sent).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class PlacementFlowIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private String testEmail;
    private UUID questionId;
    private UUID correctOptionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        testEmail = "test-placement-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, gdpr_consent) VALUES (gen_random_uuid(), ?, 'hash', 'ACTIVE', true)",
                testEmail);

        // Isolate from dev seed / other local data so grading counts only our fixture question.
        jdbcTemplate.update("UPDATE placement_questions SET active = false");

        questionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO placement_questions (id, skill, position, kind, prompt, active)
                VALUES (?, 'GRAMMAR', 0, 'SINGLE_CHOICE', '¿Cuál es correcto?', true)
                """, questionId);
        correctOptionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO placement_question_options (id, question_id, position, label, is_correct)
                VALUES (?, ?, 0, 'correcta', true)
                """, correctOptionId, questionId);
        jdbcTemplate.update("""
                INSERT INTO placement_question_options (id, question_id, position, label, is_correct)
                VALUES (gen_random_uuid(), ?, 1, 'incorrecta', false)
                """, questionId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("""
                DELETE FROM placement_writing_submissions WHERE user_id IN (SELECT id FROM users WHERE email = ?)
                """, testEmail);
        jdbcTemplate.update("""
                DELETE FROM placement_answers WHERE attempt_section_id IN (
                    SELECT s.id FROM placement_attempt_sections s
                    JOIN placement_attempts a ON a.id = s.attempt_id
                    JOIN users u ON u.id = a.user_id WHERE u.email = ?)
                """, testEmail);
        jdbcTemplate.update("""
                DELETE FROM placement_attempt_sections WHERE attempt_id IN (
                    SELECT a.id FROM placement_attempts a JOIN users u ON u.id = a.user_id WHERE u.email = ?)
                """, testEmail);
        jdbcTemplate.update("""
                DELETE FROM placement_attempts WHERE user_id IN (SELECT id FROM users WHERE email = ?)
                """, testEmail);
        jdbcTemplate.update("DELETE FROM placement_question_options WHERE question_id = ?", questionId);
        jdbcTemplate.update("DELETE FROM placement_questions WHERE id = ?", questionId);
        jdbcTemplate.update("UPDATE placement_questions SET active = true");
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", testEmail);
    }

    private static UsernamePasswordAuthenticationToken principal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
    }

    @Test
    void getTest_anonymous_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/placement/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void getTest_authenticated_hidesAnswerKey() throws Exception {
        mockMvc.perform(get("/api/v1/placement/test").with(authentication(principal(testEmail))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("isCorrect"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("cefrLevel"))));
    }

    @Test
    void submitSection_secondTime_isRejectedWithConflict() throws Exception {
        UUID attemptId = startAttemptAndSection();

        mockMvc.perform(post("/api/v1/placement/attempts/{id}/sections/GRAMMAR/submit", attemptId)
                        .with(authentication(principal(testEmail)))
                        .contentType(APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/placement/attempts/{id}/sections/GRAMMAR/submit", attemptId)
                        .with(authentication(principal(testEmail)))
                        .contentType(APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SECTION_ALREADY_SUBMITTED"));
    }

    @Test
    void submitSection_afterDeadline_stillGradesExactlyWhatWasSent() throws Exception {
        UUID attemptId = startAttemptAndSection();

        // Simulate the deadline having already passed (server-authoritative; FR-006).
        jdbcTemplate.update("""
                UPDATE placement_attempt_sections SET deadline_at = NOW() - INTERVAL '1 hour'
                WHERE attempt_id = ? AND skill = 'GRAMMAR'
                """, attemptId);

        String body = "{\"answers\":[{\"questionId\":\"" + questionId + "\",\"selectedOptionIds\":[\"" + correctOptionId + "\"],\"answerText\":null}]}";

        mockMvc.perform(post("/api/v1/placement/attempts/{id}/sections/GRAMMAR/submit", attemptId)
                        .with(authentication(principal(testEmail)))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scorePercent").value(100)); // the late submit still grades what was sent
    }

    @Test
    void startSection_concurrentDuplicateCalls_bothSucceedWithSameDeadline() throws Exception {
        String json = mockMvc.perform(post("/api/v1/placement/attempts").with(authentication(principal(testEmail))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID attemptId = UUID.fromString(json.replaceAll(".*\"attemptId\":\"([^\"]+)\".*", "$1"));

        // Two callers racing to start the same (attempt, skill) section — e.g. a client
        // that fires the start request twice (React effect double-invoke, a retry, a second
        // tab). Both must succeed idempotently with the same deadline, never a 409/5xx
        // from the underlying unique-constraint violation (regression test for the race
        // that previously left the client's countdown stuck at 0:00).
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<String>> futures = List.of(
                    pool.submit(() -> raceStart(attemptId, ready, go)),
                    pool.submit(() -> raceStart(attemptId, ready, go)));
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();

            String deadline1 = extractDeadline(futures.get(0).get(5, TimeUnit.SECONDS));
            String deadline2 = extractDeadline(futures.get(1).get(5, TimeUnit.SECONDS));

            org.junit.jupiter.api.Assertions.assertEquals(deadline1, deadline2);
        } finally {
            pool.shutdownNow();
        }
    }

    private String raceStart(UUID attemptId, CountDownLatch ready, CountDownLatch go) throws Exception {
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        return mockMvc.perform(post("/api/v1/placement/attempts/{id}/sections/LISTENING/start", attemptId)
                        .with(authentication(principal(testEmail))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String extractDeadline(String json) {
        Matcher m = Pattern.compile("\"deadlineAt\":\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    @Test
    void submitWriting_anyLoggedInUser_isStoredWithNoPaymentCheck() throws Exception {
        // Writing has its own server-timed start/deadline lifecycle (like the auto-graded
        // sections) but that timer is not a payment gate — any logged-in user can start it.
        mockMvc.perform(post("/api/v1/placement/writing/start").with(authentication(principal(testEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlineAt").exists());

        mockMvc.perform(post("/api/v1/placement/writing")
                        .with(authentication(principal(testEmail)))
                        .contentType(APPLICATION_JSON)
                        .content("{\"body\":\"Mi redacción de prueba.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        // No payment/order check ever runs — there is no such endpoint or gate to satisfy.
        // (The response does legitimately contain a "status" field for the writing timer's
        // own NOT_STARTED/IN_PROGRESS state — FR-009/FR-012 only forbid PAYMENT status/amount.)
        mockMvc.perform(get("/api/v1/placement/full-evaluation").with(authentication(principal(testEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mySubmission.body").value("Mi redacción de prueba."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("paymentStatus"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("orderStatus"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\"amount\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\"reference\""))));
    }

    @Test
    void submitWriting_withoutStarting_isRejected() throws Exception {
        mockMvc.perform(post("/api/v1/placement/writing")
                        .with(authentication(principal(testEmail)))
                        .contentType(APPLICATION_JSON)
                        .content("{\"body\":\"Sin empezar.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SECTION_NOT_STARTED"));
    }

    @Test
    void startWriting_calledTwice_returnsSameDeadline() throws Exception {
        String first = mockMvc.perform(post("/api/v1/placement/writing/start").with(authentication(principal(testEmail))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/v1/placement/writing/start").with(authentication(principal(testEmail))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(extractDeadline(first), extractDeadline(second));
    }

    private UUID startAttemptAndSection() throws Exception {
        String json = mockMvc.perform(post("/api/v1/placement/attempts").with(authentication(principal(testEmail))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID attemptId = UUID.fromString(json.replaceAll(".*\"attemptId\":\"([^\"]+)\".*", "$1"));

        mockMvc.perform(post("/api/v1/placement/attempts/{id}/sections/GRAMMAR/start", attemptId)
                        .with(authentication(principal(testEmail))))
                .andExpect(status().isOk());
        return attemptId;
    }
}
