package com.kuky.backend.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.exception.ActivityReorderInvalidException;
import com.kuky.backend.learning.exception.ActivityValidationException;
import com.kuky.backend.learning.model.Activity;
import com.kuky.backend.learning.repository.ActivityQuestionRepository;
import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.learning.repository.ActivitySubmissionRepository;
import com.kuky.backend.learning.service.ActivityExerciseGradingService;
import com.kuky.backend.learning.service.ActivityInstructionsFileStore;
import com.kuky.backend.presentations.repository.ImageRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityAdminServiceReorderTest {

    @Mock ActivityRepository activityRepository;
    @Mock ActivityQuestionRepository questionRepository;
    @Mock ActivitySubmissionRepository submissionRepository;
    @Mock ActivityInstructionsFileStore instructionsFileStore;
    @Mock PresentationRepository presentationRepository;
    @Mock ImageRepository imageRepository;
    @Mock UserRepository userRepository;
    @Mock HomeworkAdminService homeworkAdminService;
    @Mock ActivityExerciseGradingService exerciseGradingService;
    @Mock ObjectMapper objectMapper;

    ActivityAdminService service;

    @BeforeEach
    void setUp() {
        service = new ActivityAdminService(
                activityRepository,
                questionRepository,
                submissionRepository,
                instructionsFileStore,
                presentationRepository,
                imageRepository,
                userRepository,
                homeworkAdminService,
                exerciseGradingService,
                objectMapper);
    }

    @Test
    void reorderRejectsIncompletePermutation() {
        UUID presentationId = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Activity actA = new Activity();
        actA.setId(a);
        Activity actB = new Activity();
        actB.setId(b);

        when(activityRepository.presentationExists(presentationId)).thenReturn(true);
        when(activityRepository.listByPresentationId(presentationId)).thenReturn(List.of(actA, actB));

        assertThatThrownBy(() -> service.reorder(presentationId, List.of(a)))
                .isInstanceOf(ActivityReorderInvalidException.class);
        verify(activityRepository, never()).reorderPositions(any(), any());
    }

    @Test
    void reorderRejectsUnknownPresentation() {
        UUID presentationId = UUID.randomUUID();
        when(activityRepository.presentationExists(presentationId)).thenReturn(false);

        assertThatThrownBy(() -> service.reorder(presentationId, List.of()))
                .isInstanceOf(ActivityValidationException.class);
        verify(activityRepository, never()).reorderPositions(any(), any());
    }

    @Test
    void reorderAcceptsFullPermutation() {
        UUID presentationId = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Activity actA = new Activity();
        actA.setId(a);
        Activity actB = new Activity();
        actB.setId(b);

        when(activityRepository.presentationExists(presentationId)).thenReturn(true);
        when(activityRepository.listByPresentationId(presentationId)).thenReturn(List.of(actA, actB));

        service.reorder(presentationId, List.of(b, a));
        verify(activityRepository).reorderPositions(eq(presentationId), eq(List.of(b, a)));
    }
}
