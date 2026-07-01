package com.kuky.backend.placement;

import com.kuky.backend.placement.model.CefrLevel;
import com.kuky.backend.placement.model.PlacementLevelThreshold;
import com.kuky.backend.placement.service.PlacementScoringService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Deterministic score-to-CEFR banding via teacher-configured thresholds, and weakest-skill overall. */
class PlacementScoringServiceTest {

    private final PlacementScoringService service = new PlacementScoringService();

    // Matches the seeded defaults (V20): A1=0, A2=20, B1=40, B2=60, C1=75, C2=90.
    private static final List<PlacementLevelThreshold> DEFAULT_THRESHOLDS = List.of(
            threshold(CefrLevel.A1, 0), threshold(CefrLevel.A2, 20), threshold(CefrLevel.B1, 40),
            threshold(CefrLevel.B2, 60), threshold(CefrLevel.C1, 75), threshold(CefrLevel.C2, 90));

    private static PlacementLevelThreshold threshold(CefrLevel level, int minScorePercent) {
        PlacementLevelThreshold t = new PlacementLevelThreshold();
        t.setLevel(level);
        t.setMinScorePercent(minScorePercent);
        return t;
    }

    @Test
    void scoreAtOrAboveHighestThreshold_bandsC2() {
        assertThat(service.bandSkill(95, DEFAULT_THRESHOLDS)).isEqualTo("C2");
        assertThat(service.bandSkill(90, DEFAULT_THRESHOLDS)).isEqualTo("C2");
    }

    @Test
    void scoreExactlyAtAThreshold_bandsThatLevel() {
        assertThat(service.bandSkill(60, DEFAULT_THRESHOLDS)).isEqualTo("B2");
    }

    @Test
    void scoreJustBelowAThreshold_bandsThePreviousLevel() {
        assertThat(service.bandSkill(59, DEFAULT_THRESHOLDS)).isEqualTo("B1");
    }

    @Test
    void scoreBelowA1Threshold_bandsA0() {
        List<PlacementLevelThreshold> thresholds = List.of(
                threshold(CefrLevel.A1, 10), threshold(CefrLevel.A2, 20), threshold(CefrLevel.B1, 40),
                threshold(CefrLevel.B2, 60), threshold(CefrLevel.C1, 75), threshold(CefrLevel.C2, 90));
        assertThat(service.bandSkill(5, thresholds)).isEqualTo("A0");
    }

    @Test
    void zeroScore_bandsA0WhenA1ThresholdIsAboveZero() {
        List<PlacementLevelThreshold> thresholds = List.of(
                threshold(CefrLevel.A1, 1), threshold(CefrLevel.A2, 20), threshold(CefrLevel.B1, 40),
                threshold(CefrLevel.B2, 60), threshold(CefrLevel.C1, 75), threshold(CefrLevel.C2, 90));
        assertThat(service.bandSkill(0, thresholds)).isEqualTo("A0");
    }

    @Test
    void zeroScore_bandsA1WhenA1ThresholdIsZero() {
        assertThat(service.bandSkill(0, DEFAULT_THRESHOLDS)).isEqualTo("A1");
    }

    @Test
    void identicalScoresProduceIdenticalLevels() {
        String first = service.bandSkill(72, DEFAULT_THRESHOLDS);
        String second = service.bandSkill(72, DEFAULT_THRESHOLDS);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void overallIsTheWeakestSkill() {
        assertThat(service.overall(List.of("B1", "A2", "C1"))).isEqualTo("A2");
    }

    @Test
    void overallTreatsA0AsWeakestPossible() {
        assertThat(service.overall(List.of("B1", "A0", "C1"))).isEqualTo("A0");
    }
}
