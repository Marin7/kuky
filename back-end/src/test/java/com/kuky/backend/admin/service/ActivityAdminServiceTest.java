package com.kuky.backend.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.admin.dto.HomeworkQuestionDto;
import com.kuky.backend.admin.dto.SaveActivityRequest;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.exception.ActivityHasSubmissionsException;
import com.kuky.backend.learning.exception.ActivityPageTakenException;
import com.kuky.backend.learning.exception.ActivityValidationException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.learning.model.Activity;
import com.kuky.backend.learning.model.ActivitySubmission;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.repository.ActivityAnswerRepository;
import com.kuky.backend.learning.repository.ActivityQuestionRepository;
import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.learning.repository.ActivitySubmissionRepository;
import com.kuky.backend.learning.service.ActivityExerciseGradingService;
import com.kuky.backend.notification.service.NotificationService;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.units.exception.UnitNotFoundException;
import com.kuky.backend.units.model.Unit;
import com.kuky.backend.units.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Page activities: marking a unit PDF page, editing it, and unmarking it. */
@ExtendWith(MockitoExtension.class)
class ActivityAdminServiceTest {

    @Mock ActivityRepository activityRepository;
    @Mock ActivityQuestionRepository questionRepository;
    @Mock ActivitySubmissionRepository submissionRepository;
    @Mock ActivityAnswerRepository answerRepository;
    @Mock PresentationRepository presentationRepository;
    @Mock UnitRepository unitRepository;
    @Mock UserRepository userRepository;
    @Mock HomeworkAdminService homeworkAdminService;
    @Mock ActivityExerciseGradingService exerciseGradingService;
    @Mock NotificationService notificationService;

    ActivityAdminService service;

    private final UUID unitId = UUID.randomUUID();
    private final UUID presentationId = UUID.randomUUID();
    private final List<HomeworkQuestionDto> oneQuestion =
            List.of(new HomeworkQuestionDto(null, "FREE_TEXT", "¿Qué ves?", List.of(), null));

    @BeforeEach
    void setUp() {
        service = new ActivityAdminService(
                activityRepository, questionRepository, submissionRepository, answerRepository,
                presentationRepository, unitRepository, userRepository, homeworkAdminService,
                exerciseGradingService, notificationService, new ObjectMapper());
        lenient().when(unitRepository.findById(unitId)).thenReturn(Optional.of(new Unit()));
        lenient().when(presentationRepository.findOwnedByUnit(unitId)).thenReturn(Optional.of(
                new PresentationRepository.OwnedPdf(presentationId, UUID.randomUUID(), "u.pdf", 10, 10, 0)));
        HomeworkQuestion mapped = new HomeworkQuestion();
        mapped.setKind(QuestionKind.FREE_TEXT);
        mapped.setPrompt("¿Qué ves?");
        lenient().when(homeworkAdminService.validateAndMapQuestions(anyBoolean(), anyList()))
                .thenReturn(List.of(mapped));
        lenient().when(activityRepository.insert(any())).thenAnswer(inv -> {
            Activity a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            lenient().when(activityRepository.findById(a.getId())).thenReturn(Optional.of(a));
            return a;
        });
        lenient().when(activityRepository.findByPresentationAndPage(any(), any(Integer.class)))
                .thenReturn(Optional.empty());
    }

    @Test
    void createOnPageStoresThePageAndNullTitleWhenBlank() {
        var detail = service.createOnPage(unitId, 4, "   ", oneQuestion);

        ArgumentCaptor<Activity> saved = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).insert(saved.capture());
        assertThat(saved.getValue().getPage()).isEqualTo(4);
        assertThat(saved.getValue().getPresentationId()).isEqualTo(presentationId);
        assertThat(saved.getValue().getTitle()).isNull();
        assertThat(saved.getValue().displayTitle()).isEqualTo("Actividad – página 4");
        assertThat(detail.page()).isEqualTo(4);
    }

    @Test
    void createOnPageRejectsUnknownUnit() {
        UUID other = UUID.randomUUID();
        when(unitRepository.findById(other)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOnPage(other, 1, null, oneQuestion))
                .isInstanceOf(UnitNotFoundException.class);
    }

    @Test
    void createOnPageRequiresTheUnitToHaveAPdf() {
        when(presentationRepository.findOwnedByUnit(unitId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOnPage(unitId, 1, null, oneQuestion))
                .isInstanceOf(ActivityValidationException.class);
        verify(activityRepository, never()).insert(any());
    }

    @Test
    void createOnPageRejectsPagesOutsideThePdf() {
        assertThatThrownBy(() -> service.createOnPage(unitId, 0, null, oneQuestion))
                .isInstanceOf(ActivityValidationException.class);
        assertThatThrownBy(() -> service.createOnPage(unitId, 11, null, oneQuestion))
                .isInstanceOf(ActivityValidationException.class);
        verify(activityRepository, never()).insert(any());
    }

    @Test
    void createOnPageRejectsAnActivityWithoutQuestions() {
        assertThatThrownBy(() -> service.createOnPage(unitId, 2, null, List.of()))
                .isInstanceOf(ActivityValidationException.class);
        verify(activityRepository, never()).insert(any());
    }

    @Test
    void createOnPageRejectsAPageThatIsAlreadyAnActivity() {
        when(activityRepository.findByPresentationAndPage(presentationId, 3))
                .thenReturn(Optional.of(new Activity()));

        assertThatThrownBy(() -> service.createOnPage(unitId, 3, null, oneQuestion))
                .isInstanceOf(ActivityPageTakenException.class);
        verify(activityRepository, never()).insert(any());
    }

    @Test
    void updateChangesTitleAndQuestionsButNeverThePage() {
        Activity existing = new Activity();
        existing.setId(UUID.randomUUID());
        existing.setPresentationId(presentationId);
        existing.setPage(5);
        when(activityRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        service.update(existing.getId(), new SaveActivityRequest(" Mira la foto ", oneQuestion));

        verify(activityRepository).update(existing);
        assertThat(existing.getTitle()).isEqualTo("Mira la foto");
        assertThat(existing.getPage()).isEqualTo(5);
        verify(questionRepository).replaceQuestions(any(), anyList());
    }

    @Test
    void deleteWithSubmissionsNeedsExplicitConfirmation() {
        UUID id = UUID.randomUUID();
        when(activityRepository.findById(id)).thenReturn(Optional.of(new Activity()));
        when(activityRepository.countSubmissions(id)).thenReturn(2);

        assertThatThrownBy(() -> service.delete(id, false))
                .isInstanceOf(ActivityHasSubmissionsException.class);
        verify(activityRepository, never()).delete(id);

        when(activityRepository.delete(id)).thenReturn(1);
        service.delete(id, true);
        verify(activityRepository).delete(id);
    }

    @Test
    void openingASubmissionMarksItSeen() {
        Activity activity = new Activity();
        activity.setId(UUID.randomUUID());
        activity.setPage(2);
        User student = new User();
        student.setId(UUID.randomUUID());
        ActivitySubmission submission = new ActivitySubmission();
        submission.setId(UUID.randomUUID());
        submission.setActivityId(activity.getId());
        submission.setUserId(student.getId());
        submission.setStatus("SUBMITTED");
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(answerRepository.findBySubmission(submission.getId())).thenReturn(List.of());

        var dto = service.getSubmissionDetail(submission.getId());

        verify(notificationService).markActivitySeen(submission.getId());
        assertThat(dto.assignmentTitle()).isEqualTo("Actividad – página 2");
    }
}
