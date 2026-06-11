package com.kuky.backend.scheduling.repository;

import com.kuky.backend.scheduling.model.AvailabilityException;
import com.kuky.backend.scheduling.model.AvailabilityRule;
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

    public List<AvailabilityException> findUpcomingExceptions(LocalDate fromInclusive) {
        return jdbc.query(
                "SELECT * FROM availability_exceptions WHERE exception_date >= :from "
                        + "ORDER BY exception_date, start_time",
                Map.of("from", Date.valueOf(fromInclusive)), EXCEPTION_MAPPER);
    }

    public List<AvailabilityException> findExceptionsBetween(LocalDate fromInclusive, LocalDate toExclusive) {
        return jdbc.query(
                "SELECT * FROM availability_exceptions "
                        + "WHERE exception_date >= :from AND exception_date < :to "
                        + "ORDER BY exception_date, start_time",
                Map.of("from", Date.valueOf(fromInclusive), "to", Date.valueOf(toExclusive)),
                EXCEPTION_MAPPER);
    }

    public AvailabilityException insertException(AvailabilityException e) {
        UUID id = UUID.randomUUID();
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("date", Date.valueOf(e.getDate()))
                .addValue("kind", e.getKind().name())
                .addValue("startTime", Time.valueOf(e.getStartTime()))
                .addValue("endTime", Time.valueOf(e.getEndTime()));
        jdbc.update("""
                INSERT INTO availability_exceptions (id, exception_date, kind, start_time, end_time)
                VALUES (:id, :date, :kind, :startTime, :endTime)
                """, p);
        e.setId(id);
        return e;
    }

    public int deleteException(UUID id) {
        return jdbc.update("DELETE FROM availability_exceptions WHERE id = :id", Map.of("id", id));
    }
}
