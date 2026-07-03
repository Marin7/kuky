package com.kuky.backend.scheduling;

import com.kuky.backend.scheduling.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Repository-level verification of the reminder "due" query and claim against a real database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class BookingReminderRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM bookings");
        jdbcTemplate.execute("""
                INSERT INTO users (id, email, password_hash, status, role, gdpr_consent)
                VALUES (gen_random_uuid(), 'reminder-test@kuky.es', '$2a$12$placeholder', 'ACTIVE', 'STUDENT', true)
                ON CONFLICT (email) DO NOTHING
                """);
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'reminder-test@kuky.es'", UUID.class);
    }

    @Test
    void findBookingsDueForReminder_returnsConfirmedBookingWithin24Hours() {
        Instant now = Instant.now();
        UUID bookingId = insertBooking(now.plusSeconds(23 * 3600), "CONFIRMED", null);

        List<BookingRepository.ReminderDueView> due = bookingRepository.findBookingsDueForReminder(now);

        assertThat(due).extracting(BookingRepository.ReminderDueView::id).contains(bookingId);
    }

    @Test
    void claimReminder_returnsTrueOnceThenFalse() {
        Instant now = Instant.now();
        UUID bookingId = insertBooking(now.plusSeconds(23 * 3600), "CONFIRMED", null);

        boolean firstClaim = bookingRepository.claimReminder(bookingId, now);
        boolean secondClaim = bookingRepository.claimReminder(bookingId, now);

        assertThat(firstClaim).isTrue();
        assertThat(secondClaim).isFalse();
    }

    @Test
    void findBookingsDueForReminder_excludesCancelledBookings() {
        Instant now = Instant.now();
        UUID bookingId = insertBooking(now.plusSeconds(23 * 3600), "CANCELLED", now);

        List<BookingRepository.ReminderDueView> due = bookingRepository.findBookingsDueForReminder(now);

        assertThat(due).extracting(BookingRepository.ReminderDueView::id).doesNotContain(bookingId);
    }

    @Test
    void findBookingsDueForReminder_populatesSecondStudentEmail_whenOneIsAttached() {
        jdbcTemplate.execute("""
                INSERT INTO users (id, email, password_hash, status, role, gdpr_consent)
                VALUES (gen_random_uuid(), 'reminder-second@kuky.es', '$2a$12$placeholder', 'ACTIVE', 'STUDENT', true)
                ON CONFLICT (email) DO NOTHING
                """);
        UUID companionStudentId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'reminder-second@kuky.es'", UUID.class);

        Instant now = Instant.now();
        UUID bookingId = insertBooking(now.plusSeconds(23 * 3600), "CONFIRMED", null);
        jdbcTemplate.update("UPDATE bookings SET second_student_id = ? WHERE id = ?", companionStudentId, bookingId);

        List<BookingRepository.ReminderDueView> due = bookingRepository.findBookingsDueForReminder(now);

        BookingRepository.ReminderDueView view = due.stream()
                .filter(v -> v.id().equals(bookingId)).findFirst().orElseThrow();
        assertThat(view.companionStudentEmail()).isEqualTo("reminder-second@kuky.es");
    }

    @Test
    void findBookingsDueForReminder_companionStudentEmailIsNull_whenNoneAttached() {
        Instant now = Instant.now();
        UUID bookingId = insertBooking(now.plusSeconds(23 * 3600), "CONFIRMED", null);

        List<BookingRepository.ReminderDueView> due = bookingRepository.findBookingsDueForReminder(now);

        BookingRepository.ReminderDueView view = due.stream()
                .filter(v -> v.id().equals(bookingId)).findFirst().orElseThrow();
        assertThat(view.companionStudentEmail()).isNull();
    }

    private UUID insertBooking(Instant slotStart, String status, Instant cancelledAt) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO bookings (id, user_id, slot_start, slot_end, duration_minutes, status,
                                               zoom_join_url, created_at, cancelled_at)
                        VALUES (?, ?, ?, ?, 60, ?, 'https://zoom.example/test', now(), ?)
                        """,
                id, userId, java.sql.Timestamp.from(slotStart), java.sql.Timestamp.from(slotStart.plusSeconds(3600)),
                status, cancelledAt == null ? null : java.sql.Timestamp.from(cancelledAt));
        return id;
    }
}
