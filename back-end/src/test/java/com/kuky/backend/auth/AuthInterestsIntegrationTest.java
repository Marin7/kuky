package com.kuky.backend.auth;

import com.kuky.backend.config.JwtConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class AuthInterestsIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private UUID studentId;
    private UUID userId;
    private String studentEmail;
    private String userEmail;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        studentEmail = "interests-student-" + UUID.randomUUID() + "@example.com";
        userEmail = "interests-user-" + UUID.randomUUID() + "@example.com";
        studentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        String hash = passwordEncoder.encode("TestPassword123!");
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, ?, 'ACTIVE', 'STUDENT', true)",
                studentId, studentEmail, hash);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, ?, 'ACTIVE', 'USER', true)",
                userId, userEmail, hash);
    }

    @AfterEach
    void tearDown() {
        if (studentId != null) {
            jdbcTemplate.update("DELETE FROM user_interests WHERE user_id = ?", studentId);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
        }
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    private Cookie authCookie(UUID id, String email, String role) {
        String token = jwtConfig.generateToken(id, email, role);
        Cookie cookie = new Cookie("auth-token", token);
        cookie.setPath("/");
        return cookie;
    }

    @Test
    void putInterests_asStudent_returnsUpdatedResponse() throws Exception {
        mockMvc.perform(put("/api/v1/auth/interests")
                        .cookie(authCookie(studentId, studentEmail, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interests":["TRAVEL","MUSIC"],"interestsNote":"Flamenco"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interestsNote").value("Flamenco"))
                .andExpect(jsonPath("$.interests.length()").value(2));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_interests WHERE user_id = ?", Integer.class, studentId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void putInterests_asUser_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/auth/interests")
                        .cookie(authCookie(userId, userEmail, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interests":["MUSIC"],"interestsNote":null}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void putInterests_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/auth/interests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interests":["MUSIC"],"interestsNote":null}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putInterests_unknownCode_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/auth/interests")
                        .cookie(authCookie(studentId, studentEmail, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interests":["GAMING"],"interestsNote":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INTEREST"));
    }

    @Test
    void putInterests_noteTooLong_returns400() throws Exception {
        String longNote = "x".repeat(281);
        mockMvc.perform(put("/api/v1/auth/interests")
                        .cookie(authCookie(studentId, studentEmail, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interests\":[],\"interestsNote\":\"" + longNote + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
