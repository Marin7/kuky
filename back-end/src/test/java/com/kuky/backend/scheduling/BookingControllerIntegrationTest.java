package com.kuky.backend.scheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.scheduling.dto.CreateBookingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.web.context.WebApplicationContext;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class BookingControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

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
        CreateBookingRequest req = new CreateBookingRequest(slotStart);

        ensureTestUser();

        mockMvc.perform(post("/api/v1/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("test@kuky.es"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.zoomJoinUrl").isNotEmpty());
    }

    @Test
    void bookSlot_returns409_whenAlreadyBooked() throws Exception {
        Instant slotStart = validFutureSlot();
        CreateBookingRequest req = new CreateBookingRequest(slotStart);
        ensureTestUser();

        // First booking succeeds
        mockMvc.perform(post("/api/v1/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("test@kuky.es"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second booking for same slot fails
        mockMvc.perform(post("/api/v1/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("test2@kuky.es"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SLOT_UNAVAILABLE"));
    }

    @Test
    void bookSlot_returns422_whenWithinLeadWindow() throws Exception {
        // Slot only 1 hour from now — within the 24h lead window
        Instant tooSoon = Instant.now().plusSeconds(3600);
        CreateBookingRequest req = new CreateBookingRequest(tooSoon);
        ensureTestUser();

        mockMvc.perform(post("/api/v1/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("test@kuky.es"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BOOKING_TOO_SOON"));
    }

    @Test
    void listBookings_returns200WithUpcomingBooking() throws Exception {
        Instant slotStart = validFutureSlot();
        CreateBookingRequest req = new CreateBookingRequest(slotStart);
        ensureTestUser();

        mockMvc.perform(post("/api/v1/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("test@kuky.es"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/bookings")
                        .with(SecurityMockMvcRequestPostProcessors.user("test@kuky.es")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.upcoming[0].zoomJoinUrl").isNotEmpty());
    }

    private void ensureTestUser() {
        jdbcTemplate.execute("""
                INSERT INTO users (id, email, password_hash, status, gdpr_consent)
                VALUES (gen_random_uuid(), 'test@kuky.es', '$2a$12$placeholder', 'ACTIVE', true)
                ON CONFLICT (email) DO NOTHING
                """);
        jdbcTemplate.execute("""
                INSERT INTO users (id, email, password_hash, status, gdpr_consent)
                VALUES (gen_random_uuid(), 'test2@kuky.es', '$2a$12$placeholder', 'ACTIVE', true)
                ON CONFLICT (email) DO NOTHING
                """);
    }
}
