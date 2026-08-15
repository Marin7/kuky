package com.kuky.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kuky.backend.admin.dto.CreateHomeworkRequest;
import com.kuky.backend.admin.dto.HomeworkQuestionDto;
import com.kuky.backend.admin.dto.HomeworkQuestionDto.OptionDto;
import com.kuky.backend.admin.service.HomeworkAdminService;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.repository.AudioFileRepository;
import com.kuky.backend.learning.repository.ContentRepository;
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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeworkExerciseAdminServiceTest {

    private ContentRepository contentRepository;
    private HomeworkTargetRepository targetRepository;
    private HomeworkQuestionRepository questionRepository;
    private AudioFileRepository audioFileRepository;
    private UserRepository userRepository;
    private HomeworkSubmissionRepository submissionRepository;
    private HomeworkAdminService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID ASSIGNMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        contentRepository = mock(ContentRepository.class);
        targetRepository = mock(HomeworkTargetRepository.class);
        questionRepository = mock(HomeworkQuestionRepository.class);
        audioFileRepository = mock(AudioFileRepository.class);
        userRepository = mock(UserRepository.class);
        submissionRepository = mock(HomeworkSubmissionRepository.class);
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository,
                mock(com.kuky.backend.learning.repository.HomeworkAnswerRepository.class),
                audioFileRepository, userRepository, submissionRepository, mock(ExerciseGradingService.class),
                objectMapper, mock(com.kuky.backend.notification.service.NotificationService.class));

        // For the happy path: insert returns an id and the re-fetch returns an assignment.
        when(contentRepository.insertAssignment(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ASSIGNMENT_ID);
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setTitle("t");
        assignment.setInstructions("i");
        assignment.setFormat(HomeworkFormat.EXERCISE);
        when(contentRepository.findAssignmentById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(questionRepository.findByAssignment(ASSIGNMENT_ID)).thenReturn(List.of());
        when(targetRepository.findAssigneesWithSubmissions(ASSIGNMENT_ID)).thenReturn(List.of());
    }

    private CreateHomeworkRequest exercise(List<HomeworkQuestionDto> questions) {
        return new CreateHomeworkRequest("Título", "Instrucciones", null, null, null,
                "EXERCISE", questions, null, null, null, List.of());
    }

    private static HomeworkQuestionDto q(String kind, OptionDto... options) {
        return new HomeworkQuestionDto(null, kind, "¿…?", List.of(options), null);
    }

    private HomeworkQuestionDto multiBlank(String prompt, String... accepted) throws Exception {
        ObjectNode blank = objectMapper.createObjectNode();
        blank.set("acceptedAnswers", objectMapper.valueToTree(List.of(accepted)));
        ObjectNode structure = objectMapper.createObjectNode();
        structure.set("blanks", objectMapper.createArrayNode().add(blank));
        return new HomeworkQuestionDto(null, "MULTI_BLANK", prompt, List.of(), structure);
    }

    @Test
    void exerciseWithNoQuestionsIsRejected() {
        assertThatThrownBy(() -> service.create(exercise(List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        verify(questionRepository, never()).replaceQuestions(any(), anyList());
    }

    @Test
    void singleChoiceWithoutCorrectOptionIsRejected() {
        var req = exercise(List.of(q("SINGLE_CHOICE",
                new OptionDto(null, "a", false), new OptionDto(null, "b", false))));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void singleChoiceWithTwoCorrectOptionsIsRejected() {
        var req = exercise(List.of(q("SINGLE_CHOICE",
                new OptionDto(null, "a", true), new OptionDto(null, "b", true))));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multiChoiceWithoutCorrectOptionIsRejected() {
        var req = exercise(List.of(q("MULTI_CHOICE",
                new OptionDto(null, "a", false), new OptionDto(null, "b", false))));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multiBlankWithoutBlanksIsRejected() throws Exception {
        var req = exercise(List.of(multiBlank("Sin huecos", "x")));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multiBlankWithSingleBlankIsAccepted() throws Exception {
        var req = exercise(List.of(multiBlank("Completa: Ella ___ en Madrid.", "vive")));
        assertThatNoException().isThrownBy(() -> service.create(req));
        verify(questionRepository, times(1)).replaceQuestions(any(), anyList());
    }

    @Test
    void mixedManualAndStructuredIsAcceptedAsMixed() {
        var req = new CreateHomeworkRequest("Título", "Instrucciones", null, "AUDIO", null,
                "MANUAL", List.of(
                        q("SINGLE_CHOICE", new OptionDto(null, "a", true), new OptionDto(null, "b", false)),
                        new HomeworkQuestionDto(null, "FREE_TEXT", "Resume", List.of(), null)),
                "https://example.com/a.mp3", null, "AUDIO_URL", List.of());
        assertThatNoException().isThrownBy(() -> service.create(req));
        verify(questionRepository, times(1)).replaceQuestions(any(), anyList());
    }

    @Test
    void emptyNonWriteIsRejected() {
        var req = new CreateHomeworkRequest("Título", "Instrucciones", null, "AUDIO", null,
                null, List.of(), null, null, null, List.of());
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validExercisePersistsQuestions() throws Exception {
        var req = exercise(List.of(
                q("SINGLE_CHOICE", new OptionDto(null, "a", false), new OptionDto(null, "b", true)),
                multiBlank("Completa: Ella ___ en Madrid.", "vive")));
        assertThatNoException().isThrownBy(() -> service.create(req));
        verify(questionRepository, times(1)).replaceQuestions(any(), anyList());
    }

    // --- TRUE_FALSE ----------------------------------------------------------

    private static HomeworkQuestionDto trueFalse(boolean correctIsTrue) {
        return new HomeworkQuestionDto(null, "TRUE_FALSE", "El verbo «ser» se usa para nacionalidad.",
                List.of(
                        new OptionDto(null, "true", correctIsTrue),
                        new OptionDto(null, "false", !correctIsTrue)),
                null);
    }

    @Test
    void trueFalseWithoutCorrectOptionIsRejected() {
        var req = exercise(List.of(new HomeworkQuestionDto(null, "TRUE_FALSE", "¿…?",
                List.of(new OptionDto(null, "true", false), new OptionDto(null, "false", false)), null)));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trueFalseWithWrongLabelsIsRejected() {
        var req = exercise(List.of(new HomeworkQuestionDto(null, "TRUE_FALSE", "¿…?",
                List.of(new OptionDto(null, "Verdadero", true), new OptionDto(null, "Falso", false)), null)));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trueFalseWithWrongOptionCountIsRejected() {
        var req = exercise(List.of(new HomeworkQuestionDto(null, "TRUE_FALSE", "¿…?",
                List.of(new OptionDto(null, "true", true)), null)));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trueFalseWithEmptyPromptIsRejected() {
        var req = exercise(List.of(new HomeworkQuestionDto(null, "TRUE_FALSE", "  ",
                List.of(new OptionDto(null, "true", true), new OptionDto(null, "false", false)), null)));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validTrueFalsePersists() {
        assertThatNoException().isThrownBy(() -> service.create(exercise(List.of(trueFalse(true)))));
        verify(questionRepository, times(1)).replaceQuestions(any(), anyList());
    }

    // --- MULTI_BLANK caps / DRAG_DROP multi-correct (034) --------------------

    @Test
    void multiBlankRejectsMoreThanTenAcceptedAnswers() throws Exception {
        String[] eleven = new String[11];
        for (int i = 0; i < 11; i++) eleven[i] = "a" + i;
        var req = exercise(List.of(multiBlank("Completa: Ella ___ en Madrid.", eleven)));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multiBlankDedupesNormalizedAcceptedAnswers() throws Exception {
        var req = exercise(List.of(multiBlank("Completa: Ella ___ en Madrid.", "vive", "Vive", " vive ")));
        assertThatNoException().isThrownBy(() -> service.create(req));
        verify(questionRepository, times(1)).replaceQuestions(any(), anyList());
    }

    private HomeworkQuestionDto dragDrop(String prompt, ObjectNode structure) {
        return new HomeworkQuestionDto(null, "DRAG_DROP", prompt, List.of(), structure);
    }

    @Test
    void dragDropCanonicalMultiCorrectPersists() throws Exception {
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";
        String id3 = "33333333-3333-3333-3333-333333333333";
        ObjectNode structure = objectMapper.readValue("""
                {"bank":[
                  {"id":"%s","label":"manzana"},
                  {"id":"%s","label":"pera"},
                  {"id":"%s","label":"uva"}
                ],
                "blanks":[
                  {"correctBankIds":["%s","%s"]},
                  {"correctBankIds":["%s"]}
                ]}
                """.formatted(id1, id2, id3, id1, id2, id3), ObjectNode.class);
        var req = exercise(List.of(dragDrop("Como ___ y ___.", structure)));
        assertThatNoException().isThrownBy(() -> service.create(req));
        verify(questionRepository, times(1)).replaceQuestions(any(), anyList());
    }

    @Test
    void dragDropAllowsSameBankIdOnTwoBlanks() throws Exception {
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";
        ObjectNode structure = objectMapper.readValue("""
                {"bank":[
                  {"id":"%s","label":"manzana"},
                  {"id":"%s","label":"pera"}
                ],
                "blanks":[
                  {"correctBankIds":["%s","%s"]},
                  {"correctBankIds":["%s","%s"]}
                ]}
                """.formatted(id1, id2, id1, id2, id1, id2), ObjectNode.class);
        var req = exercise(List.of(dragDrop("Como ___ y ___.", structure)));
        assertThatNoException().isThrownBy(() -> service.create(req));
        verify(questionRepository, times(1)).replaceQuestions(any(), anyList());
    }

    @Test
    void dragDropLegacyPositionalStillAccepted() throws Exception {
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";
        ObjectNode structure = objectMapper.readValue("""
                {"bank":[
                  {"id":"%s","label":"perro"},
                  {"id":"%s","label":"casa"}
                ]}
                """.formatted(id1, id2), ObjectNode.class);
        var req = exercise(List.of(dragDrop("El ___ y la ___.", structure)));
        assertThatNoException().isThrownBy(() -> service.create(req));
        verify(questionRepository, times(1)).replaceQuestions(any(), anyList());
    }

    @Test
    void dragDropAllowsBankSmallerThanBlankCount() throws Exception {
        String id1 = "11111111-1111-1111-1111-111111111111";
        ObjectNode structure = objectMapper.readValue("""
                {"bank":[
                  {"id":"%s","label":"el"}
                ],
                "blanks":[
                  {"correctBankIds":["%s"]},
                  {"correctBankIds":["%s"]},
                  {"correctBankIds":["%s"]}
                ]}
                """.formatted(id1, id1, id1, id1), ObjectNode.class);
        var req = exercise(List.of(dragDrop("___ ___ ___.", structure)));
        assertThatNoException().isThrownBy(() -> service.create(req));
        verify(questionRepository, times(1)).replaceQuestions(any(), anyList());
    }

    @Test
    void dragDropRejectsBankOverThirty() throws Exception {
        var bank = objectMapper.createArrayNode();
        var blanks = objectMapper.createArrayNode();
        for (int i = 0; i < 31; i++) {
            String id = "00000000-0000-0000-0000-%012d".formatted(i);
            ObjectNode item = objectMapper.createObjectNode();
            item.put("id", id);
            item.put("label", "w" + i);
            bank.add(item);
            if (i < 2) {
                ObjectNode blank = objectMapper.createObjectNode();
                blank.set("correctBankIds", objectMapper.createArrayNode().add(id));
                blanks.add(blank);
            }
        }
        ObjectNode structure = objectMapper.createObjectNode();
        structure.set("bank", bank);
        structure.set("blanks", blanks);
        var req = exercise(List.of(dragDrop("El ___ y la ___.", structure)));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    private HomeworkQuestionDto numberedSingleChoice(String prompt, ObjectNode structure) {
        return new HomeworkQuestionDto(null, "SINGLE_CHOICE", prompt, List.of(), structure);
    }

    private ObjectNode twoOptions(boolean firstCorrect, String a, String b) {
        ObjectNode item = objectMapper.createObjectNode();
        var opts = objectMapper.createArrayNode();
        ObjectNode o1 = objectMapper.createObjectNode();
        o1.put("id", UUID.randomUUID().toString());
        o1.put("label", a);
        o1.put("correct", firstCorrect);
        ObjectNode o2 = objectMapper.createObjectNode();
        o2.put("id", UUID.randomUUID().toString());
        o2.put("label", b);
        o2.put("correct", !firstCorrect);
        opts.add(o1);
        opts.add(o2);
        item.set("options", opts);
        return item;
    }

    @Test
    void numberedSingleChoicePersistsItemsAndEmptyOptions() {
        ObjectNode item1 = twoOptions(true, "ser", "estar");
        item1.put("number", 1);
        ObjectNode item2 = twoOptions(false, "por", "para");
        item2.put("number", 2);
        ObjectNode item3 = twoOptions(true, "muy", "mucho");
        item3.put("number", 3);
        ObjectNode structure = objectMapper.createObjectNode();
        structure.set("items", objectMapper.createArrayNode().add(item1).add(item2).add(item3));

        List<HomeworkQuestion> mapped = service.validateAndMapQuestions(false, List.of(
                numberedSingleChoice("Elige: (1) ser/estar (2) por/para (3) muy/mucho", structure)));

        assertThat(mapped).hasSize(1);
        assertThat(mapped.getFirst().getOptions()).isEmpty();
        assertThat(mapped.getFirst().getStructureJson()).contains("\"number\":1");
        assertThat(mapped.getFirst().getStructureJson()).contains("\"number\":3");
        assertThat(mapped.getFirst().getStructureJson()).doesNotContain("\"number\":4");
    }

    @Test
    void numberedSingleChoiceGapIsRejected() {
        ObjectNode item1 = twoOptions(true, "a", "b");
        item1.put("number", 1);
        ObjectNode item3 = twoOptions(true, "c", "d");
        item3.put("number", 3);
        ObjectNode structure = objectMapper.createObjectNode();
        structure.set("items", objectMapper.createArrayNode().add(item1).add(item3));

        assertThatThrownBy(() -> service.validateAndMapQuestions(false, List.of(
                numberedSingleChoice("(1) uno (3) tres", structure))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consecutivos");
    }

    @Test
    void unmarkedSingleChoiceStillRequiresOneCorrect() {
        assertThatThrownBy(() -> service.validateAndMapQuestions(false, List.of(
                q("SINGLE_CHOICE", new OptionDto(null, "a", false), new OptionDto(null, "b", false)))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void loneMarkerIsNumberedMode() {
        ObjectNode item1 = twoOptions(true, "sí", "no");
        item1.put("number", 1);
        ObjectNode structure = objectMapper.createObjectNode();
        structure.set("items", objectMapper.createArrayNode().add(item1));

        List<HomeworkQuestion> mapped = service.validateAndMapQuestions(false, List.of(
                numberedSingleChoice("Elige (1) la forma correcta", structure)));

        assertThat(mapped.getFirst().getOptions()).isEmpty();
        assertThat(mapped.getFirst().getStructureJson()).contains("\"number\":1");
    }

    @Test
    void classicIgnoresStaleItemsInStructure() throws Exception {
        ObjectNode stale = objectMapper.readValue("""
                {"items":[{"number":1,"options":[{"id":"x","label":"stale","correct":true}]}]}
                """, ObjectNode.class);
        HomeworkQuestionDto dto = new HomeworkQuestionDto(null, "SINGLE_CHOICE", "El plural de lápiz",
                List.of(new OptionDto(null, "lápizes", false), new OptionDto(null, "lápices", true)),
                stale);

        List<HomeworkQuestion> mapped = service.validateAndMapQuestions(false, List.of(dto));
        assertThat(mapped.getFirst().getOptions()).hasSize(2);
        assertThat(mapped.getFirst().getStructureJson()).isEqualTo("{}");
    }
}
