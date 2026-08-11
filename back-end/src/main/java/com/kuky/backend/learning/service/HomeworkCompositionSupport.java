package com.kuky.backend.learning.service;

import com.kuky.backend.learning.model.HomeworkComposition;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.QuestionKind;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

/**
 * Derives assignment composition / stored format from type + question kinds,
 * and shared score-percent helpers for auto / mixed grading.
 */
public final class HomeworkCompositionSupport {

    private static final double FULLY_CORRECT_EPSILON = 1e-9;

    private HomeworkCompositionSupport() {}

    public static boolean isAutoGradable(QuestionKind kind) {
        return kind != null && kind != QuestionKind.FREE_TEXT;
    }

    public static HomeworkComposition composition(HomeworkType type, Collection<QuestionKind> kinds) {
        if (type == HomeworkType.WRITE) {
            return HomeworkComposition.WRITE;
        }
        boolean hasManual = false;
        boolean hasAuto = false;
        if (kinds != null) {
            for (QuestionKind kind : kinds) {
                if (kind == QuestionKind.FREE_TEXT) hasManual = true;
                else if (kind != null) hasAuto = true;
            }
        }
        if (hasManual && hasAuto) return HomeworkComposition.MIXED;
        if (hasAuto) return HomeworkComposition.ALL_AUTO;
        return HomeworkComposition.ALL_MANUAL;
    }

    public static HomeworkComposition compositionFromQuestions(
            HomeworkType type, Collection<? extends HasKind> questions) {
        return composition(type, kindsOf(questions));
    }

    /** Activities have no WRITE type — composition from question kinds only. */
    public static HomeworkComposition activityComposition(Collection<? extends HasKind> questions) {
        return composition(null, kindsOf(questions));
    }

    public static HomeworkFormat formatFromComposition(HomeworkComposition composition) {
        return switch (composition) {
            case WRITE, ALL_MANUAL -> HomeworkFormat.MANUAL;
            case ALL_AUTO -> HomeworkFormat.EXERCISE;
            case MIXED -> HomeworkFormat.MIXED;
        };
    }

    public static HomeworkFormat deriveFormat(HomeworkType type, Collection<? extends HasKind> questions) {
        return formatFromComposition(compositionFromQuestions(type, questions));
    }

    public static HomeworkFormat deriveActivityFormat(Collection<? extends HasKind> questions) {
        return formatFromComposition(activityComposition(questions));
    }

    /** {@code score_percent = round(mean * 100)}. */
    public static int scorePercent(double scoreSum, int total) {
        if (total <= 0) return 0;
        return (int) Math.round((scoreSum / total) * 100);
    }

    public static int scorePercentFromScores(Collection<? extends Number> scores) {
        if (scores == null || scores.isEmpty()) return 0;
        double sum = 0;
        int n = 0;
        for (Number s : scores) {
            if (s == null) continue;
            sum += s.doubleValue();
            n++;
        }
        return scorePercent(sum, n);
    }

    /** Converts a teacher percent (0–100) to a 0–1 contribution for overall mean. */
    public static double teacherPercentAsScore(int percent) {
        return percent / 100.0;
    }

    /**
     * Count of contributions equal to {@code 1.0} (auto correct or teacher 100%).
     * Partials do not count.
     */
    public static int fullyCorrectCount(Collection<? extends Number> scores) {
        if (scores == null || scores.isEmpty()) return 0;
        int count = 0;
        for (Number s : scores) {
            if (s == null) continue;
            if (isFullyCorrect(s)) count++;
        }
        return count;
    }

    public static boolean isFullyCorrect(Number score) {
        if (score == null) return false;
        if (score instanceof BigDecimal bd) {
            return bd.compareTo(BigDecimal.ONE) == 0;
        }
        return Math.abs(score.doubleValue() - 1.0) < FULLY_CORRECT_EPSILON;
    }

    public static BigDecimal scoreAsDecimal(double score) {
        return BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP);
    }

    private static List<QuestionKind> kindsOf(Collection<? extends HasKind> questions) {
        if (questions == null || questions.isEmpty()) return List.of();
        return questions.stream().map(HasKind::kind).toList();
    }

    /** Minimal kind accessor so homework and activity question models can share derivation. */
    @FunctionalInterface
    public interface HasKind {
        QuestionKind kind();
    }
}
