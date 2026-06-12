package com.kuky.backend.learning;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.dto.SubmitExerciseRequest.AnswerDto;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.learning.service.ExerciseGradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExerciseGradingServiceTest {

    private ContentRepository contentRepository;
    private HomeworkQuestionRepository questionRepository;
    private HomeworkSubmissionRepository submissionRepository;
    private HomeworkAnswerRepository answerRepository;
    private HomeworkTargetRepository targetRepository;
    private UserRepository userRepository;
    private ExerciseGradingService service;

    private static final String EMAIL = "alumno@example.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ASSIGNMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        contentRepository = mock(ContentRepository.class);
        questionRepository = mock(HomeworkQuestionRepository.class);
        submissionRepository = mock(HomeworkSubmissionRepository.class);
        answerRepository = mock(HomeworkAnswerRepository.class);
        targetRepository = mock(HomeworkTargetRepository.class);
        userRepository = mock(UserRepository.class);
        service = new ExerciseGradingService(contentRepository, questionRepository,
                submissionRepository, answerRepository, targetRepository, userRepository);

        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setTitle("Ejercicio");
        assignment.setInstructions("Responde");
        assignment.setFormat(HomeworkFormat.EXERCISE);
        when(contentRepository.findPublishedAssignmentById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(targetRepository.isAssignedTo(ASSIGNMENT_ID, USER_ID)).thenReturn(true);
        when(submissionRepository.findByUserAndAssignment(USER_ID, ASSIGNMENT_ID)).thenReturn(Optional.empty());

        HomeworkSubmission saved = new HomeworkSubmission();
        saved.setId(UUID.randomUUID());
        when(submissionRepository.upsertGraded(any(), any(), anyInt(), any())).thenReturn(saved);
    }

    // --- helpers -------------------------------------------------------------

    private static QuestionOption option(String label, boolean correct) {
        QuestionOption o = new QuestionOption();
        o.setId(UUID.randomUUID());
        o.setLabel(label);
        o.setCorrect(correct);
        return o;
    }

    private static HomeworkQuestion question(QuestionKind kind, List<QuestionOption> options) {
        HomeworkQuestion q = new HomeworkQuestion();
        q.setId(UUID.randomUUID());
        q.setAssignmentId(ASSIGNMENT_ID);
        q.setKind(kind);
        q.setPrompt("¿…?");
        q.setOptions(options);
        return q;
    }

    private ExerciseResultResponse grade(HomeworkQuestion question, AnswerDto answer) {
        when(questionRepository.findByAssignment(ASSIGNMENT_ID)).thenReturn(List.of(question));
        return service.submit(EMAIL, ASSIGNMENT_ID,
                new SubmitExerciseRequest(List.of(answer)));
    }

    private static double scoreOf(ExerciseResultResponse r) {
        return r.questions().get(0).score();
    }

    // --- single choice -------------------------------------------------------

    @Test
    void singleChoiceCorrect() {
        QuestionOption a = option("los lápizes", false);
        QuestionOption b = option("los lápices", true);
        HomeworkQuestion q = question(QuestionKind.SINGLE_CHOICE, List.of(a, b));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(b.getId()), null));
        assertThat(scoreOf(r)).isEqualTo(1.0);
        assertThat(r.questions().get(0).correct()).isTrue();
    }

    @Test
    void singleChoiceWrong() {
        QuestionOption a = option("los lápizes", false);
        QuestionOption b = option("los lápices", true);
        HomeworkQuestion q = question(QuestionKind.SINGLE_CHOICE, List.of(a, b));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(a.getId()), null));
        assertThat(scoreOf(r)).isEqualTo(0.0);
    }

    // --- multi choice (partial credit) --------------------------------------

    @Test
    void multiChoiceAllCorrect() {
        QuestionOption o0 = option("a", true);
        QuestionOption o1 = option("b", true);
        QuestionOption o2 = option("c", false);
        QuestionOption o3 = option("d", false);
        HomeworkQuestion q = question(QuestionKind.MULTI_CHOICE, List.of(o0, o1, o2, o3));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(o0.getId(), o1.getId()), null));
        assertThat(scoreOf(r)).isEqualTo(1.0);
    }

    @Test
    void multiChoicePartialCredit() {
        QuestionOption o0 = option("a", true);
        QuestionOption o1 = option("b", true);
        QuestionOption o2 = option("c", false);
        QuestionOption o3 = option("d", false);
        HomeworkQuestion q = question(QuestionKind.MULTI_CHOICE, List.of(o0, o1, o2, o3));
        // one correct selected (o0), one incorrect selected (o2): rightDecisions = o0 + o3 = 2/4
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(o0.getId(), o2.getId()), null));
        assertThat(scoreOf(r)).isEqualTo(0.5);
        assertThat(r.questions().get(0).correct()).isFalse();
    }

    // --- fill blank ----------------------------------------------------------

    @Test
    void fillBlankCaseInsensitiveMatch() {
        HomeworkQuestion q = question(QuestionKind.FILL_BLANK, List.of(option("fui", true)));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(), "Fui"));
        assertThat(scoreOf(r)).isEqualTo(1.0);
    }

    @Test
    void fillBlankTrimsWhitespace() {
        HomeworkQuestion q = question(QuestionKind.FILL_BLANK, List.of(option("fui", true)));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(), "  fui  "));
        assertThat(scoreOf(r)).isEqualTo(1.0);
    }

    @Test
    void fillBlankAccentMismatchIsWrong() {
        HomeworkQuestion q = question(QuestionKind.FILL_BLANK, List.of(option("compré", true)));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(), "compre"));
        assertThat(scoreOf(r)).isEqualTo(0.0);
    }

    @Test
    void unansweredQuestionScoresZero() {
        HomeworkQuestion q = question(QuestionKind.FILL_BLANK, List.of(option("fui", true)));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(), null));
        assertThat(scoreOf(r)).isEqualTo(0.0);
    }

    // --- overall -------------------------------------------------------------

    @Test
    void overallPercentAndFullyCorrectCount() {
        QuestionOption a = option("mal", false);
        QuestionOption b = option("bien", true);
        HomeworkQuestion q1 = question(QuestionKind.SINGLE_CHOICE, List.of(a, b));
        HomeworkQuestion q2 = question(QuestionKind.FILL_BLANK, List.of(option("fui", true)));
        when(questionRepository.findByAssignment(ASSIGNMENT_ID)).thenReturn(List.of(q1, q2));

        // q1 correct, q2 wrong → 1 of 2 fully correct, 50%
        ExerciseResultResponse r = service.submit(EMAIL, ASSIGNMENT_ID, new SubmitExerciseRequest(List.of(
                new AnswerDto(q1.getId(), List.of(b.getId()), null),
                new AnswerDto(q2.getId(), List.of(), "no"))));

        assertThat(r.totalQuestions()).isEqualTo(2);
        assertThat(r.fullyCorrectCount()).isEqualTo(1);
        assertThat(r.scorePercent()).isEqualTo(50);
    }
}
