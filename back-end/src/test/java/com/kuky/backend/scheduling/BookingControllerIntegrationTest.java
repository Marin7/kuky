package com.kuky.backend.scheduling;

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

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class BookingControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        jdbcTemplate.execute("DELETE FROM bookings");
    }

    private Instant validFutureSlot() {
        ZoneId zone = ZoneId.of("Europe/Madrid");
        LocalDate today = LocalDate.now(zone);
        // Find a slot at least 25h out, on the hour, within 09:00-18:00, within the 2-week horizon
        LocalDate target = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(7);
        // Make sure it's in the next week portion of the horizon
        return target.atTime(10, 0).atZone(zone).toInstant();
    }

    @Test
    void bookSlot_returns201WithJoinUrl() throws Exception {
        Instant slotStart = validFutureSlot();

        ensureTestUser();

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(slotStart, 60)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.zoomJoinUrl").isNotEmpty());
    }

    @Test
    void bookSlot_returns409_whenAlreadyBooked() throws Exception {
        Instant slotStart = validFutureSlot();
        ensureTestUser();

        // First booking succeeds
        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(slotStart, 60)))
                .andExpect(status().isCreated());

        // Second booking for same slot fails
        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test2@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(slotStart, 60)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SLOT_UNAVAILABLE"));
    }

    @Test
    void bookSlot_returns422_whenWithinLeadWindow() throws Exception {
        // Slot only 1 hour from now — within the 24h lead window
        Instant tooSoon = Instant.now().plusSeconds(3600);
        ensureTestUser();

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(tooSoon, 60)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BOOKING_TOO_SOON"));
    }

    @Test
    void listBookings_returns200WithUpcomingBooking() throws Exception {
        Instant slotStart = validFutureSlot();
        ensureTestUser();

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(slotStart, 60)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.upcoming[0].zoomJoinUrl").isNotEmpty());
    }

    // --- extended (1.5-hour) duration: booking creation and schedule (US2) -------------------

    @Test
    void bookSlot_90Minutes_returns201_forEligibleStudent() throws Exception {
        Instant slotStart = validFutureSlot();
        ensureTestUser();
        grantExtendedClass("test@kuky.es");

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(slotStart, 90)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationMinutes").value(90));
    }

    @Test
    void bookSlot_90Minutes_returns403_forNonEligibleStudent() throws Exception {
        Instant slotStart = validFutureSlot();
        ensureTestUser();

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(slotStart, 90)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("EXTENDED_CLASS_NOT_ELIGIBLE"));
    }

    @Test
    void bookSlot_invalidDuration_returns422() throws Exception {
        Instant slotStart = validFutureSlot();
        ensureTestUser();

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(slotStart, 45)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_DURATION"));
    }

    @Test
    void getSchedule_90Minutes_returnsSlotsOnTheNinetyMinuteGrid() throws Exception {
        mockMvc.perform(get("/api/v1/schedule").param("durationMinutes", "90"))
                .andExpect(status().isOk());
    }

    @Test
    void getSchedule_invalidDuration_returns422() throws Exception {
        mockMvc.perform(get("/api/v1/schedule").param("durationMinutes", "45"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_DURATION"));
    }

    // --- 15-minute booking buffer (spec 020, US1/US3) -----------------------------------------

    @Test
    void bookSlot_returns409_whenWithinFifteenMinuteBufferOfAnotherBooking() throws Exception {
        Instant firstSlot = validFutureSlot();
        // Both slots must stay hour-aligned (validateBookable requires it for a 60-min booking), so
        // the closest reachable case is the immediately-adjacent (0 min gap) back-to-back slot —
        // previously allowed, now rejected by the buffer (FR-001).
        Instant nearbySlot = firstSlot.plusSeconds(3600);
        ensureTestUser();

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(firstSlot, 60)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test2@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(nearbySlot, 60)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SLOT_UNAVAILABLE"));
    }

    @Test
    void bookSlot_afterCancellation_reopensItsFormerBufferZone() throws Exception {
        // spec 020 FR-006: cancelling a booking releases its buffer for the very next request —
        // there is no separate "release" step, findConfirmedBookingIntervalsBetween already
        // excludes non-CONFIRMED rows.
        Instant firstSlot = validFutureSlot();
        // Immediately-adjacent (0 min gap) slot, matching the rejection case above — must be
        // hour-aligned for validateBookable to consider it at all.
        Instant nearbySlot = firstSlot.plusSeconds(3600);
        ensureTestUser();

        String created = mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(firstSlot, 60)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String bookingId = created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(delete("/api/v1/bookings/{id}", bookingId)
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test2@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(nearbySlot, 60)))
                .andExpect(status().isCreated());
    }

    // --- shared bookings: companion student's own view/cancellation (US2) ------------------------

    @Test
    void listBookings_asSecondStudent_includesSharedBooking() throws Exception {
        Instant slotStart = validFutureSlot();
        ensureTestUser();

        String created = mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(slotStart, 60)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String bookingId = created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        attachCompanionStudent(bookingId, "test2@kuky.es");

        mockMvc.perform(get("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test2@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming[0].id").value(bookingId))
                .andExpect(jsonPath("$.upcoming[0].isCompanionStudent").value(true));
    }

    @Test
    void cancelBooking_asSecondStudent_cancelsWholeClass_removingItFromBothViews() throws Exception {
        Instant slotStart = validFutureSlot();
        ensureTestUser();

        String created = mockMvc.perform(post("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(slotStart, 60)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String bookingId = created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        attachCompanionStudent(bookingId, "test2@kuky.es");

        mockMvc.perform(delete("/api/v1/bookings/{id}", bookingId)
                        .with(authentication(new UsernamePasswordAuthenticationToken("test2@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming").isEmpty());

        mockMvc.perform(get("/api/v1/bookings")
                        .with(authentication(new UsernamePasswordAuthenticationToken("test2@kuky.es", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming").isEmpty());
    }

    private void attachCompanionStudent(String bookingId, String companionStudentEmail) {
        jdbcTemplate.update("""
                UPDATE bookings SET second_student_id = (SELECT id FROM users WHERE email = ?) WHERE id = ?::uuid
                """, companionStudentEmail, bookingId);
    }

    private static String bookingJson(Instant slotStart, int durationMinutes) {
        return "{\"slotStart\":\"" + slotStart.toString() + "\",\"durationMinutes\":" + durationMinutes + "}";
    }

    private void ensureTestUser() {
        jdbcTemplate.execute("""
                INSERT INTO users (id, email, password_hash, status, role, gdpr_consent)
                VALUES (gen_random_uuid(), 'test@kuky.es', '$2a$12$placeholder', 'ACTIVE', 'STUDENT', true)
                ON CONFLICT (email) DO UPDATE SET extended_class_eligible = false
                """);
        jdbcTemplate.execute("""
                INSERT INTO users (id, email, password_hash, status, role, gdpr_consent)
                VALUES (gen_random_uuid(), 'test2@kuky.es', '$2a$12$placeholder', 'ACTIVE', 'STUDENT', true)
                ON CONFLICT (email) DO NOTHING
                """);
    }

    private void grantExtendedClass(String email) {
        jdbcTemplate.update("UPDATE users SET extended_class_eligible = true WHERE email = ?", email);
    }
}
