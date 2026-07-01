package com.kuky.backend.placement.service;

import com.kuky.backend.placement.model.PlacementLevelThreshold;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Maps a graded section's overall score percentage to a CEFR level using the
 * teacher-configured thresholds (one minimum score per level, A1..C2): a
 * section is awarded the highest level whose threshold it meets or exceeds.
 * A score below A1's threshold yields "A0" (below A1). The overall estimate
 * is the weakest of the three per-skill levels — a placement is only as
 * strong as its weakest skill.
 */
@Service
public class PlacementScoringService {

    private static final String[] ORDER = {"A1", "A2", "B1", "B2", "C1", "C2"};

    /**
     * "A0" or "A1".."C2" for one section, given its score (0-100) and the
     * teacher-configured thresholds. {@code thresholds} must be ordered A1..C2
     * (as returned by {@code PlacementLevelThresholdRepository.findAll()}).
     */
    public String bandSkill(int scorePercent, List<PlacementLevelThreshold> thresholds) {
        String highestPassing = null;
        for (PlacementLevelThreshold t : thresholds) {
            if (scorePercent >= t.getMinScorePercent()) {
                highestPassing = t.getLevel().name();
            }
        }
        return highestPassing == null ? "A0" : highestPassing;
    }

    /** Overall estimate = the weakest of the per-skill levels ("A0" counts as weakest). */
    public String overall(List<String> perSkillLevels) {
        String weakest = null;
        int weakestRank = Integer.MAX_VALUE;
        for (String level : perSkillLevels) {
            int rank = rank(level);
            if (rank < weakestRank) {
                weakestRank = rank;
                weakest = level;
            }
        }
        return weakest == null ? "A0" : weakest;
    }

    private int rank(String level) {
        if ("A0".equals(level)) return 0;
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i].equals(level)) return i + 1;
        }
        return 0;
    }
}
