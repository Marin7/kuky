package com.kuky.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.admin.dto.StudentMistakesResponse;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.admin.service.StudentMistakesAdminService;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.ExerciseResultResponse.QuestionResultDto;
import com.kuky.backend.learning.dto.ExerciseResultResponse.UnitResultDto;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository.AutoGradedSubmissionRow;
import com.kuky.backend.learning.service.ExerciseGradingService;
import com.kuky.backend.learning.service.HomeworkCompositionSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentMistakesAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");

    private UserRepository userRepository;
    private HomeworkSubmissionRepository submissionRepository;
    private HomeworkQuestionRepository questionRepository;
    private ExerciseGradingService grading;
    private StudentMistakesAdminService service;

    private final UUID studentId = UUID.randomUUID();
    private final List<AutoGradedSubmissionRow> rows = new ArrayList<>();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        submissionRepository = mock(HomeworkSubmissionRepository.class);
        questionRepository = mock(HomeworkQuestionRepository.class);
        grading = mock(ExerciseGradingService.class);
        service = new StudentMistakesAdminService(userRepository, submissionRepository, questionRepository,
                grading, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

        User student = new User();
        student.setId(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(submissionRepository.findAutoGradedSubmittedByUser(studentId)).thenReturn(rows);
    }

    // --- fixtures ---------------------------------------------------------------

    private static HomeworkQuestion question(QuestionKind kind, String prompt) {
        HomeworkQuestion q = new HomeworkQuestion();
        q.setId(UUID.randomUUID());
        q.setKind(kind);
        q.setPrompt(prompt);
        q.setStructureJson("{}");
        return q;
    }

    private static QuestionResultDto result(HomeworkQuestion q, boolean correct) {
        return new QuestionResultDto(q.getId(), correct ? 1.0 : 0.0, correct,
                List.of(), List.of(), List.of(), List.of());
    }

    private static QuestionResultDto numberedResult(HomeworkQuestion q, boolean... itemsCorrect) {
        List<UnitResultDto> units = new ArrayList<>();
        int right = 0;
        for (int i = 0; i < itemsCorrect.length; i++) {
            units.add(new UnitResultDto(i, itemsCorrect[i] ? 1.0 : 0.0, itemsCorrect[i], "x", List.of("y")));
            if (itemsCorrect[i]) right++;
        }
        double score = itemsCorrect.length == 0 ? 0.0 : (double) right / itemsCorrect.length;
        return new QuestionResultDto(q.getId(), score, score >= 1.0, List.of(), List.of(), units, List.of());
    }

    /** Registers one submitted homework (no snapshot) whose auto results are {@code results}. */
    private HomeworkSubmission submit(Instant submittedAt, List<String> labels, HomeworkFormat format,
                                      List<HomeworkQuestion> questions, Map<UUID, QuestionResultDto> results) {
        HomeworkSubmission s = new HomeworkSubmission();
        s.setId(UUID.randomUUID());
        s.setUserId(studentId);
        s.setAssignmentId(UUID.randomUUID());
        s.setStatus("GRADED");
        s.setSubmittedAt(submittedAt);
        stub(s, questions, results);
        when(questionRepository.findByAssignment(s.getAssignmentId())).thenReturn(questions);
        rows.add(new AutoGradedSubmissionRow(s, format, "Tarea " + rows.size(), labels));
        return s;
    }

    private void stub(HomeworkSubmission s, List<HomeworkQuestion> questions, Map<UUID, QuestionResultDto> results) {
        List<QuestionResultDto> ordered = questions.stream()
                .filter(q -> results.containsKey(q.getId()))
                .map(q -> results.get(q.getId()))
                .toList();
        when(grading.storedProvisionalResultFor(same(questions), same(s)))
                .thenReturn(new ExerciseResultResponse(0, 0, ordered.size(), ordered));
        when(grading.studentQuestionsFor(same(questions))).thenReturn(questions.stream()
                .map(q -> new ExerciseQuestionDto(q.getId(), q.getKind().name(), q.getPrompt(), List.of(), null))
                .toList());
    }

    /** A homework with {@code wrong} wrong and {@code right} right single-choice questions. */
    private HomeworkSubmission simple(Instant submittedAt, List<String> labels, int wrong, int right) {
        List<HomeworkQuestion> qs = new ArrayList<>();
        Map<UUID, QuestionResultDto> results = new java.util.HashMap<>();
        for (int i = 0; i < wrong + right; i++) {
            HomeworkQuestion q = question(QuestionKind.SINGLE_CHOICE, "Pregunta " + i);
            qs.add(q);
            results.put(q.getId(), result(q, i >= wrong));
        }
        return submit(submittedAt, labels, HomeworkFormat.EXERCISE, qs, results);
    }

    private StudentMistakesResponse get() {
        return service.getMistakes(studentId, null, null, null);
    }

    // --- US1: derivation, counting, ordering, paging ------------------------------

    @Test
    void onlyWrongAutoGradedQuestionsBecomeEntries_andFreeTextIsIgnored() {
        HomeworkQuestion wrong = question(QuestionKind.SINGLE_CHOICE, "¿Ser o estar?");
        HomeworkQuestion right = question(QuestionKind.TRUE_FALSE, "Verdadero");
        HomeworkQuestion free = question(QuestionKind.FREE_TEXT, "Escribe");
        submit(NOW.minusSeconds(60), List.of(), HomeworkFormat.MIXED, List.of(wrong, right, free),
                Map.of(wrong.getId(), result(wrong, false), right.getId(), result(right, true)));

        StudentMistakesResponse r = get();

        assertThat(r.entries()).extracting(e -> e.question().id()).containsExactly(wrong.getId());
        assertThat(r.entries().get(0).homeworkFormat()).isEqualTo("MIXED");
        assertThat(r.entries().get(0).questionPosition()).isEqualTo(1);
        assertThat(r.summary().mistakeCount()).isEqualTo(1);
        assertThat(r.summary().answeredCount()).isEqualTo(2); // free text not counted
        assertThat(r.allTimeMistakeCount()).isEqualTo(1);
    }

    @Test
    void numberedSingleChoice_countsEachWrongItem_butIsOneEntry() {
        HomeworkQuestion numbered = question(QuestionKind.SINGLE_CHOICE, "(1) es (2) está (3) son");
        submit(NOW, List.of(), HomeworkFormat.EXERCISE, List.of(numbered),
                Map.of(numbered.getId(), numberedResult(numbered, false, true, false)));

        StudentMistakesResponse r = get();

        assertThat(r.entries()).hasSize(1);
        assertThat(r.entries().get(0).mistakeCount()).isEqualTo(2);
        assertThat(r.summary().mistakeCount()).isEqualTo(2);
        assertThat(r.summary().answeredCount()).isEqualTo(3);
        assertThat(r.totalEntries()).isEqualTo(1);
    }

    @Test
    void numberedSingleChoice_withoutStoredAnswer_countsEveryItemWrong_likeScoreContributions() {
        HomeworkQuestion numbered = question(QuestionKind.SINGLE_CHOICE, "(1) a (2) b (3) c (4) d");
        submit(NOW, List.of(), HomeworkFormat.EXERCISE, List.of(numbered),
                Map.of(numbered.getId(), numberedResult(numbered)));

        StudentMistakesResponse r = get();

        int contributions = HomeworkCompositionSupport.contributions(numbered, null).size();
        assertThat(contributions).isEqualTo(4);
        assertThat(r.summary().answeredCount()).isEqualTo(contributions);
        assertThat(r.summary().mistakeCount()).isEqualTo(contributions);
    }

    @Test
    void missingAnswerOnNormalQuestion_countsAsWrong() {
        HomeworkQuestion q = question(QuestionKind.MULTI_BLANK, "Yo ___ (ser)");
        QuestionResultDto unanswered = new QuestionResultDto(q.getId(), 0.0, false,
                List.of(), List.of(), List.of(new UnitResultDto(0, 0.0, false, "", List.of("soy"))), List.of());
        submit(NOW, List.of(), HomeworkFormat.EXERCISE, List.of(q), Map.of(q.getId(), unanswered));

        assertThat(get().entries()).hasSize(1);
    }

    @Test
    void entriesAreNewestFirst_thenInQuestionOrder() {
        HomeworkSubmission older = simple(NOW.minus(Duration.ofDays(2)), List.of(), 1, 0);
        HomeworkSubmission newer = simple(NOW.minus(Duration.ofDays(1)), List.of(), 2, 0);

        StudentMistakesResponse r = get();

        assertThat(r.entries()).extracting(StudentMistakesResponse.Entry::submissionId)
                .containsExactly(newer.getId(), newer.getId(), older.getId());
        assertThat(r.entries()).extracting(StudentMistakesResponse.Entry::questionPosition)
                .containsExactly(1, 2, 1);
    }

    @Test
    void pagesHoldTwentyEntries_andOutOfRangePagesAreClamped() {
        simple(NOW, List.of(), 45, 0);

        assertThat(get().totalPages()).isEqualTo(3);
        assertThat(get().entries()).hasSize(20);

        StudentMistakesResponse last = service.getMistakes(studentId, null, null, "3");
        assertThat(last.page()).isEqualTo(3);
        assertThat(last.entries()).hasSize(5);

        StudentMistakesResponse beyond = service.getMistakes(studentId, null, null, "9");
        assertThat(beyond.page()).isEqualTo(3);
        assertThat(beyond.entries()).hasSize(5);

        StudentMistakesResponse garbage = service.getMistakes(studentId, null, null, "abc");
        assertThat(garbage.page()).isEqualTo(1);
        assertThat(garbage.pageSize()).isEqualTo(20);
    }

    @Test
    void emptyHistory_servesPageOneOfOne() {
        StudentMistakesResponse r = get();

        assertThat(r.page()).isEqualTo(1);
        assertThat(r.totalPages()).isEqualTo(1);
        assertThat(r.entries()).isEmpty();
        assertThat(r.summary().answeredCount()).isZero();
    }

    @Test
    void snapshotTitleAndQuestionsAreUsed_whenPresent() {
        UUID qid = UUID.randomUUID();
        HomeworkSubmission s = new HomeworkSubmission();
        s.setId(UUID.randomUUID());
        s.setUserId(studentId);
        s.setAssignmentId(UUID.randomUUID());
        s.setStatus("GRADED");
        s.setSubmittedAt(NOW);
        s.setAssignmentSnapshot("""
                {"title":"Título original","questions":[
                  {"id":"%s","position":0,"kind":"SINGLE_CHOICE","prompt":"Texto original","structure":{},"options":[]}
                ]}
                """.formatted(qid));
        rows.add(new AutoGradedSubmissionRow(s, HomeworkFormat.EXERCISE, "Título editado", List.of()));
        HomeworkQuestion probe = question(QuestionKind.SINGLE_CHOICE, "x");
        probe.setId(qid);
        when(grading.storedProvisionalResultFor(any(), same(s)))
                .thenReturn(new ExerciseResultResponse(0, 0, 1, List.of(result(probe, false))));
        when(grading.studentQuestionsFor(any())).thenAnswer(inv -> {
            List<HomeworkQuestion> qs = inv.getArgument(0);
            return qs.stream().map(q -> new ExerciseQuestionDto(q.getId(), q.getKind().name(), q.getPrompt(),
                    List.of(), null)).toList();
        });

        StudentMistakesResponse r = get();

        assertThat(r.entries()).hasSize(1);
        assertThat(r.entries().get(0).homeworkTitle()).isEqualTo("Título original");
        assertThat(r.entries().get(0).question().prompt()).isEqualTo("Texto original");
        verify(questionRepository, never()).findByAssignment(any());
    }

    @Test
    void unknownStudent_throwsNotFound() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMistakes(unknown, null, null, null))
                .isInstanceOf(StudentNotFoundException.class);
    }

    // --- US2: label filter ----------------------------------------------------------

    @Test
    void labelOptions_areCaseInsensitive_firstSeenNewestFirst_sortedByKey() {
        simple(NOW.minus(Duration.ofDays(1)), List.of("subjuntivo", "Vocabulario"), 1, 0);
        simple(NOW, List.of("Subjuntivo"), 1, 0);
        simple(NOW.minus(Duration.ofDays(3)), List.of(), 1, 0);

        assertThat(get().labelOptions()).containsExactly(
                new StudentMistakesResponse.LabelOption("subjuntivo", "Subjuntivo"),
                new StudentMistakesResponse.LabelOption("vocabulario", "Vocabulario"));
    }

    @Test
    void labelFilter_matchesAnyOfSeveralLabels_andAllLabelsIncludesUnlabelled() {
        HomeworkSubmission tagged = simple(NOW, List.of("Unidad 3", "Subjuntivo"), 2, 1);
        simple(NOW.minus(Duration.ofDays(1)), List.of("Vocabulario"), 1, 5);
        simple(NOW.minus(Duration.ofDays(2)), List.of(), 1, 0);

        StudentMistakesResponse filtered = service.getMistakes(studentId, null, "subjuntivo", null);
        assertThat(filtered.appliedLabel()).isEqualTo("subjuntivo");
        assertThat(filtered.entries()).extracting(StudentMistakesResponse.Entry::submissionId)
                .containsOnly(tagged.getId());
        assertThat(filtered.summary().mistakeCount()).isEqualTo(2);
        assertThat(filtered.summary().answeredCount()).isEqualTo(3);
        assertThat(filtered.allTimeMistakeCount()).isEqualTo(4);

        StudentMistakesResponse all = get();
        assertThat(all.appliedLabel()).isNull();
        assertThat(all.summary().mistakeCount()).isEqualTo(4);
    }

    @Test
    void unknownLabel_fallsBackToAllLabels() {
        simple(NOW, List.of("Subjuntivo"), 1, 0);
        simple(NOW, List.of(), 1, 0);

        StudentMistakesResponse r = service.getMistakes(studentId, null, "gone", null);

        assertThat(r.appliedLabel()).isNull();
        assertThat(r.entries()).hasSize(2);
    }

    // --- US3: period filter ---------------------------------------------------------

    @Test
    void periodFilter_includesTheBoundary_andCombinesWithLabel() {
        HomeworkSubmission boundary = simple(NOW.minus(Duration.ofDays(30)), List.of("Subjuntivo"), 1, 0);
        HomeworkSubmission justOut = simple(NOW.minus(Duration.ofDays(30)).minusSeconds(1), List.of("Subjuntivo"), 1, 0);
        HomeworkSubmission old = simple(NOW.minus(Duration.ofDays(120)), List.of("Léxico"), 1, 0);

        StudentMistakesResponse d30 = service.getMistakes(studentId, "DAYS_30", null, null);
        assertThat(d30.appliedPeriod()).isEqualTo("DAYS_30");
        assertThat(d30.entries()).extracting(StudentMistakesResponse.Entry::submissionId)
                .containsExactly(boundary.getId());

        StudentMistakesResponse d90 = service.getMistakes(studentId, "DAYS_90", null, null);
        assertThat(d90.entries()).extracting(StudentMistakesResponse.Entry::submissionId)
                .containsExactly(boundary.getId(), justOut.getId());

        StudentMistakesResponse d90Lexico = service.getMistakes(studentId, "DAYS_90", "léxico", null);
        assertThat(d90Lexico.entries()).isEmpty();
        assertThat(d90Lexico.summary().answeredCount()).isZero();

        StudentMistakesResponse allLexico = service.getMistakes(studentId, "ALL", "léxico", null);
        assertThat(allLexico.entries()).extracting(StudentMistakesResponse.Entry::submissionId)
                .containsExactly(old.getId());
    }

    @Test
    void period_doesNotChangeAllTimeCountOrLabelOptions_andUnknownPeriodMeansAll() {
        simple(NOW, List.of("Subjuntivo"), 1, 0);
        simple(NOW.minus(Duration.ofDays(200)), List.of("Léxico"), 2, 0);

        StudentMistakesResponse d30 = service.getMistakes(studentId, "DAYS_30", null, null);
        assertThat(d30.allTimeMistakeCount()).isEqualTo(3);
        assertThat(d30.summary().mistakeCount()).isEqualTo(1);
        assertThat(d30.labelOptions()).hasSize(2);

        StudentMistakesResponse bogus = service.getMistakes(studentId, "LAST_YEAR", null, null);
        assertThat(bogus.appliedPeriod()).isEqualTo("ALL");
        assertThat(bogus.summary().mistakeCount()).isEqualTo(3);
    }
}
