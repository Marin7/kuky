package com.kuky.backend.placement.repository;

import com.kuky.backend.placement.model.CefrLevel;
import com.kuky.backend.placement.model.PlacementLevelThreshold;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** The six {@code placement_level_thresholds} rows (A1..C2), always present (seeded by the migration). */
@Repository
public class PlacementLevelThresholdRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PlacementLevelThresholdRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PlacementLevelThreshold> MAPPER = (rs, n) -> {
        PlacementLevelThreshold t = new PlacementLevelThreshold();
        t.setLevel(CefrLevel.valueOf(rs.getString("level")));
        t.setMinScorePercent(rs.getInt("min_score_percent"));
        return t;
    };

    /** All six thresholds, ordered A1 -> C2. */
    public List<PlacementLevelThreshold> findAll() {
        List<PlacementLevelThreshold> rows = jdbc.query(
                "SELECT * FROM placement_level_thresholds", Map.of(), MAPPER);
        return rows.stream()
                .sorted(Comparator.comparingInt(t -> t.getLevel().ordinal()))
                .toList();
    }

    @Transactional
    public void updateAll(List<PlacementLevelThreshold> thresholds) {
        for (PlacementLevelThreshold t : thresholds) {
            jdbc.update("UPDATE placement_level_thresholds SET min_score_percent = :pct WHERE level = :level",
                    new MapSqlParameterSource()
                            .addValue("pct", t.getMinScorePercent())
                            .addValue("level", t.getLevel().name()));
        }
    }
}
