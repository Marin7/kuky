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
                audioFileRepository, userRepository, submissionRepository, mock(ExerciseGradingService.class),
                objectMapper);

        // For the happy path: insert returns an id and the re-fetch returns an assignment.
        when(contentRepository.insertAssignment(any(), any(), any(), any(), any(), any(), any(), any()))
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
                "EXERCISE", questions, null, null, List.of());
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
    void manualWithQuestionsIsRejected() {
        var req = new CreateHomeworkRequest("Título", "Instrucciones", null, null, null,
                "MANUAL", List.of(q("SINGLE_CHOICE", new OptionDto(null, "a", true), new OptionDto(null, "b", false))),
                null, null, List.of());
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
}
