package com.kuky.backend.admin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class StudentAdminControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JavaMailSender mailSender;

    private MockMvc mockMvc;
    private String adminEmail;
    private String userEmail;
    private String studentEmail;
    private String adminAccountEmail;
    private UUID userId;
    private UUID studentId;
    private UUID adminAccountId;

    private static UsernamePasswordAuthenticationToken adminPrincipal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        Mockito.reset(mailSender);

        adminEmail = "admin-" + UUID.randomUUID() + "@kuky.es";
        userEmail = "user-" + UUID.randomUUID() + "@example.com";
        studentEmail = "student-" + UUID.randomUUID() + "@example.com";
        adminAccountEmail = "otheradmin-" + UUID.randomUUID() + "@example.com";

        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (gen_random_uuid(), ?, 'hash', 'ACTIVE', 'ADMIN', true)",
                adminEmail);

        userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'USER', true)",
                userId, userEmail);

        studentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                studentId, studentEmail);

        adminAccountId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'ADMIN', true)",
                adminAccountId, adminAccountEmail);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM users WHERE email IN (?, ?, ?, ?)",
                adminEmail, userEmail, studentEmail, adminAccountEmail);
    }

    @Test
    void getRegisteredUsers_returnsOnlyUserRoleAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email=='" + userEmail + "')]").exists())
                .andExpect(jsonPath("$[?(@.email=='" + studentEmail + "')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.email=='" + adminAccountEmail + "')]").doesNotExist());
    }

    @Test
    void grantStudent_promotesUserAndSendsEmail() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + userId + "/student")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        String role = jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = ?", String.class, userId);
        org.junit.jupiter.api.Assertions.assertEquals("STUDENT", role);
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void grantStudent_isIdempotent_whenAlreadyStudent() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + studentId + "/student")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        Mockito.verify(mailSender, Mockito.never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void grantStudent_returns404_forUnknownId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + UUID.randomUUID() + "/student")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void grantStudent_returns404_forAdminId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + adminAccountId + "/student")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void grantStudent_returns403_forNonAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + userId + "/student")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                studentEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokeStudent_demotesStudentAndSendsEmail_andPreservesHistoryEndpoints() throws Exception {
        // Give the student a booking so we can prove history stays reachable after revoke.
        jdbcTemplate.update("""
                INSERT INTO bookings (id, user_id, slot_start, slot_end, duration_minutes, status)
                VALUES (gen_random_uuid(), ?, NOW() + INTERVAL '7 days', NOW() + INTERVAL '7 days' + INTERVAL '60 minutes', 60, 'CONFIRMED')
                """, studentId);

        mockMvc.perform(delete("/api/v1/admin/users/" + studentId + "/student")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        String role = jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = ?", String.class, studentId);
        org.junit.jupiter.api.Assertions.assertEquals("USER", role);
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));

        // History (own bookings) remains visible after revoke — GET /bookings only requires authentication.
        mockMvc.perform(get("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                studentEmail, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming[0].status").value("CONFIRMED"));

        jdbcTemplate.update("DELETE FROM bookings WHERE user_id = ?", studentId);
    }

    @Test
    void revokeStudent_isIdempotent_whenAlreadyUser() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/" + userId + "/student")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        Mockito.verify(mailSender, Mockito.never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void revokeStudent_returns404_forUnknownId() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/" + UUID.randomUUID() + "/student")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void revokeStudent_returns403_forNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/" + studentId + "/student")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                studentEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void grantExtendedClass_grantsEligibilityAndSendsEmail() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + studentId + "/extended-class")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extendedClassEligible").value(true));

        Boolean eligible = jdbcTemplate.queryForObject(
                "SELECT extended_class_eligible FROM users WHERE id = ?", Boolean.class, studentId);
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, eligible);
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void grantExtendedClass_isIdempotent_whenAlreadyEligible() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + studentId + "/extended-class")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk());
        Mockito.reset(mailSender);

        mockMvc.perform(post("/api/v1/admin/users/" + studentId + "/extended-class")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extendedClassEligible").value(true));

        Mockito.verify(mailSender, Mockito.never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void grantExtendedClass_returns404_forUnknownId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + UUID.randomUUID() + "/extended-class")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void grantExtendedClass_returns404_forAdminId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + adminAccountId + "/extended-class")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void grantExtendedClass_returns403_forNonAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + studentId + "/extended-class")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                studentEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokeExtendedClass_revokesEligibilityAndSendsEmail_andPreservesExistingBookingDuration() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + studentId + "/extended-class")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk());

        // FR-013: an existing 90-minute booking made while eligible must be unaffected by revocation.
        jdbcTemplate.update("""
                INSERT INTO bookings (id, user_id, slot_start, slot_end, duration_minutes, status)
                VALUES (gen_random_uuid(), ?, NOW() + INTERVAL '7 days', NOW() + INTERVAL '7 days' + INTERVAL '90 minutes', 90, 'CONFIRMED')
                """, studentId);
        Mockito.reset(mailSender);

        mockMvc.perform(delete("/api/v1/admin/users/" + studentId + "/extended-class")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extendedClassEligible").value(false));

        Boolean eligible = jdbcTemplate.queryForObject(
                "SELECT extended_class_eligible FROM users WHERE id = ?", Boolean.class, studentId);
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.FALSE, eligible);
        Mockito.verify(mailSender).send(any(SimpleMailMessage.class));

        Integer duration = jdbcTemplate.queryForObject(
                "SELECT duration_minutes FROM bookings WHERE user_id = ? AND status = 'CONFIRMED'",
                Integer.class, studentId);
        org.junit.jupiter.api.Assertions.assertEquals(90, duration);

        jdbcTemplate.update("DELETE FROM bookings WHERE user_id = ?", studentId);
    }

    @Test
    void revokeExtendedClass_isIdempotent_whenAlreadyIneligible() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/" + studentId + "/extended-class")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extendedClassEligible").value(false));

        Mockito.verify(mailSender, Mockito.never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void revokeExtendedClass_returns404_forUnknownId() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/" + UUID.randomUUID() + "/extended-class")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void revokeExtendedClass_returns403_forNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/" + studentId + "/extended-class")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                studentEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isForbidden());
    }
}
