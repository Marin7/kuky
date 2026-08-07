package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.Activity;
import com.kuky.backend.learning.model.ActivityInstructionsFile;
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
        a.setTitle(rs.getString("title"));
        a.setFormat(HomeworkFormat.valueOf(rs.getString("format")));
        a.setLevel(rs.getString("level"));
        a.setHomeworkType(rs.getString("homework_type"));
        a.setPosition(rs.getInt("position"));
        a.setTriggerFileId(rs.getObject("trigger_file_id", UUID.class));
        a.setTriggerPage(rs.getObject("trigger_page", Integer.class));
        a.setInstructionsText(rs.getString("instructions_text") != null
                ? rs.getString("instructions_text") : "");
        a.setYoutubeUrl(rs.getString("youtube_url"));
        a.setImageId(rs.getObject("image_id", UUID.class));
        a.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        a.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return a;
    };

    private static final RowMapper<ActivityInstructionsFile> INSTRUCTIONS_MAPPER = (rs, n) -> {
        ActivityInstructionsFile f = new ActivityInstructionsFile();
        f.setId(rs.getObject("id", UUID.class));
        f.setActivityId(rs.getObject("activity_id", UUID.class));
        f.setOriginalName(rs.getString("original_name"));
        f.setContentType(rs.getString("content_type"));
        f.setByteSize(rs.getLong("byte_size"));
        f.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return f;
    };

    public record ActivityListRow(
            Activity activity,
            String presentationTitle,
            boolean hasInstructions
    ) {}

    public Optional<Activity> findById(UUID id) {
        return jdbc.query("SELECT * FROM activities WHERE id = :id", Map.of("id", id), ACTIVITY_MAPPER)
                .stream().findFirst();
    }

    public List<Activity> listByPresentationId(UUID presentationId) {
        return jdbc.query(
                "SELECT * FROM activities WHERE presentation_id = :pid ORDER BY position",
                Map.of("pid", presentationId), ACTIVITY_MAPPER);
    }

    public List<ActivityListRow> listAll(UUID presentationIdOrNull) {
        String sql = """
                SELECT a.*, p.title AS presentation_title,
                       EXISTS (SELECT 1 FROM activity_instructions_files f WHERE f.activity_id = a.id) AS has_instructions
                FROM activities a
                JOIN presentations p ON p.id = a.presentation_id
                """ + (presentationIdOrNull != null ? " WHERE a.presentation_id = :pid" : "") + """
                 ORDER BY p.title, a.position
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (presentationIdOrNull != null) {
            params.addValue("pid", presentationIdOrNull);
        }
        return jdbc.query(sql, params, (rs, n) -> {
            Activity a = ACTIVITY_MAPPER.mapRow(rs, n);
            return new ActivityListRow(a, rs.getString("presentation_title"), rs.getBoolean("has_instructions"));
        });
    }

    public int maxPosition(UUID presentationId) {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(position), -1) FROM activities WHERE presentation_id = :pid",
                Map.of("pid", presentationId), Integer.class);
        return max == null ? -1 : max;
    }

    public Activity insert(Activity activity) {
        Instant now = Instant.now();
        UUID id = activity.getId() == null ? UUID.randomUUID() : activity.getId();
        jdbc.update("""
                INSERT INTO activities
                    (id, presentation_id, title, format, level, homework_type, position,
                     trigger_file_id, trigger_page, instructions_text, youtube_url, image_id,
                     created_at, updated_at)
                VALUES
                    (:id, :pid, :title, :format, :level, :homeworkType, :position,
                     :triggerFileId, :triggerPage, :instructionsText, :youtubeUrl, :imageId,
                     :createdAt, :updatedAt)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("pid", activity.getPresentationId())
                .addValue("title", activity.getTitle())
                .addValue("format", activity.getFormat().name())
                .addValue("level", activity.getLevel())
                .addValue("homeworkType", activity.getHomeworkType())
                .addValue("position", activity.getPosition())
                .addValue("triggerFileId", activity.getTriggerFileId())
                .addValue("triggerPage", activity.getTriggerPage())
                .addValue("instructionsText",
                        activity.getInstructionsText() != null ? activity.getInstructionsText() : "")
                .addValue("youtubeUrl", activity.getYoutubeUrl())
                .addValue("imageId", activity.getImageId())
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
                UPDATE activities SET
                    presentation_id = :pid,
                    title = :title,
                    format = :format,
                    level = :level,
                    homework_type = :homeworkType,
                    position = :position,
                    trigger_file_id = :triggerFileId,
                    trigger_page = :triggerPage,
                    instructions_text = :instructionsText,
                    youtube_url = :youtubeUrl,
                    image_id = :imageId,
                    updated_at = :updatedAt
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", activity.getId())
                .addValue("pid", activity.getPresentationId())
                .addValue("title", activity.getTitle())
                .addValue("format", activity.getFormat().name())
                .addValue("level", activity.getLevel())
                .addValue("homeworkType", activity.getHomeworkType())
                .addValue("position", activity.getPosition())
                .addValue("triggerFileId", activity.getTriggerFileId())
                .addValue("triggerPage", activity.getTriggerPage())
                .addValue("instructionsText",
                        activity.getInstructionsText() != null ? activity.getInstructionsText() : "")
                .addValue("youtubeUrl", activity.getYoutubeUrl())
                .addValue("imageId", activity.getImageId())
                .addValue("updatedAt", Timestamp.from(now)));
        activity.setUpdatedAt(now);
    }

    public int delete(UUID id) {
        return jdbc.update("DELETE FROM activities WHERE id = :id", Map.of("id", id));
    }

    /** Rewrite positions to 0..n-1 in the given order. */
    public void reorderPositions(UUID presentationId, List<UUID> orderedIds) {
        Instant now = Instant.now();
        for (int i = 0; i < orderedIds.size(); i++) {
            jdbc.update("""
                    UPDATE activities SET position = :pos, updated_at = :updatedAt
                    WHERE id = :id AND presentation_id = :pid
                    """, new MapSqlParameterSource()
                    .addValue("pos", i)
                    .addValue("updatedAt", Timestamp.from(now))
                    .addValue("id", orderedIds.get(i))
                    .addValue("pid", presentationId));
        }
    }

    public List<UUID> findInstructionFileIdsByPresentationId(UUID presentationId) {
        return jdbc.query("""
                SELECT f.id FROM activity_instructions_files f
                JOIN activities a ON a.id = f.activity_id
                WHERE a.presentation_id = :pid
                """, Map.of("pid", presentationId),
                (rs, n) -> rs.getObject("id", UUID.class));
    }

    public void clearTriggerForFile(UUID fileId) {
        jdbc.update("""
                UPDATE activities
                SET trigger_file_id = NULL, trigger_page = NULL, updated_at = :updatedAt
                WHERE trigger_file_id = :fid
                """, new MapSqlParameterSource()
                .addValue("fid", fileId)
                .addValue("updatedAt", Timestamp.from(Instant.now())));
    }

    public void clearTriggerOnActivity(UUID activityId) {
        jdbc.update("""
                UPDATE activities
                SET trigger_file_id = NULL, trigger_page = NULL, updated_at = :updatedAt
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", activityId)
                .addValue("updatedAt", Timestamp.from(Instant.now())));
    }

    public Optional<ActivityInstructionsFile> findInstructionsByActivityId(UUID activityId) {
        return jdbc.query(
                "SELECT * FROM activity_instructions_files WHERE activity_id = :aid",
                Map.of("aid", activityId), INSTRUCTIONS_MAPPER).stream().findFirst();
    }

    public ActivityInstructionsFile insertInstructions(ActivityInstructionsFile file) {
        UUID id = file.getId() == null ? UUID.randomUUID() : file.getId();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO activity_instructions_files
                    (id, activity_id, original_name, content_type, byte_size, created_at)
                VALUES (:id, :aid, :name, :ct, :size, :createdAt)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("aid", file.getActivityId())
                .addValue("name", file.getOriginalName())
                .addValue("ct", file.getContentType())
                .addValue("size", file.getByteSize())
                .addValue("createdAt", Timestamp.from(now)));
        file.setId(id);
        file.setCreatedAt(now);
        return file;
    }

    public void deleteInstructionsByActivityId(UUID activityId) {
        jdbc.update("DELETE FROM activity_instructions_files WHERE activity_id = :aid",
                Map.of("aid", activityId));
    }

    public boolean presentationExists(UUID presentationId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM presentations WHERE id = :id",
                Map.of("id", presentationId), Integer.class);
        return count != null && count > 0;
    }

    public boolean fileBelongsToPresentation(UUID presentationId, UUID fileId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(1) FROM presentation_files
                WHERE presentation_id = :pid AND id = :fid
                """, Map.of("pid", presentationId, "fid", fileId), Integer.class);
        return count != null && count > 0;
    }
}
