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
import com.fasterxml.jackson.databind.ObjectMapper;
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
                submissionRepository, answerRepository, targetRepository, userRepository, new ObjectMapper());

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
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(b.getId()), null, null));
        assertThat(scoreOf(r)).isEqualTo(1.0);
        assertThat(r.questions().get(0).correct()).isTrue();
    }

    @Test
    void singleChoiceWrong() {
        QuestionOption a = option("los lápizes", false);
        QuestionOption b = option("los lápices", true);
        HomeworkQuestion q = question(QuestionKind.SINGLE_CHOICE, List.of(a, b));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(a.getId()), null, null));
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
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(o0.getId(), o1.getId()), null, null));
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
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(o0.getId(), o2.getId()), null, null));
        assertThat(scoreOf(r)).isEqualTo(0.5);
        assertThat(r.questions().get(0).correct()).isFalse();
    }

    // --- multi blank (single gap) --------------------------------------------

    @Test
    void multiBlankCaseInsensitiveMatch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.MULTI_BLANK, "Ayer yo ___ al cine.",
                "{\"blanks\":[{\"acceptedAnswers\":[\"fui\"]}]}");
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(), null,
                mapper.readTree("{\"blanks\":[\"Fui\"]}")));
        assertThat(scoreOf(r)).isEqualTo(1.0);
    }

    @Test
    void multiBlankTrimsWhitespace() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.MULTI_BLANK, "Ayer yo ___ al cine.",
                "{\"blanks\":[{\"acceptedAnswers\":[\"fui\"]}]}");
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(), null,
                mapper.readTree("{\"blanks\":[\"  fui  \"]}")));
        assertThat(scoreOf(r)).isEqualTo(1.0);
    }

    @Test
    void multiBlankAccentMismatchIsWrong() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.MULTI_BLANK, "Yo ___ pan.",
                "{\"blanks\":[{\"acceptedAnswers\":[\"compré\"]}]}");
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(), null,
                mapper.readTree("{\"blanks\":[\"compre\"]}")));
        assertThat(scoreOf(r)).isEqualTo(0.0);
    }

    @Test
    void unansweredQuestionScoresZero() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.MULTI_BLANK, "Ayer yo ___ al cine.",
                "{\"blanks\":[{\"acceptedAnswers\":[\"fui\"]}]}");
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(), null,
                mapper.readTree("{\"blanks\":[\"\"]}")));
        assertThat(scoreOf(r)).isEqualTo(0.0);
    }

    // --- overall -------------------------------------------------------------

    @Test
    void overallPercentAndFullyCorrectCount() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        QuestionOption a = option("mal", false);
        QuestionOption b = option("bien", true);
        HomeworkQuestion q1 = question(QuestionKind.SINGLE_CHOICE, List.of(a, b));
        HomeworkQuestion q2 = structured(QuestionKind.MULTI_BLANK, "Ayer yo ___ al cine.",
                "{\"blanks\":[{\"acceptedAnswers\":[\"fui\"]}]}");
        when(questionRepository.findByAssignment(ASSIGNMENT_ID)).thenReturn(List.of(q1, q2));

        // q1 correct, q2 wrong → 1 of 2 fully correct, 50%
        ExerciseResultResponse r = service.submit(EMAIL, ASSIGNMENT_ID, new SubmitExerciseRequest(List.of(
                new AnswerDto(q1.getId(), List.of(b.getId()), null, null),
                new AnswerDto(q2.getId(), List.of(), null, mapper.readTree("{\"blanks\":[\"no\"]}")))));

        assertThat(r.totalQuestions()).isEqualTo(2);
        assertThat(r.fullyCorrectCount()).isEqualTo(1);
        assertThat(r.scorePercent()).isEqualTo(50);
    }

    // --- new structured kinds ------------------------------------------------

    @Test
    void multiBlank_partialCredit() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.MULTI_BLANK,
                "Hoy ___ al mercado y compro ___.",
                """
                {"blanks":[{"acceptedAnswers":["voy"]},{"acceptedAnswers":["fruta","Fruta"]}]}
                """);
        ExerciseResultResponse half = grade(q, new AnswerDto(q.getId(), List.of(), null,
                mapper.readTree("""
                {"blanks":["voy","manzana"]}
                """)));
        assertThat(half.questions().getFirst().score()).isEqualTo(0.5);
        assertThat(half.questions().getFirst().unitResults()).hasSize(2);
        assertThat(half.questions().getFirst().unitResults().get(0).correct()).isTrue();
        assertThat(half.questions().getFirst().unitResults().get(1).correct()).isFalse();
    }

    @Test
    void dragDrop_gradesByBankIdOrder() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";
        HomeworkQuestion q = structured(QuestionKind.DRAG_DROP,
                "El ___ y la ___.",
                """
                {"bank":[{"id":"%s","label":"perro"},{"id":"%s","label":"casa"}]}
                """.formatted(id1, id2));
        ExerciseResultResponse ok = grade(q, new AnswerDto(q.getId(), List.of(), null,
                mapper.readTree("""
                {"placements":["%s","%s"]}
                """.formatted(id1, id2))));
        assertThat(ok.scorePercent()).isEqualTo(100);

        ExerciseResultResponse swapped = grade(q, new AnswerDto(q.getId(), List.of(), null,
                mapper.readTree("""
                {"placements":["%s","%s"]}
                """.formatted(id2, id1))));
        assertThat(swapped.scorePercent()).isEqualTo(0);
    }

    @Test
    void tableFill_gradesBlankCells() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.TABLE_FILL, "Presente de hablar",
                """
                {"rowHeaders":["yo","tú"],"colHeaders":["Presente"],
                 "cells":[
                   {"r":0,"c":0,"type":"blank","acceptedAnswers":["hablo"]},
                   {"r":1,"c":0,"type":"blank","acceptedAnswers":["hablas"]}
                 ]}
                """);
        ExerciseResultResponse ok = grade(q, new AnswerDto(q.getId(), List.of(), null,
                mapper.readTree("""
                {"cells":{"0,0":"Hablo","1,0":"hablas"}}
                """)));
        assertThat(ok.scorePercent()).isEqualTo(100);
        assertThat(ok.questions().getFirst().unitResults()).hasSize(2);
    }

    @Test
    void matching_scoresExpectedPairs() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.MATCHING, "Empareja",
                """
                {"left":[{"id":"L1","label":"dog"},{"id":"L2","label":"cat"}],
                 "right":[{"id":"R1","label":"perro"},{"id":"R2","label":"gato"},{"id":"R3","label":"casa"}],
                 "pairs":[{"leftId":"L1","rightId":"R1"},{"leftId":"L2","rightId":"R2"}]}
                """);
        ExerciseResultResponse half = grade(q, new AnswerDto(q.getId(), List.of(), null,
                mapper.readTree("""
                {"pairs":[{"leftId":"L1","rightId":"R1"},{"leftId":"L2","rightId":"R3"}]}
                """)));
        assertThat(half.questions().getFirst().score()).isEqualTo(0.5);
        assertThat(half.fullyCorrectCount()).isEqualTo(0);
    }

    private static HomeworkQuestion structured(QuestionKind kind, String prompt, String structureJson) {
        HomeworkQuestion q = new HomeworkQuestion();
        q.setId(UUID.randomUUID());
        q.setAssignmentId(ASSIGNMENT_ID);
        q.setKind(kind);
        q.setPrompt(prompt);
        q.setStructureJson(structureJson);
        q.setOptions(List.of());
        return q;
    }
}
