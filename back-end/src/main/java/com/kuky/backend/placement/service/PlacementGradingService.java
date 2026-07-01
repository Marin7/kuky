package com.kuky.backend.placement.service;

import com.kuky.backend.placement.dto.SectionResultResponse;
import com.kuky.backend.placement.dto.SubmitSectionRequest;
import com.kuky.backend.placement.model.PlacementAnswer;
import com.kuky.backend.placement.model.PlacementQuestion;
import com.kuky.backend.placement.model.PlacementQuestionOption;
import com.kuky.backend.placement.model.QuestionKind;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Auto-grades placement questions. Rules mirror learning.ExerciseGradingService
 * exactly (single-choice 0/1, multi-choice partial credit, fill-blank trim +
 * case-insensitive + accent-exact) — duplicated rather than shared because the
 * two domains are intentionally decoupled (research.md §1).
 */
@Service
public class PlacementGradingService {

    public record GradedSection(
            List<PlacementAnswer> answers,
            List<SectionResultResponse.QuestionResultDto> questionResults,
            int scorePercent
    ) {}

    public GradedSection grade(List<PlacementQuestion> questions, SubmitSectionRequest request) {
        Map<UUID, SubmitSectionRequest.AnswerDto> byQuestion = (request == null || request.answers() == null)
                ? Map.of()
                : request.answers().stream()
                    .filter(a -> a.questionId() != null)
                    .collect(Collectors.toMap(SubmitSectionRequest.AnswerDto::questionId, Function.identity(), (a, b) -> a));

        List<PlacementAnswer> answers = new ArrayList<>();
        List<SectionResultResponse.QuestionResultDto> questionResults = new ArrayList<>();
        double scoreSum = 0;

        for (PlacementQuestion q : questions) {
            SubmitSectionRequest.AnswerDto given = byQuestion.get(q.getId());
            GradedAnswer graded = gradeQuestion(q, given);
            scoreSum += graded.score;

            PlacementAnswer answer = new PlacementAnswer();
            answer.setQuestionId(q.getId());
            answer.setAnswerText(graded.answerText);
            answer.setScore(BigDecimal.valueOf(graded.score).setScale(3, RoundingMode.HALF_UP));
            answer.setSelectedOptionIds(graded.selectedOptionIds);
            answers.add(answer);

            questionResults.add(new SectionResultResponse.QuestionResultDto(
                    q.getId(), graded.score, graded.score >= 1.0,
                    correctOptionIds(q), acceptedAnswers(q)));
        }

        int total = questions.size();
        int scorePercent = total == 0 ? 0 : (int) Math.round((scoreSum / total) * 100);
        return new GradedSection(answers, questionResults, scorePercent);
    }

    private record GradedAnswer(double score, String answerText, List<UUID> selectedOptionIds) {}

    private GradedAnswer gradeQuestion(PlacementQuestion q, SubmitSectionRequest.AnswerDto given) {
        return switch (q.getKind()) {
            case SINGLE_CHOICE -> gradeSingleChoice(q, given);
            case MULTI_CHOICE -> gradeMultiChoice(q, given);
            case FILL_BLANK -> gradeFillBlank(q, given);
        };
    }

    private GradedAnswer gradeSingleChoice(PlacementQuestion q, SubmitSectionRequest.AnswerDto given) {
        Set<UUID> selected = selectedFor(q, given);
        Set<UUID> correct = q.getOptions().stream()
                .filter(PlacementQuestionOption::isCorrect).map(PlacementQuestionOption::getId)
                .collect(Collectors.toSet());
        double score = selected.equals(correct) ? 1.0 : 0.0;
        return new GradedAnswer(score, null, new ArrayList<>(selected));
    }

    private GradedAnswer gradeMultiChoice(PlacementQuestion q, SubmitSectionRequest.AnswerDto given) {
        Set<UUID> selected = selectedFor(q, given);
        int n = q.getOptions().size();
        if (n == 0) return new GradedAnswer(0.0, null, new ArrayList<>(selected));
        int rightDecisions = 0;
        for (PlacementQuestionOption o : q.getOptions()) {
            boolean isSelected = selected.contains(o.getId());
            if (o.isCorrect() && isSelected) rightDecisions++;
            else if (!o.isCorrect() && !isSelected) rightDecisions++;
        }
        double score = (double) rightDecisions / n;
        return new GradedAnswer(score, null, new ArrayList<>(selected));
    }

    private GradedAnswer gradeFillBlank(PlacementQuestion q, SubmitSectionRequest.AnswerDto given) {
        String raw = given == null ? null : given.answerText();
        if (raw == null || raw.isBlank()) {
            return new GradedAnswer(0.0, raw, List.of());
        }
        String normalized = normalize(raw);
        boolean matches = q.getOptions().stream()
                .anyMatch(o -> normalize(o.getLabel()).equals(normalized));
        return new GradedAnswer(matches ? 1.0 : 0.0, raw, List.of());
    }

    /** Trim + case-insensitive but accent-exact (no diacritic stripping) — matches ExerciseGradingService. */
    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private Set<UUID> selectedFor(PlacementQuestion q, SubmitSectionRequest.AnswerDto given) {
        if (given == null || given.selectedOptionIds() == null) return Set.of();
        Set<UUID> valid = q.getOptions().stream().map(PlacementQuestionOption::getId).collect(Collectors.toSet());
        Set<UUID> selected = new HashSet<>(given.selectedOptionIds());
        selected.retainAll(valid);
        return selected;
    }

    private static List<UUID> correctOptionIds(PlacementQuestion q) {
        if (q.getKind() == QuestionKind.FILL_BLANK) return List.of();
        return q.getOptions().stream().filter(PlacementQuestionOption::isCorrect).map(PlacementQuestionOption::getId).toList();
    }

    private static List<String> acceptedAnswers(PlacementQuestion q) {
        if (q.getKind() != QuestionKind.FILL_BLANK) return List.of();
        return q.getOptions().stream().map(PlacementQuestionOption::getLabel).toList();
    }
}
