package com.kuky.backend.admin;

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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class BookingAdminControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private String adminEmail;
    private String studentEmail;
    private UUID studentId;

    private static UsernamePasswordAuthenticationToken adminPrincipal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
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
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM bookings WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE email IN (?, ?)", adminEmail, studentEmail);
    }

    private UUID insertBooking(String status, Instant slotStart) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO bookings (id, user_id, slot_start, duration_minutes, status)
                VALUES (?, ?, ?, 50, ?)
                """, id, studentId, java.sql.Timestamp.from(slotStart), status);
        return id;
    }

    @Test
    void setNoShow_flipsFlagForPastConfirmedBooking() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS));

        mockMvc.perform(put("/api/v1/admin/bookings/" + bookingId + "/no-show")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noShow\": true}"))
                .andExpect(status().isNoContent());

        Boolean noShow = jdbcTemplate.queryForObject(
                "SELECT no_show FROM bookings WHERE id = ?", Boolean.class, bookingId);
        org.junit.jupiter.api.Assertions.assertEquals(true, noShow);
    }

    @Test
    void setNoShow_returns422_forFutureBooking() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(put("/api/v1/admin/bookings/" + bookingId + "/no-show")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noShow\": true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BOOKING_NOT_ELIGIBLE_FOR_NO_SHOW"));
    }

    @Test
    void setNoShow_returns403_forNonAdmin() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS));

        mockMvc.perform(put("/api/v1/admin/bookings/" + bookingId + "/no-show")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                studentEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noShow\": true}"))
                .andExpect(status().isForbidden());
    }
}
