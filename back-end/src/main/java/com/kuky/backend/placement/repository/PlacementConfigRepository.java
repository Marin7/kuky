package com.kuky.backend.placement.repository;

import com.kuky.backend.placement.model.PlacementConfig;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** The singleton placement_config row (one row, seeded by the migration). */
@Repository
public class PlacementConfigRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PlacementConfigRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PlacementConfig> MAPPER = (rs, n) -> {
        PlacementConfig c = new PlacementConfig();
        c.setId(rs.getObject("id", UUID.class));
        c.setReadingTimeSeconds(rs.getInt("reading_time_seconds"));
        c.setListeningTimeSeconds(rs.getInt("listening_time_seconds"));
        c.setGrammarTimeSeconds(rs.getInt("grammar_time_seconds"));
        c.setWritingTimeSeconds(rs.getInt("writing_time_seconds"));
        c.setWritingPrompt(rs.getString("writing_prompt"));
        c.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return c;
    };

    public PlacementConfig get() {
        return jdbc.queryForObject("SELECT * FROM placement_config LIMIT 1", Map.of(), MAPPER);
    }

    public PlacementConfig update(PlacementConfig config) {
        jdbc.update("""
                UPDATE placement_config SET
                    reading_time_seconds = :readingTimeSeconds,
                    listening_time_seconds = :listeningTimeSeconds,
                    grammar_time_seconds = :grammarTimeSeconds,
                    writing_time_seconds = :writingTimeSeconds,
                    writing_prompt = :writingPrompt,
                    updated_at = :updatedAt
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", config.getId())
                .addValue("readingTimeSeconds", config.getReadingTimeSeconds())
                .addValue("listeningTimeSeconds", config.getListeningTimeSeconds())
                .addValue("grammarTimeSeconds", config.getGrammarTimeSeconds())
                .addValue("writingTimeSeconds", config.getWritingTimeSeconds())
                .addValue("writingPrompt", config.getWritingPrompt())
                .addValue("updatedAt", Timestamp.from(Instant.now())));
        return get();
    }
}
