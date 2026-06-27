package com.kuky.backend.scheduling.repository;

import com.kuky.backend.scheduling.model.AvailabilityException;
import com.kuky.backend.scheduling.model.AvailabilityRule;
import com.kuky.backend.scheduling.model.DayWindow;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class AvailabilityRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AvailabilityRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<AvailabilityRule> RULE_MAPPER = (rs, n) -> {
        AvailabilityRule r = new AvailabilityRule();
        r.setId(rs.getObject("id", UUID.class));
        r.setDayOfWeek(rs.getInt("day_of_week"));
        r.setStartTime(rs.getTime("start_time").toLocalTime());
        r.setEndTime(rs.getTime("end_time").toLocalTime());
        return r;
    };

    private static final RowMapper<AvailabilityException> EXCEPTION_MAPPER = (rs, n) -> {
        AvailabilityException e = new AvailabilityException();
        e.setId(rs.getObject("id", UUID.class));
        e.setDate(rs.getDate("exception_date").toLocalDate());
        e.setKind(AvailabilityException.Kind.valueOf(rs.getString("kind")));
        e.setStartTime(rs.getTime("start_time").toLocalTime());
        e.setEndTime(rs.getTime("end_time").toLocalTime());
        return e;
    };

    public List<AvailabilityRule> findAllRules() {
        return jdbc.query(
                "SELECT * FROM availability_rules ORDER BY day_of_week, start_time",
                Map.of(), RULE_MAPPER);
    }

    /** Replace the entire weekly pattern atomically (delete-all + insert). */
    @Transactional
    public void replaceWeekly(List<AvailabilityRule> rules) {
        jdbc.update("DELETE FROM availability_rules", Map.of());
        for (AvailabilityRule r : rules) {
            MapSqlParameterSource p = new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("dayOfWeek", r.getDayOfWeek())
                    .addValue("startTime", Time.valueOf(r.getStartTime()))
                    .addValue("endTime", Time.valueOf(r.getEndTime()));
            jdbc.update("""
                    INSERT INTO availability_rules (id, day_of_week, start_time, end_time)
                    VALUES (:id, :dayOfWeek, :startTime, :endTime)
                    """, p);
        }
    }

    /**
     * Legacy date-specific overrides, read only by {@code WeekAvailabilityBootstrap} for the
     * one-time behavior-preserving migration to per-week snapshots (feature 009). Not used at
     * runtime; removable a release after rollout.
     */
    public List<AvailabilityException> findExceptionsBetween(LocalDate fromInclusive, LocalDate toExclusive) {
        return jdbc.query(
                "SELECT * FROM availability_exceptions "
                        + "WHERE exception_date >= :from AND exception_date < :to "
                        + "ORDER BY exception_date, start_time",
                Map.of("from", Date.valueOf(fromInclusive), "to", Date.valueOf(toExclusive)),
                EXCEPTION_MAPPER);
    }

    // --- Per-week materialized availability (feature 009) -------------------------------------

    private static final RowMapper<DayWindow> DAY_WINDOW_MAPPER = (rs, n) -> new DayWindow(
            rs.getDate("slot_date").toLocalDate(),
            rs.getTime("start_time").toLocalTime(),
            rs.getTime("end_time").toLocalTime());

    public List<DayWindow> findDayWindowsBetween(LocalDate fromInclusive, LocalDate toExclusive) {
        return jdbc.query(
                "SELECT slot_date, start_time, end_time FROM week_availability "
                        + "WHERE slot_date >= :from AND slot_date < :to "
                        + "ORDER BY slot_date, start_time",
                Map.of("from", Date.valueOf(fromInclusive), "to", Date.valueOf(toExclusive)),
                DAY_WINDOW_MAPPER);
    }

    public List<DayWindow> findDayWindows(LocalDate date) {
        return jdbc.query(
                "SELECT slot_date, start_time, end_time FROM week_availability "
                        + "WHERE slot_date = :date ORDER BY start_time",
                Map.of("date", Date.valueOf(date)), DAY_WINDOW_MAPPER);
    }

    public List<LocalDate> findMaterializedWeekStarts(LocalDate fromInclusive, LocalDate toExclusive) {
        return jdbc.query(
                "SELECT week_start FROM materialized_weeks "
                        + "WHERE week_start >= :from AND week_start < :to ORDER BY week_start",
                Map.of("from", Date.valueOf(fromInclusive), "to", Date.valueOf(toExclusive)),
                (rs, n) -> rs.getDate("week_start").toLocalDate());
    }

    public boolean hasAnyMaterializedWeek() {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM materialized_weeks)", Map.of(), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Atomically snapshot one week: claim the week marker, and only if newly claimed insert its
     * windows. Idempotent and concurrency-safe via the {@code materialized_weeks} primary key —
     * a concurrent caller that loses the race sees the conflict and makes no changes.
     *
     * @return {@code true} if this call materialized the week, {@code false} if it already existed.
     */
    @Transactional
    public boolean materializeWeek(LocalDate weekStart, List<DayWindow> windows) {
        int claimed = jdbc.update(
                "INSERT INTO materialized_weeks (week_start) VALUES (:ws) ON CONFLICT (week_start) DO NOTHING",
                Map.of("ws", Date.valueOf(weekStart)));
        if (claimed == 0) {
            return false;
        }
        for (DayWindow w : windows) {
            insertDayWindow(w.date(), w.startTime(), w.endTime());
        }
        return true;
    }

    /** Replace all windows for a single date (the per-week edit). */
    @Transactional
    public void replaceDayWindows(LocalDate date, List<DayWindow> windows) {
        jdbc.update("DELETE FROM week_availability WHERE slot_date = :date",
                Map.of("date", Date.valueOf(date)));
        for (DayWindow w : windows) {
            insertDayWindow(date, w.startTime(), w.endTime());
        }
    }

    private void insertDayWindow(LocalDate date, java.time.LocalTime start, java.time.LocalTime end) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("date", Date.valueOf(date))
                .addValue("start", Time.valueOf(start))
                .addValue("end", Time.valueOf(end));
        jdbc.update("""
                INSERT INTO week_availability (id, slot_date, start_time, end_time)
                VALUES (:id, :date, :start, :end)
                """, p);
    }
}
