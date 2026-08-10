package com.kuky.backend.learning.service;

import com.kuky.backend.learning.model.HomeworkComposition;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.TeacherValidation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * Derives assignment composition / stored format from type + question kinds,
 * and shared score-percent helpers for auto / mixed grading.
 */
public final class HomeworkCompositionSupport {

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

    public static double teacherValidationScore(TeacherValidation validation) {
        return validation == TeacherValidation.VALIDATED ? 1.0 : 0.0;
    }

    public static double teacherValidationScore(String validation) {
        if (validation == null) return 0.0;
        return teacherValidationScore(TeacherValidation.valueOf(validation));
    }

    public static BigDecimal scoreAsDecimal(double score) {
        return BigDecimal.valueOf(score).setScale(3, java.math.RoundingMode.HALF_UP);
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
