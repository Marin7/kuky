package com.kuky.backend.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.admin.dto.StudentMistakesResponse;
import com.kuky.backend.admin.dto.StudentMistakesResponse.Entry;
import com.kuky.backend.admin.dto.StudentMistakesResponse.LabelOption;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository.AutoGradedSubmissionRow;
import com.kuky.backend.learning.service.AssignmentSnapshot;
import com.kuky.backend.learning.service.ExerciseGradingService;
import com.kuky.backend.learning.service.HomeworkCompositionSupport;
import com.kuky.backend.learning.service.SingleChoiceItems;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin "Errores frecuentes": a student's wrong answers on auto-graded homework
 * questions, derived on every request from the frozen submission snapshot, the
 * stored per-answer scores and the homework's current labels (no table of its own).
 * Strictly read-only — it must never touch notification / seen state.
 */
@Service
public class StudentMistakesAdminService {

    static final int PAGE_SIZE = 20;

    public enum Period {
        ALL(0), DAYS_30(30), DAYS_90(90);

        private final int days;

        Period(int days) { this.days = days; }
    }

    private final UserRepository userRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkQuestionRepository questionRepository;
    private final ExerciseGradingService exerciseGradingService;
    private final AssignmentSnapshot assignmentSnapshot;
    private final Clock clock;

    public StudentMistakesAdminService(UserRepository userRepository,
                                       HomeworkSubmissionRepository submissionRepository,
                                       HomeworkQuestionRepository questionRepository,
                                       ExerciseGradingService exerciseGradingService,
                                       ObjectMapper objectMapper,
                                       Clock clock) {
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.questionRepository = questionRepository;
        this.exerciseGradingService = exerciseGradingService;
        this.assignmentSnapshot = new AssignmentSnapshot(objectMapper);
        this.clock = clock;
    }

    /** Parameters arrive as raw strings and are normalised leniently (never a 400). */
    public StudentMistakesResponse getMistakes(UUID studentId, String period, String labelKey, String page) {
        userRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));

        Period appliedPeriod = parsePeriod(period);
        int requestedPage = parsePage(page);
        String requestedLabel = labelKey(labelKey);

        List<SubmissionMistakes> all = submissionRepository.findAutoGradedSubmittedByUser(studentId).stream()
                .map(this::toSubmissionMistakes)
                .sorted(Comparator.comparing(SubmissionMistakes::submittedAt).reversed())
                .toList();

        List<LabelOption> labelOptions = labelOptions(all);
        String appliedLabel = requestedLabel != null
                && labelOptions.stream().anyMatch(o -> o.key().equals(requestedLabel))
                ? requestedLabel : null;

        Instant cutoff = appliedPeriod == Period.ALL
                ? null
                : clock.instant().minus(Duration.ofDays(appliedPeriod.days));
        List<SubmissionMistakes> filtered = all.stream()
                .filter(s -> cutoff == null || !s.submittedAt().isBefore(cutoff))
                .filter(s -> appliedLabel == null || s.hasLabel(appliedLabel))
                .toList();

        List<Entry> entries = filtered.stream()
                .flatMap(s -> s.entries().stream())
                .sorted(Comparator.comparing(Entry::submittedAt).reversed()
                        .thenComparingInt(Entry::questionPosition))
                .toList();

        int totalEntries = entries.size();
        int totalPages = Math.max(1, (totalEntries + PAGE_SIZE - 1) / PAGE_SIZE);
        int servedPage = Math.min(requestedPage, totalPages);
        int from = (servedPage - 1) * PAGE_SIZE;
        List<Entry> pageEntries = entries.subList(from, Math.min(from + PAGE_SIZE, totalEntries));

        return new StudentMistakesResponse(
                all.stream().mapToInt(SubmissionMistakes::mistakeCount).sum(),
                new StudentMistakesResponse.Summary(
                        filtered.stream().mapToInt(SubmissionMistakes::mistakeCount).sum(),
                        filtered.stream().mapToInt(SubmissionMistakes::answeredCount).sum()),
                labelOptions,
                appliedPeriod.name(),
                appliedLabel,
                servedPage,
                PAGE_SIZE,
                totalPages,
                totalEntries,
                pageEntries);
    }

    /** One qualifying submission: its auto-graded answered count and its wrong-question entries. */
    private record SubmissionMistakes(Instant submittedAt, List<String> labels,
                                      int answeredCount, List<Entry> entries) {
        int mistakeCount() {
            return entries.stream().mapToInt(Entry::mistakeCount).sum();
        }

        boolean hasLabel(String key) {
            return labels.stream().anyMatch(l -> key.equals(labelKey(l)));
        }
    }

    private SubmissionMistakes toSubmissionMistakes(AutoGradedSubmissionRow row) {
        HomeworkSubmission submission = row.submission();
        List<HomeworkQuestion> questions = assignmentSnapshot.present(submission)
                ? assignmentSnapshot.questionsOf(submission)
                : questionRepository.findByAssignment(submission.getAssignmentId());

        // Title as the student saw it; labels are always the current ones.
        HomeworkAssignment titleHolder = new HomeworkAssignment();
        titleHolder.setTitle(row.currentTitle());
        assignmentSnapshot.applyContent(titleHolder, submission);

        Map<UUID, ExerciseResultResponse.QuestionResultDto> resultById =
                exerciseGradingService.storedProvisionalResultFor(questions, submission).questions().stream()
                        .collect(Collectors.toMap(ExerciseResultResponse.QuestionResultDto::questionId,
                                Function.identity(), (a, b) -> a));
        Map<UUID, ExerciseQuestionDto> studentQuestionById =
                exerciseGradingService.studentQuestionsFor(questions).stream()
                        .collect(Collectors.toMap(ExerciseQuestionDto::id, Function.identity(), (a, b) -> a));

        int answered = 0;
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            HomeworkQuestion q = questions.get(i);
            if (!HomeworkCompositionSupport.isAutoGradable(q.getKind())) continue;
            ExerciseResultResponse.QuestionResultDto result = resultById.get(q.getId());
            if (result == null) continue;

            int[] counts = answeredAndWrong(q, result);
            answered += counts[0];
            if (counts[1] == 0) continue;
            entries.add(new Entry(
                    submission.getId(),
                    submission.getAssignmentId(),
                    titleHolder.getTitle(),
                    row.format().name(),
                    row.labels(),
                    submission.getSubmittedAt(),
                    i + 1,
                    counts[1],
                    studentQuestionById.get(q.getId()),
                    result));
        }
        return new SubmissionMistakes(submission.getSubmittedAt(), row.labels(), answered, entries);
    }

    /**
     * {answered, wrong} contributions for one question — mirrors
     * {@link HomeworkCompositionSupport#contributions}: a numbered single choice
     * counts per item (N zeros when nothing was stored), any other kind counts once.
     */
    static int[] answeredAndWrong(HomeworkQuestion q, ExerciseResultResponse.QuestionResultDto result) {
        if (SingleChoiceItems.isNumbered(q)) {
            List<ExerciseResultResponse.UnitResultDto> units = result.unitResults();
            if (units == null || units.isEmpty()) {
                int n = SingleChoiceItems.itemCount(q);
                return new int[] {n, n};
            }
            int wrong = (int) units.stream().filter(u -> !u.correct()).count();
            return new int[] {units.size(), wrong};
        }
        return new int[] {1, result.correct() ? 0 : 1};
    }

    /** Case-insensitive label groups over every period; first spelling seen (newest first) wins. */
    private static List<LabelOption> labelOptions(List<SubmissionMistakes> newestFirst) {
        Map<String, String> byKey = new LinkedHashMap<>();
        for (SubmissionMistakes s : newestFirst) {
            for (String label : s.labels()) {
                String key = labelKey(label);
                if (key != null) byKey.putIfAbsent(key, label.trim());
            }
        }
        return byKey.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new LabelOption(e.getKey(), e.getValue()))
                .toList();
    }

    static String labelKey(String label) {
        if (label == null) return null;
        String trimmed = label.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    static Period parsePeriod(String raw) {
        if (raw == null) return Period.ALL;
        try {
            return Period.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Period.ALL;
        }
    }

    static int parsePage(String raw) {
        if (raw == null) return 1;
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
