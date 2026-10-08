package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.Activity;
import com.kuky.backend.learning.model.HomeworkFormat;
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
public class ActivityRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ActivityRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Activity> ACTIVITY_MAPPER = (rs, n) -> {
        Activity a = new Activity();
        a.setId(rs.getObject("id", UUID.class));
        a.setPresentationId(rs.getObject("presentation_id", UUID.class));
        a.setPage(rs.getInt("page"));
        a.setTitle(rs.getString("title"));
        a.setFormat(HomeworkFormat.valueOf(rs.getString("format")));
        a.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        a.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return a;
    };

    public Optional<Activity> findById(UUID id) {
        return jdbc.query("SELECT * FROM activities WHERE id = :id", Map.of("id", id), ACTIVITY_MAPPER)
                .stream().findFirst();
    }

    public Optional<Activity> findByPresentationAndPage(UUID presentationId, int page) {
        return jdbc.query(
                "SELECT * FROM activities WHERE presentation_id = :pid AND page = :page",
                Map.of("pid", presentationId, "page", page), ACTIVITY_MAPPER).stream().findFirst();
    }

    public List<Activity> listByPresentationId(UUID presentationId) {
        return jdbc.query(
                "SELECT * FROM activities WHERE presentation_id = :pid ORDER BY page",
                Map.of("pid", presentationId), ACTIVITY_MAPPER);
    }

    /** Unit that owns the activity's presentation (null for a standalone presentation). */
    public Optional<UUID> findUnitId(UUID activityId) {
        return jdbc.query("""
                SELECT p.unit_id FROM activities a
                JOIN presentations p ON p.id = a.presentation_id
                WHERE a.id = :id
                """, Map.of("id", activityId), (rs, n) -> rs.getObject("unit_id", UUID.class))
                .stream().findFirst();
    }

    public Activity insert(Activity activity) {
        Instant now = Instant.now();
        UUID id = activity.getId() == null ? UUID.randomUUID() : activity.getId();
        jdbc.update("""
                INSERT INTO activities (id, presentation_id, page, title, format, created_at, updated_at)
                VALUES (:id, :pid, :page, :title, :format, :createdAt, :updatedAt)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("pid", activity.getPresentationId())
                .addValue("page", activity.getPage())
                .addValue("title", activity.getTitle())
                .addValue("format", activity.getFormat().name())
                .addValue("createdAt", Timestamp.from(now))
                .addValue("updatedAt", Timestamp.from(now)));
        activity.setId(id);
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);
        return activity;
    }

    public void update(Activity activity) {
        Instant now = Instant.now();
        jdbc.update("""
                UPDATE activities SET title = :title, format = :format, updated_at = :updatedAt
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", activity.getId())
                .addValue("title", activity.getTitle())
                .addValue("format", activity.getFormat().name())
                .addValue("updatedAt", Timestamp.from(now)));
        activity.setUpdatedAt(now);
    }

    public int delete(UUID id) {
        return jdbc.update("DELETE FROM activities WHERE id = :id", Map.of("id", id));
    }

    public int countSubmissions(UUID activityId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM activity_submissions WHERE activity_id = :aid",
                Map.of("aid", activityId), Integer.class);
        return count == null ? 0 : count;
    }

    public int countBeyondPage(UUID presentationId, int maxPage) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM activities WHERE presentation_id = :pid AND page > :maxPage",
                Map.of("pid", presentationId, "maxPage", maxPage), Integer.class);
        return count == null ? 0 : count;
    }

    /** Deletes activities on pages past {@code maxPage} (cascades their questions and submissions). */
    public int deleteBeyondPage(UUID presentationId, int maxPage) {
        return jdbc.update(
                "DELETE FROM activities WHERE presentation_id = :pid AND page > :maxPage",
                Map.of("pid", presentationId, "maxPage", maxPage));
    }
}
