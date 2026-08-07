package com.kuky.backend.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.exception.ActivityValidationException;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityAdminServiceValidationTest {

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
    void createRequiresPageTrigger() {
        UUID presentationId = UUID.randomUUID();
        when(activityRepository.presentationExists(presentationId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                "Title", presentationId, "MANUAL", null, null,
                null, null, "Do this", "https://youtu.be/dQw4w9WgXcQ", null, null))
                .isInstanceOf(ActivityValidationException.class);
    }

    @Test
    void createRejectsInvalidYoutubeUrl() {
        UUID presentationId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        when(activityRepository.presentationExists(presentationId)).thenReturn(true);
        when(activityRepository.fileBelongsToPresentation(presentationId, fileId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                "Title", presentationId, "MANUAL", null, null,
                fileId, 2, "Do this", "https://example.com/video", null, null))
                .isInstanceOf(ActivityValidationException.class);
    }

    @Test
    void createRequiresYoutubeOrImage() {
        UUID presentationId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        when(activityRepository.presentationExists(presentationId)).thenReturn(true);
        when(activityRepository.fileBelongsToPresentation(presentationId, fileId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                "Title", presentationId, "MANUAL", null, null,
                fileId, 2, "Do this", null, null, null))
                .isInstanceOf(ActivityValidationException.class);
    }

    @Test
    void createRejectsUnknownImage() {
        UUID presentationId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        when(activityRepository.presentationExists(presentationId)).thenReturn(true);
        when(activityRepository.fileBelongsToPresentation(presentationId, fileId)).thenReturn(true);
        when(imageRepository.findById(imageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                "Title", presentationId, "MANUAL", null, null,
                fileId, 2, "Do this", null, imageId, null))
                .isInstanceOf(ActivityValidationException.class);
    }
}
