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
import com.kuky.backend.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.learning.service.ExerciseGradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        NotificationService notificationService = mock(NotificationService.class);
        service = new ExerciseGradingService(contentRepository, questionRepository,
                submissionRepository, answerRepository, targetRepository, userRepository,
                notificationService, new ObjectMapper());

        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setTitle("Ejercicio");
        assignment.setInstructions("Responde");
        assignment.setFormat(HomeworkFormat.EXERCISE);
        assignment.setPublished(true);
        assignment.setContentRevisedAt(java.time.Instant.parse("2026-08-15T12:00:00Z"));
        when(contentRepository.findPublishedAssignmentById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(contentRepository.lockAssignment(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(targetRepository.isAssignedTo(ASSIGNMENT_ID, USER_ID)).thenReturn(true);
        when(targetRepository.findDueOn(ASSIGNMENT_ID, USER_ID)).thenReturn(null);
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
                new SubmitExerciseRequest(List.of(answer), java.time.Instant.parse("2026-08-15T12:00:00Z")));
    }

    private static double scoreOf(ExerciseResultResponse r) {
        return r.questions().get(0).score();
    }

    // --- take view -----------------------------------------------------------

    @Test
    void getExercise_includesThisStudentsDueOn() {
        QuestionOption a = option("los lápices", true);
        HomeworkQuestion q = question(QuestionKind.SINGLE_CHOICE, List.of(a));
        when(questionRepository.findByAssignment(ASSIGNMENT_ID)).thenReturn(List.of(q));
        java.time.LocalDate due = java.time.LocalDate.of(2026, 8, 20);
        when(targetRepository.findDueOn(ASSIGNMENT_ID, USER_ID)).thenReturn(due);

        var response = service.getExercise(EMAIL, ASSIGNMENT_ID);

        assertThat(response.dueOn()).isEqualTo(due);
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

    // --- true / false --------------------------------------------------------

    @Test
    void trueFalseCorrect() {
        QuestionOption t = option("true", true);
        QuestionOption f = option("false", false);
        HomeworkQuestion q = question(QuestionKind.TRUE_FALSE, List.of(t, f));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(t.getId()), null));
        assertThat(scoreOf(r)).isEqualTo(1.0);
        assertThat(r.questions().get(0).correct()).isTrue();
    }

    @Test
    void trueFalseWrongRevealsCorrectOption() {
        QuestionOption t = option("true", true);
        QuestionOption f = option("false", false);
        HomeworkQuestion q = question(QuestionKind.TRUE_FALSE, List.of(t, f));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(f.getId()), null));
        assertThat(scoreOf(r)).isEqualTo(0.0);
        assertThat(r.questions().get(0).correctOptionIds()).containsExactly(t.getId());
    }

    @Test
    void trueFalseUnansweredScoresZero() {
        QuestionOption t = option("true", false);
        QuestionOption f = option("false", true);
        HomeworkQuestion q = question(QuestionKind.TRUE_FALSE, List.of(t, f));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(), null));
        assertThat(scoreOf(r)).isEqualTo(0.0);
        assertThat(r.questions().get(0).correctOptionIds()).containsExactly(f.getId());
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

    // --- multi blank (single gap) --------------------------------------------

    @Test
    void multiBlankCaseInsensitiveMatch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.MULTI_BLANK, "Ayer yo ___ al cine.",
                "{\"blanks\":[{\"acceptedAnswers\":[\"fui\"]}]}");
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("{\"blanks\":[\"Fui\"]}")));
        assertThat(scoreOf(r)).isEqualTo(1.0);
    }

    @Test
    void multiBlankTrimsWhitespace() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.MULTI_BLANK, "Ayer yo ___ al cine.",
                "{\"blanks\":[{\"acceptedAnswers\":[\"fui\"]}]}");
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("{\"blanks\":[\"  fui  \"]}")));
        assertThat(scoreOf(r)).isEqualTo(1.0);
    }

    @Test
    void multiBlankAccentMismatchIsWrong() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.MULTI_BLANK, "Yo ___ pan.",
                "{\"blanks\":[{\"acceptedAnswers\":[\"compré\"]}]}");
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("{\"blanks\":[\"compre\"]}")));
        assertThat(scoreOf(r)).isEqualTo(0.0);
    }

    @Test
    void unansweredQuestionScoresZero() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.MULTI_BLANK, "Ayer yo ___ al cine.",
                "{\"blanks\":[{\"acceptedAnswers\":[\"fui\"]}]}");
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(),
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
                new AnswerDto(q1.getId(), List.of(b.getId()), null),
                new AnswerDto(q2.getId(), List.of(), mapper.readTree("{\"blanks\":[\"no\"]}"))),
                java.time.Instant.parse("2026-08-15T12:00:00Z")));

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
        ExerciseResultResponse half = grade(q, new AnswerDto(q.getId(), List.of(),
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
        ExerciseResultResponse ok = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("""
                {"placements":["%s","%s"]}
                """.formatted(id1, id2))));
        assertThat(ok.scorePercent()).isEqualTo(100);

        ExerciseResultResponse swapped = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("""
                {"placements":["%s","%s"]}
                """.formatted(id2, id1))));
        assertThat(swapped.scorePercent()).isEqualTo(0);
    }

    @Test
    void dragDrop_anyOfCorrectBankIds() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";
        String id3 = "33333333-3333-3333-3333-333333333333";
        HomeworkQuestion q = structured(QuestionKind.DRAG_DROP,
                "Como ___ y ___.",
                """
                {"bank":[
                  {"id":"%s","label":"manzana"},
                  {"id":"%s","label":"pera"},
                  {"id":"%s","label":"uva"}
                ],
                "blanks":[
                  {"correctBankIds":["%s","%s"]},
                  {"correctBankIds":["%s"]}
                ]}
                """.formatted(id1, id2, id3, id1, id2, id3));
        ExerciseResultResponse withAlt = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("""
                {"placements":["%s","%s"]}
                """.formatted(id2, id3))));
        assertThat(withAlt.scorePercent()).isEqualTo(100);
        assertThat(withAlt.questions().getFirst().unitResults().get(0).expectedDisplay())
                .containsExactly("manzana", "pera");

        ExerciseResultResponse distractor = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("""
                {"placements":["%s","%s"]}
                """.formatted(id3, id1))));
        // blank0 wrong (uva not accepted), blank1 wrong (manzana not accepted for blank1)
        assertThat(distractor.scorePercent()).isEqualTo(0);
    }

    @Test
    void dragDrop_orderIndependentSharedAcceptedSet() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";
        // "___ and ___" — both blanks accept both words (either order OK)
        HomeworkQuestion q = structured(QuestionKind.DRAG_DROP,
                "Como ___ y ___.",
                """
                {"bank":[
                  {"id":"%s","label":"manzana"},
                  {"id":"%s","label":"pera"}
                ],
                "blanks":[
                  {"correctBankIds":["%s","%s"]},
                  {"correctBankIds":["%s","%s"]}
                ]}
                """.formatted(id1, id2, id1, id2, id1, id2));
        ExerciseResultResponse ab = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("""
                {"placements":["%s","%s"]}
                """.formatted(id1, id2))));
        assertThat(ab.scorePercent()).isEqualTo(100);

        ExerciseResultResponse ba = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("""
                {"placements":["%s","%s"]}
                """.formatted(id2, id1))));
        assertThat(ba.scorePercent()).isEqualTo(100);
    }

    @Test
    void dragDrop_studentStrip_reusableWhenSameIdOnTwoBlanks() {
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";
        HomeworkQuestion q = structured(QuestionKind.DRAG_DROP,
                "Como ___ y ___.",
                """
                {"bank":[
                  {"id":"%s","label":"el"},
                  {"id":"%s","label":"la"}
                ],
                "blanks":[
                  {"correctBankIds":["%s"]},
                  {"correctBankIds":["%s"]}
                ]}
                """.formatted(id1, id2, id1, id1));
        var student = service.studentQuestionsFor(List.of(q));
        assertThat(student).hasSize(1);
        var structure = student.getFirst().structure();
        assertThat(structure.path("bankReusable").asBoolean()).isTrue();
        assertThat(structure.path("bank").isArray()).isTrue();
        assertThat(structure.has("blanks")).isFalse();
        assertThat(structure.toString()).doesNotContain("correctBankIds");
    }

    @Test
    void dragDrop_studentStrip_exclusiveForCanonicalAndLegacy() {
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";
        HomeworkQuestion canonical = structured(QuestionKind.DRAG_DROP,
                "El ___ y la ___.",
                """
                {"bank":[
                  {"id":"%s","label":"perro"},
                  {"id":"%s","label":"casa"}
                ],
                "blanks":[
                  {"correctBankIds":["%s"]},
                  {"correctBankIds":["%s"]}
                ]}
                """.formatted(id1, id2, id1, id2));
        var canonicalStrip = service.studentQuestionsFor(List.of(canonical)).getFirst().structure();
        assertThat(canonicalStrip.path("bankReusable").asBoolean()).isFalse();
        assertThat(canonicalStrip.has("blanks")).isFalse();

        HomeworkQuestion legacy = structured(QuestionKind.DRAG_DROP,
                "El ___ y la ___.",
                """
                {"bank":[{"id":"%s","label":"perro"},{"id":"%s","label":"casa"}]}
                """.formatted(id1, id2));
        var legacyStrip = service.studentQuestionsFor(List.of(legacy)).getFirst().structure();
        assertThat(legacyStrip.path("bankReusable").asBoolean()).isFalse();
        assertThat(legacyStrip.has("blanks")).isFalse();
    }

    @Test
    void dragDrop_duplicatePlacement_scoresBothBlanksWhenSharedKey() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";
        HomeworkQuestion q = structured(QuestionKind.DRAG_DROP,
                "Como ___ y ___.",
                """
                {"bank":[
                  {"id":"%s","label":"el"},
                  {"id":"%s","label":"la"}
                ],
                "blanks":[
                  {"correctBankIds":["%s"]},
                  {"correctBankIds":["%s"]}
                ]}
                """.formatted(id1, id2, id1, id1));
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("""
                {"placements":["%s","%s"]}
                """.formatted(id1, id1))));
        assertThat(r.scorePercent()).isEqualTo(100);
        assertThat(r.questions().getFirst().unitResults()).hasSize(2);
        assertThat(r.questions().getFirst().unitResults().get(0).correct()).isTrue();
        assertThat(r.questions().getFirst().unitResults().get(1).correct()).isTrue();
        assertThat(r.questions().getFirst().unitResults().get(0).studentDisplay()).isEqualTo("el");
        assertThat(r.questions().getFirst().unitResults().get(1).studentDisplay()).isEqualTo("el");
    }

    @Test
    void multiBlank_showsAllAcceptedWhenCorrectAndMulti() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HomeworkQuestion q = structured(QuestionKind.MULTI_BLANK,
                "Hoy ___ al mercado.",
                """
                {"blanks":[{"acceptedAnswers":["voy","Voy a"]}]}
                """);
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("""
                {"blanks":["voy"]}
                """)));
        assertThat(r.scorePercent()).isEqualTo(100);
        assertThat(r.questions().getFirst().unitResults().getFirst().correct()).isTrue();
        assertThat(r.questions().getFirst().unitResults().getFirst().expectedDisplay())
                .containsExactly("voy", "Voy a");
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
        ExerciseResultResponse ok = grade(q, new AnswerDto(q.getId(), List.of(),
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
        ExerciseResultResponse half = grade(q, new AnswerDto(q.getId(), List.of(),
                mapper.readTree("""
                {"pairs":[{"leftId":"L1","rightId":"R1"},{"leftId":"L2","rightId":"R3"}]}
                """)));
        assertThat(half.questions().getFirst().score()).isEqualTo(0.5);
        assertThat(half.fullyCorrectCount()).isEqualTo(0);
        assertThat(half.questions().getFirst().unitResults())
                .extracting(u -> u.label() + "|" + u.studentDisplay() + "|" + u.correct())
                .containsExactly("dog|perro|true", "cat|casa|false");
        assertThat(half.questions().getFirst().unitResults().get(1).expectedDisplay())
                .containsExactly("gato");
    }

    // --- numbered SINGLE_CHOICE ----------------------------------------------

    private static HomeworkQuestion numberedSingleChoice(
            String prompt, String structureJson) {
        return structured(QuestionKind.SINGLE_CHOICE, prompt, structureJson);
    }

    private static String threeItemStructure(String id1a, String id1b,
                                             String id2a, String id2b,
                                             String id3a, String id3b) {
        return """
                {"items":[
                  {"number":1,"options":[
                    {"id":"%s","label":"ser","correct":true},
                    {"id":"%s","label":"estar","correct":false}]},
                  {"number":2,"options":[
                    {"id":"%s","label":"por","correct":false},
                    {"id":"%s","label":"para","correct":true}]},
                  {"number":3,"options":[
                    {"id":"%s","label":"muy","correct":true},
                    {"id":"%s","label":"mucho","correct":false}]}
                ]}
                """.formatted(id1a, id1b, id2a, id2b, id3a, id3b);
    }

    @Test
    void numberedSingleChoiceTwoOfThreeIsSixtySeven() throws Exception {
        String id1a = UUID.randomUUID().toString();
        String id1b = UUID.randomUUID().toString();
        String id2a = UUID.randomUUID().toString();
        String id2b = UUID.randomUUID().toString();
        String id3a = UUID.randomUUID().toString();
        String id3b = UUID.randomUUID().toString();
        HomeworkQuestion q = numberedSingleChoice(
                "Elige: (1) ser/estar (2) por/para (3) muy/mucho",
                threeItemStructure(id1a, id1b, id2a, id2b, id3a, id3b));
        ObjectMapper mapper = new ObjectMapper();
        // items 1 and 2 correct, item 3 wrong
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(), mapper.readTree("""
                {"selections":{"1":"%s","2":"%s","3":"%s"}}
                """.formatted(id1a, id2b, id3b))));

        assertThat(r.scorePercent()).isEqualTo(67);
        assertThat(r.fullyCorrectCount()).isEqualTo(2);
        assertThat(r.totalQuestions()).isEqualTo(3);
        assertThat(r.questions()).hasSize(1);
        assertThat(r.questions().getFirst().score()).isCloseTo(2.0 / 3.0, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(r.questions().getFirst().correct()).isFalse();
        assertThat(r.questions().getFirst().correctOptionIds()).isEmpty();
        assertThat(r.questions().getFirst().unitResults()).hasSize(3);
        assertThat(r.questions().getFirst().unitResults().get(2).correct()).isFalse();
        assertThat(r.questions().getFirst().unitResults().get(2).expectedDisplay()).contains("muy");
    }

    @Test
    void numberedSingleChoiceAllCorrectIsThreeOfThree() throws Exception {
        String id1a = UUID.randomUUID().toString();
        String id1b = UUID.randomUUID().toString();
        String id2a = UUID.randomUUID().toString();
        String id2b = UUID.randomUUID().toString();
        String id3a = UUID.randomUUID().toString();
        String id3b = UUID.randomUUID().toString();
        HomeworkQuestion q = numberedSingleChoice(
                "Elige: (1) ser/estar (2) por/para (3) muy/mucho",
                threeItemStructure(id1a, id1b, id2a, id2b, id3a, id3b));
        ObjectMapper mapper = new ObjectMapper();
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(), mapper.readTree("""
                {"selections":{"1":"%s","2":"%s","3":"%s"}}
                """.formatted(id1a, id2b, id3a))));

        assertThat(r.scorePercent()).isEqualTo(100);
        assertThat(r.fullyCorrectCount()).isEqualTo(3);
        assertThat(r.totalQuestions()).isEqualTo(3);
        assertThat(r.questions().getFirst().correct()).isTrue();
    }

    @Test
    void numberedSingleChoiceIncompleteSubmitIsRejected() throws Exception {
        String id1a = UUID.randomUUID().toString();
        String id1b = UUID.randomUUID().toString();
        String id2a = UUID.randomUUID().toString();
        String id2b = UUID.randomUUID().toString();
        String id3a = UUID.randomUUID().toString();
        String id3b = UUID.randomUUID().toString();
        HomeworkQuestion q = numberedSingleChoice(
                "Elige: (1) a (2) b (3) c",
                threeItemStructure(id1a, id1b, id2a, id2b, id3a, id3b));
        ObjectMapper mapper = new ObjectMapper();
        when(questionRepository.findByAssignment(ASSIGNMENT_ID)).thenReturn(List.of(q));

        assertThatThrownBy(() ->
                service.submit(EMAIL, ASSIGNMENT_ID, new SubmitExerciseRequest(List.of(
                        new AnswerDto(q.getId(), List.of(), mapper.readTree("""
                                {"selections":{"1":"%s","2":"%s"}}
                                """.formatted(id1a, id2b)))),
                        java.time.Instant.parse("2026-08-15T12:00:00Z"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("todas las preguntas");
    }

    @Test
    void numberedStudentGetStripsCorrectFlags() {
        String id1a = UUID.randomUUID().toString();
        String id1b = UUID.randomUUID().toString();
        HomeworkQuestion q = numberedSingleChoice("Solo (1) esto",
                """
                {"items":[{"number":1,"options":[
                  {"id":"%s","label":"sí","correct":true},
                  {"id":"%s","label":"no","correct":false}]}]}
                """.formatted(id1a, id1b));
        var student = service.studentQuestionsFor(List.of(q));
        assertThat(student).hasSize(1);
        assertThat(student.getFirst().options()).isEmpty();
        assertThat(student.getFirst().structure().path("items").isArray()).isTrue();
        assertThat(student.getFirst().structure().toString()).doesNotContain("correct");
        assertThat(student.getFirst().structure().path("items").get(0).path("options").get(0).path("label").asText())
                .isEqualTo("sí");
    }

    @Test
    void classicSingleChoiceStillOneContribution() {
        QuestionOption a = option("mal", false);
        QuestionOption b = option("bien", true);
        HomeworkQuestion q = question(QuestionKind.SINGLE_CHOICE, List.of(a, b));
        q.setPrompt("El plural de lápiz");
        ExerciseResultResponse r = grade(q, new AnswerDto(q.getId(), List.of(b.getId()), null));
        assertThat(r.totalQuestions()).isEqualTo(1);
        assertThat(r.fullyCorrectCount()).isEqualTo(1);
        assertThat(r.scorePercent()).isEqualTo(100);
        assertThat(r.questions().getFirst().unitResults()).isEmpty();
    }

    @Test
    void numberedPlusClassicIsFourContributions() throws Exception {
        String id1a = UUID.randomUUID().toString();
        String id1b = UUID.randomUUID().toString();
        String id2a = UUID.randomUUID().toString();
        String id2b = UUID.randomUUID().toString();
        String id3a = UUID.randomUUID().toString();
        String id3b = UUID.randomUUID().toString();
        HomeworkQuestion numbered = numberedSingleChoice(
                "Elige: (1) a (2) b (3) c",
                threeItemStructure(id1a, id1b, id2a, id2b, id3a, id3b));
        QuestionOption wrong = option("mal", false);
        QuestionOption right = option("bien", true);
        HomeworkQuestion classic = question(QuestionKind.SINGLE_CHOICE, List.of(wrong, right));
        classic.setPrompt("Sin números");
        when(questionRepository.findByAssignment(ASSIGNMENT_ID)).thenReturn(List.of(numbered, classic));

        ObjectMapper mapper = new ObjectMapper();
        ExerciseResultResponse r = service.submit(EMAIL, ASSIGNMENT_ID, new SubmitExerciseRequest(List.of(
                new AnswerDto(numbered.getId(), List.of(), mapper.readTree("""
                        {"selections":{"1":"%s","2":"%s","3":"%s"}}
                        """.formatted(id1a, id2b, id3a))),
                new AnswerDto(classic.getId(), List.of(right.getId()), null)),
                java.time.Instant.parse("2026-08-15T12:00:00Z")));

        assertThat(r.totalQuestions()).isEqualTo(4);
        assertThat(r.fullyCorrectCount()).isEqualTo(4);
        assertThat(r.scorePercent()).isEqualTo(100);
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
