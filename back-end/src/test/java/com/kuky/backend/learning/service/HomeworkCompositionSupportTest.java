package com.kuky.backend.learning.service;

import com.kuky.backend.learning.model.HomeworkComposition;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.QuestionKind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HomeworkCompositionSupportTest {

    @Test
    void writeAlwaysWriteCompositionAndManualFormat() {
        assertThat(HomeworkCompositionSupport.composition(HomeworkType.WRITE, List.of(QuestionKind.FREE_TEXT)))
                .isEqualTo(HomeworkComposition.WRITE);
        assertThat(HomeworkCompositionSupport.formatFromComposition(HomeworkComposition.WRITE))
                .isEqualTo(HomeworkFormat.MANUAL);
    }

    @Test
    void onlyFreeTextIsAllManual() {
        assertThat(HomeworkCompositionSupport.composition(HomeworkType.AUDIO, List.of(QuestionKind.FREE_TEXT)))
                .isEqualTo(HomeworkComposition.ALL_MANUAL);
        assertThat(HomeworkCompositionSupport.formatFromComposition(HomeworkComposition.ALL_MANUAL))
                .isEqualTo(HomeworkFormat.MANUAL);
    }

    @Test
    void onlyStructuredIsAllAuto() {
        assertThat(HomeworkCompositionSupport.composition(
                HomeworkType.GRAMMAR, List.of(QuestionKind.SINGLE_CHOICE, QuestionKind.MULTI_BLANK)))
                .isEqualTo(HomeworkComposition.ALL_AUTO);
        assertThat(HomeworkCompositionSupport.formatFromComposition(HomeworkComposition.ALL_AUTO))
                .isEqualTo(HomeworkFormat.EXERCISE);
    }

    @Test
    void mixedKindsYieldMixed() {
        assertThat(HomeworkCompositionSupport.composition(
                HomeworkType.AUDIO, List.of(QuestionKind.FREE_TEXT, QuestionKind.SINGLE_CHOICE)))
                .isEqualTo(HomeworkComposition.MIXED);
        assertThat(HomeworkCompositionSupport.formatFromComposition(HomeworkComposition.MIXED))
                .isEqualTo(HomeworkFormat.MIXED);
    }

    @Test
    void combinedScorePercentRoundsMean() {
        // 1.0 + 0.0 → 50%
        assertThat(HomeworkCompositionSupport.scorePercentFromScores(List.of(1.0, 0.0))).isEqualTo(50);
        // 100% + 0% + partial auto 0.5 → mean 0.5 → 50%
        assertThat(HomeworkCompositionSupport.scorePercentFromScores(List.of(
                HomeworkCompositionSupport.teacherPercentAsScore(100),
                HomeworkCompositionSupport.teacherPercentAsScore(0),
                0.5))).isEqualTo(50);
        // 1 + 1 + 0 → 67%
        assertThat(HomeworkCompositionSupport.scorePercentFromScores(List.of(
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO))).isEqualTo(67);
        // auto correct + manual 50 → 75%
        assertThat(HomeworkCompositionSupport.scorePercentFromScores(List.of(
                1.0, HomeworkCompositionSupport.teacherPercentAsScore(50)))).isEqualTo(75);
    }

    @Test
    void fullyCorrectCountOnlyAtOne() {
        assertThat(HomeworkCompositionSupport.fullyCorrectCount(List.of(
                1.0, 0.7, HomeworkCompositionSupport.teacherPercentAsScore(100),
                HomeworkCompositionSupport.teacherPercentAsScore(50), BigDecimal.ONE))).isEqualTo(3);
        assertThat(HomeworkCompositionSupport.fullyCorrectCount(List.of(0.99, 0.0))).isEqualTo(0);
        assertThat(HomeworkCompositionSupport.isFullyCorrect(BigDecimal.ONE)).isTrue();
        assertThat(HomeworkCompositionSupport.isFullyCorrect(BigDecimal.valueOf(0.7))).isFalse();
    }
}
