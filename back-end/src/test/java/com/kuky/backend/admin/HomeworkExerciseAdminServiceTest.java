package com.kuky.backend.admin;

import com.kuky.backend.admin.dto.CreateHomeworkRequest;
import com.kuky.backend.admin.dto.HomeworkQuestionDto;
import com.kuky.backend.admin.dto.HomeworkQuestionDto.OptionDto;
import com.kuky.backend.admin.service.HomeworkAdminService;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
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
    private UserRepository userRepository;
    private HomeworkAdminService service;

    private static final UUID ASSIGNMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        contentRepository = mock(ContentRepository.class);
        targetRepository = mock(HomeworkTargetRepository.class);
        questionRepository = mock(HomeworkQuestionRepository.class);
        userRepository = mock(UserRepository.class);
        service = new HomeworkAdminService(contentRepository, targetRepository, questionRepository, userRepository);

        // For the happy path: insert returns an id and the re-fetch returns an assignment.
        when(contentRepository.insertAssignment(any(), any(), any(), any(), any(), any()))
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
                "EXERCISE", questions, List.of());
    }

    private static HomeworkQuestionDto q(String kind, OptionDto... options) {
        return new HomeworkQuestionDto(null, kind, "¿…?", List.of(options));
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
    void fillBlankWithoutAcceptedAnswerIsRejected() {
        var req = exercise(List.of(q("FILL_BLANK")));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void manualWithQuestionsIsRejected() {
        var req = new CreateHomeworkRequest("Título", "Instrucciones", null, null, null,
                "MANUAL", List.of(q("FILL_BLANK", new OptionDto(null, "fui", true))), List.of());
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validExercisePersistsQuestions() {
        var req = exercise(List.of(
                q("SINGLE_CHOICE", new OptionDto(null, "a", false), new OptionDto(null, "b", true)),
                q("FILL_BLANK", new OptionDto(null, "fui", true))));
        assertThatNoException().isThrownBy(() -> service.create(req));
        verify(questionRepository, times(1)).replaceQuestions(any(), anyList());
    }
}
