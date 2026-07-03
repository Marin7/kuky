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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private String companionStudentEmail;
    private UUID companionStudentId;

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

        companionStudentId = UUID.randomUUID();
        companionStudentEmail = "second-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                companionStudentId, companionStudentEmail);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM bookings WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE email IN (?, ?, ?)", adminEmail, studentEmail, companionStudentEmail);
    }

    private UUID insertBooking(String status, Instant slotStart) {
        return insertBooking(status, slotStart, 50);
    }

    private UUID insertBooking(String status, Instant slotStart, int durationMinutes) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO bookings (id, user_id, slot_start, slot_end, duration_minutes, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, studentId, java.sql.Timestamp.from(slotStart),
                java.sql.Timestamp.from(slotStart.plusSeconds((long) durationMinutes * 60)), durationMinutes, status);
        return id;
    }

    private String attachCompanionStudentJson(UUID studentId) {
        return "{\"studentId\":\"" + studentId + "\"}";
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

    // --- attach companion student (US1) -----------------------------------------------------------

    @Test
    void attachCompanionStudent_returns200WithPopulatedDto() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/companion-student")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attachCompanionStudentJson(companionStudentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companionStudentId").value(companionStudentId.toString()))
                .andExpect(jsonPath("$.companionStudentEmail").value(companionStudentEmail));
    }

    @Test
    void attachCompanionStudent_returns404_forUnknownBooking() throws Exception {
        mockMvc.perform(post("/api/v1/admin/bookings/" + UUID.randomUUID() + "/companion-student")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attachCompanionStudentJson(companionStudentId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("BOOKING_NOT_FOUND"));
    }

    @Test
    void attachCompanionStudent_returns404_forUnknownStudent() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/companion-student")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attachCompanionStudentJson(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void attachCompanionStudent_returns409_forCancelledBooking() throws Exception {
        UUID bookingId = insertBooking("CANCELLED", Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/companion-student")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attachCompanionStudentJson(companionStudentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BOOKING_NOT_ATTACHABLE"));
    }

    @Test
    void attachCompanionStudent_returns409_forPastBooking() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/companion-student")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attachCompanionStudentJson(companionStudentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BOOKING_NOT_ATTACHABLE"));
    }

    @Test
    void attachCompanionStudent_returns409_whenAlreadyAttached() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));
        jdbcTemplate.update("UPDATE bookings SET second_student_id = ? WHERE id = ?", companionStudentId, bookingId);

        UUID thirdStudentId = UUID.randomUUID();
        String thirdStudentEmail = "third-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                thirdStudentId, thirdStudentEmail);
        try {
            mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/companion-student")
                            .with(authentication(adminPrincipal(adminEmail)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(attachCompanionStudentJson(thirdStudentId)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("COMPANION_ALREADY_ATTACHED"));
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE email = ?", thirdStudentEmail);
        }
    }

    @Test
    void attachCompanionStudent_returns409_whenTargetIsThePrimaryStudent() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/companion-student")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attachCompanionStudentJson(studentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("COMPANION_SAME_AS_BOOKING_STUDENT"));
    }

    @Test
    void attachCompanionStudent_returns422_forNonStudentTarget() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));
        UUID plainUserId = UUID.randomUUID();
        String plainUserEmail = "user-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'USER', true)",
                plainUserId, plainUserEmail);

        try {
            mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/companion-student")
                            .with(authentication(adminPrincipal(adminEmail)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(attachCompanionStudentJson(plainUserId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("COMPANION_NOT_STUDENT"));
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE email = ?", plainUserEmail);
        }
    }

    @Test
    void attachCompanionStudent_returns403_whenExtendedClassAndTargetNotEligible() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS), 90);

        mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/companion-student")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attachCompanionStudentJson(companionStudentId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("EXTENDED_CLASS_NOT_ELIGIBLE"));
    }

    @Test
    void attachCompanionStudent_returns403_forNonAdmin() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/admin/bookings/" + bookingId + "/companion-student")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                studentEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attachCompanionStudentJson(companionStudentId)))
                .andExpect(status().isForbidden());
    }

    // --- detach companion student (US3) -----------------------------------------------------------

    @Test
    void detachCompanionStudent_returns204AndClearsFields() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));
        jdbcTemplate.update("UPDATE bookings SET second_student_id = ? WHERE id = ?", companionStudentId, bookingId);

        mockMvc.perform(delete("/api/v1/admin/bookings/" + bookingId + "/companion-student")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isNoContent());

        UUID remaining = jdbcTemplate.queryForObject(
                "SELECT second_student_id FROM bookings WHERE id = ?", UUID.class, bookingId);
        org.junit.jupiter.api.Assertions.assertNull(remaining);
    }

    @Test
    void detachCompanionStudent_returns404_whenNothingToDetach() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(delete("/api/v1/admin/bookings/" + bookingId + "/companion-student")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("COMPANION_NOT_ATTACHED"));
    }

    // --- per-student no-show (US3) --------------------------------------------------------------

    @Test
    void setNoShow_targetingCompanion_setsOnlyTheCompanionFlag() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS));
        jdbcTemplate.update("UPDATE bookings SET second_student_id = ? WHERE id = ?", companionStudentId, bookingId);

        mockMvc.perform(put("/api/v1/admin/bookings/" + bookingId + "/no-show")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noShow\": true, \"studentRole\": \"COMPANION\"}"))
                .andExpect(status().isNoContent());

        Boolean bookingStudentNoShow = jdbcTemplate.queryForObject(
                "SELECT no_show FROM bookings WHERE id = ?", Boolean.class, bookingId);
        Boolean companionNoShow = jdbcTemplate.queryForObject(
                "SELECT second_student_no_show FROM bookings WHERE id = ?", Boolean.class, bookingId);
        org.junit.jupiter.api.Assertions.assertEquals(false, bookingStudentNoShow);
        org.junit.jupiter.api.Assertions.assertEquals(true, companionNoShow);
    }

    @Test
    void setNoShow_targetingCompanion_returns404_whenNoCompanionAttached() throws Exception {
        UUID bookingId = insertBooking("CONFIRMED", Instant.now().minus(1, ChronoUnit.DAYS));

        mockMvc.perform(put("/api/v1/admin/bookings/" + bookingId + "/no-show")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noShow\": true, \"studentRole\": \"COMPANION\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("COMPANION_NOT_ATTACHED"));
    }
}
