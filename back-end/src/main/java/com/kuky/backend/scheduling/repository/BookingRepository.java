package com.kuky.backend.scheduling.repository;

import com.kuky.backend.scheduling.model.Booking;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BookingRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public BookingRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Booking> BOOKING_MAPPER = (rs, rowNum) -> {
        Booking b = new Booking();
        b.setId(rs.getObject("id", UUID.class));
        b.setUserId(rs.getObject("user_id", UUID.class));
        b.setSlotStart(rs.getTimestamp("slot_start").toInstant());
        b.setDurationMinutes(rs.getInt("duration_minutes"));
        b.setStatus(rs.getString("status"));
        b.setZoomMeetingId(rs.getString("zoom_meeting_id"));
        b.setZoomJoinUrl(rs.getString("zoom_join_url"));
        b.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
        if (cancelledAt != null) {
            b.setCancelledAt(cancelledAt.toInstant());
        }
        Timestamp reminderSentAt = rs.getTimestamp("reminder_sent_at");
        if (reminderSentAt != null) {
            b.setReminderSentAt(reminderSentAt.toInstant());
        }
        return b;
    };

    public List<Instant> findConfirmedSlotStartsBetween(Instant from, Instant to) {
        String sql = """
                SELECT slot_start FROM bookings
                WHERE status = 'CONFIRMED'
                AND slot_start >= :from AND slot_start < :to
                """;
        return jdbc.query(sql,
                Map.of("from", Timestamp.from(from), "to", Timestamp.from(to)),
                (rs, rowNum) -> rs.getTimestamp("slot_start").toInstant());
    }

    /** Lightweight projection of an upcoming confirmed booking (with the student's email). */
    public record ConfirmedBookingView(UUID id, String email, Instant slotStart) {}

    /** Upcoming confirmed bookings (start at/after {@code from}), joined to the booking student. */
    public List<ConfirmedBookingView> findUpcomingConfirmedBookings(Instant from) {
        String sql = """
                SELECT b.id, u.email, b.slot_start
                FROM bookings b JOIN users u ON u.id = b.user_id
                WHERE b.status = 'CONFIRMED' AND b.slot_start >= :from
                ORDER BY b.slot_start
                """;
        return jdbc.query(sql, Map.of("from", Timestamp.from(from)),
                (rs, rowNum) -> new ConfirmedBookingView(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getTimestamp("slot_start").toInstant()));
    }

    /** Full admin view of an upcoming confirmed booking, including end time and Zoom URL. */
    public record AdminBookingView(UUID id, UUID studentId, String email,
                                   String firstName, String lastName, String username,
                                   Instant slotStart, Instant slotEnd, String zoomJoinUrl) {}

    /** Upcoming confirmed bookings for the admin panel (start at/after {@code from}). */
    public List<AdminBookingView> findUpcomingBookingsForAdmin(Instant from) {
        String sql = """
                SELECT b.id, u.id AS student_id, u.email, u.first_name, u.last_name, u.username,
                       b.slot_start, b.duration_minutes, b.zoom_join_url
                FROM bookings b JOIN users u ON u.id = b.user_id
                WHERE b.status = 'CONFIRMED' AND b.slot_start >= :from
                ORDER BY b.slot_start
                """;
        return jdbc.query(sql, Map.of("from", Timestamp.from(from)), (rs, rowNum) -> {
            Instant slotStart = rs.getTimestamp("slot_start").toInstant();
            int minutes = rs.getInt("duration_minutes");
            return new AdminBookingView(
                    rs.getObject("id", UUID.class),
                    rs.getObject("student_id", UUID.class),
                    rs.getString("email"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("username"),
                    slotStart,
                    slotStart.plusSeconds((long) minutes * 60),
                    rs.getString("zoom_join_url"));
        });
    }

    public Optional<Booking> findById(UUID id) {
        String sql = "SELECT * FROM bookings WHERE id = :id";
        List<Booking> results = jdbc.query(sql, Map.of("id", id), BOOKING_MAPPER);
        return results.stream().findFirst();
    }

    public List<Booking> findByUserId(UUID userId) {
        String sql = "SELECT * FROM bookings WHERE user_id = :userId ORDER BY slot_start DESC";
        return jdbc.query(sql, Map.of("userId", userId), BOOKING_MAPPER);
    }

    public Booking insert(Booking booking) {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        String sql = """
                INSERT INTO bookings (id, user_id, slot_start, duration_minutes, status, created_at)
                VALUES (:id, :userId, :slotStart, :durationMinutes, :status, :createdAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", booking.getUserId())
                .addValue("slotStart", Timestamp.from(booking.getSlotStart()))
                .addValue("durationMinutes", booking.getDurationMinutes())
                .addValue("status", booking.getStatus())
                .addValue("createdAt", Timestamp.from(now));
        jdbc.update(sql, params);
        booking.setId(id);
        booking.setCreatedAt(now);
        return booking;
    }

    public void updateZoomDetails(UUID id, String meetingId, String joinUrl) {
        String sql = """
                UPDATE bookings SET zoom_meeting_id = :meetingId, zoom_join_url = :joinUrl
                WHERE id = :id
                """;
        jdbc.update(sql, Map.of("id", id, "meetingId", meetingId, "joinUrl", joinUrl));
    }

    public void markCancelled(UUID id, Instant when) {
        String sql = """
                UPDATE bookings SET status = 'CANCELLED', cancelled_at = :cancelledAt
                WHERE id = :id
                """;
        jdbc.update(sql, Map.of("id", id, "cancelledAt", Timestamp.from(when)));
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM bookings WHERE id = :id", Map.of("id", id));
    }

    /** Lightweight projection of a booking due for its 24h-before reminder. */
    public record ReminderDueView(UUID id, String email, Instant slotStart, String zoomJoinUrl) {}

    /**
     * Confirmed, not-yet-reminded bookings that have crossed the 24h-before mark
     * (and haven't started yet). Catch-up style: bounded only by {@code now}, so a
     * missed poll cycle is caught on the next run rather than relying on a narrow window.
     */
    public List<ReminderDueView> findBookingsDueForReminder(Instant now) {
        String sql = """
                SELECT b.id, u.email, b.slot_start, b.zoom_join_url
                FROM bookings b JOIN users u ON u.id = b.user_id
                WHERE b.status = 'CONFIRMED'
                AND b.reminder_sent_at IS NULL
                AND b.slot_start > :now
                AND b.slot_start <= :dueBy
                ORDER BY b.slot_start
                """;
        return jdbc.query(sql,
                Map.of("now", Timestamp.from(now), "dueBy", Timestamp.from(now.plusSeconds(24 * 3600))),
                (rs, rowNum) -> new ReminderDueView(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getTimestamp("slot_start").toInstant(),
                        rs.getString("zoom_join_url")));
    }

    /** Atomically claims a booking's reminder slot; returns false if it was already claimed. */
    public boolean claimReminder(UUID id, Instant now) {
        String sql = """
                UPDATE bookings SET reminder_sent_at = :now
                WHERE id = :id AND reminder_sent_at IS NULL
                """;
        int rows = jdbc.update(sql, Map.of("id", id, "now", Timestamp.from(now)));
        return rows > 0;
    }
}
