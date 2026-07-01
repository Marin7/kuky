package com.kuky.backend.config;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the student-only access gate added to SecurityConfig for booking creation,
 * purchasing, and coursework (/api/v1/learning/**), and that browsing/history endpoints
 * remain unaffected by role.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class SecurityConfigStudentGatingIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private String studentEmail;
    private String userEmail;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        studentEmail = "gating-student-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (gen_random_uuid(), ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                studentEmail);

        userEmail = "gating-user-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (gen_random_uuid(), ?, 'hash', 'ACTIVE', 'USER', true)",
                userEmail);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM users WHERE email IN (?, ?)", studentEmail, userEmail);
    }

    private MockMvc mvc() {
        return mockMvc;
    }

    private static UsernamePasswordAuthenticationToken principal(String role) {
        return new UsernamePasswordAuthenticationToken(
                "gating-test-" + UUID.randomUUID() + "@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    // --- Gated actions: anonymous ---

    @Test
    void createBooking_anonymous_returns401() throws Exception {
        mvc().perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void purchase_anonymous_returns401() throws Exception {
        mvc().perform(post("/api/v1/purchases").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void learningOverview_anonymous_returns401() throws Exception {
        mvc().perform(get("/api/v1/learning")).andExpect(status().isUnauthorized());
    }

    // --- Gated actions: USER role blocked ---

    @Test
    void createBooking_userRole_returns403() throws Exception {
        mvc().perform(post("/api/v1/bookings")
                        .with(authentication(principal("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void purchase_userRole_returns403() throws Exception {
        mvc().perform(post("/api/v1/purchases")
                        .with(authentication(principal("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void learningOverview_userRole_returns403() throws Exception {
        mvc().perform(get("/api/v1/learning").with(authentication(principal("USER"))))
                .andExpect(status().isForbidden());
    }

    // --- Gated actions: STUDENT and ADMIN pass the gate (may still fail business validation) ---

    @Test
    void createBooking_studentRole_passesGate() throws Exception {
        // Empty body fails @Valid (slotStart is required) *after* the security gate —
        // 400, not 401/403, proves the gate let a STUDENT through.
        mvc().perform(post("/api/v1/bookings")
                        .with(authentication(principal("STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createBooking_adminRole_passesGate() throws Exception {
        mvc().perform(post("/api/v1/bookings")
                        .with(authentication(principal("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void purchase_studentRole_passesGate() throws Exception {
        // Empty body fails @Valid (itemType/slug required) *after* the security gate.
        mvc().perform(post("/api/v1/purchases")
                        .with(authentication(principal("STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void learningOverview_studentRole_passesGate() throws Exception {
        mvc().perform(get("/api/v1/learning")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                studentEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isOk());
    }

    // --- Unaffected by role: public browsing ---

    @Test
    void getSchedule_anonymous_returns200() throws Exception {
        mvc().perform(get("/api/v1/schedule")).andExpect(status().isOk());
    }

    @Test
    void getResourceCatalog_anonymous_returns200() throws Exception {
        mvc().perform(get("/api/v1/resources")).andExpect(status().isOk());
    }

    // --- Unaffected by role: own history stays authenticated-only ---

    @Test
    void listBookings_userRole_returns200() throws Exception {
        mvc().perform(get("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                userEmail, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))))))
                .andExpect(status().isOk());
    }

    @Test
    void listPurchases_userRole_returns200() throws Exception {
        mvc().perform(get("/api/v1/purchases")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                userEmail, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))))))
                .andExpect(status().isOk());
    }
}
