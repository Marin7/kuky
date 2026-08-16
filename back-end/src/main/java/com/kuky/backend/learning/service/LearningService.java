package com.kuky.backend.learning.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.learning.dto.ActivitySummary;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.LearningResponse;
import com.kuky.backend.learning.dto.PastClassResponse;
import com.kuky.backend.learning.dto.PresentationBlockResponse;
import com.kuky.backend.learning.dto.SharedPresentationSummary;
import com.kuky.backend.learning.dto.UnitRef;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.notification.dto.UnitSeenResponse;
import com.kuky.backend.notification.service.NotificationService;
import com.kuky.backend.presentations.exception.PresentationNotFoundException;
import com.kuky.backend.presentations.model.PresentationFile;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.presentations.service.PresentationFileStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
public class LearningService {

    private final ContentRepository contentRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkQuestionRepository questionRepository;
    private final HomeworkAnswerRepository answerRepository;
    private final HomeworkTargetRepository targetRepository;
    private final UserRepository userRepository;
    private final PresentationRepository presentationRepository;
    private final PresentationFileStore presentationFileStore;
    private final ActivityStudentService activityStudentService;
    private final AssignmentSnapshot assignmentSnapshot;
    private final SchedulingProperties props;
    private final NotificationService notificationService;

    public LearningService(ContentRepository contentRepository,
                           HomeworkSubmissionRepository submissionRepository,
                           HomeworkQuestionRepository questionRepository,
                           HomeworkAnswerRepository answerRepository,
                           HomeworkTargetRepository targetRepository,
                           UserRepository userRepository,
                           PresentationRepository presentationRepository,
                           PresentationFileStore presentationFileStore,
                           ActivityStudentService activityStudentService,
                           AssignmentSnapshot assignmentSnapshot,
                           SchedulingProperties props,
                           NotificationService notificationService) {
        this.contentRepository = contentRepository;
        this.submissionRepository = submissionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.targetRepository = targetRepository;
        this.userRepository = userRepository;
        this.presentationRepository = presentationRepository;
        this.presentationFileStore = presentationFileStore;
        this.activityStudentService = activityStudentService;
        this.assignmentSnapshot = assignmentSnapshot;
        this.props = props;
        this.notificationService = notificationService;
    }

    public LearningResponse getOverview(String userEmail) {
        User user = requireUser(userEmail);
        Set<UUID> unseenUnits = notificationService.unseenUnitIds(user.getId());
        Set<UUID> unseenHomework = notificationService.unseenHomeworkIds(user.getId());

        List<PresentationBlockResponse> presentation = contentRepository.findPublishedPresentation().stream()
                .map(p -> new PresentationBlockResponse(p.getHeading(), p.getBody()))
                .toList();

        LocalDate enrolledOn = user.getCreatedAt().atZone(teacherZone()).toLocalDate();
        List<PastClassResponse> pastClasses = contentRepository.findPublishedPastClassesSince(enrolledOn).stream()
                .map(c -> new PastClassResponse(c.getId(), c.getTitle(), c.getHeldOn(), c.getTeacherNote()))
                .toList();

        Map<UUID, HomeworkSubmission> submissionsByAssignment = submissionRepository.findByUserId(user.getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        HomeworkSubmission::getAssignmentId, Function.identity()));

        LocalDate today = LocalDate.now(teacherZone());

        Map<UUID, ContentRepository.AssignmentUnit> assignmentUnits = contentRepository
                .findAssignmentUnitsForUser(user.getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        ContentRepository.AssignmentUnit::assignmentId,
                        au -> au));

        Map<UUID, LocalDate> dueOns = targetRepository.findDueOnsForUser(user.getId());

        List<HomeworkItemResponse> homework = contentRepository.findAssignmentsForUser(user.getId()).stream()
                .map(a -> {
                    ContentRepository.AssignmentUnit au = assignmentUnits.get(a.getId());
                    UnitRef unit = au == null ? null
                            : new UnitRef(au.unitId(), au.level(), au.subject(), au.position(),
                                    unseenUnits.contains(au.unitId()));
                    Integer unitPosition = au == null ? null : au.unitPosition();
                    HomeworkSubmission submission = submissionsByAssignment.get(a.getId());
                    List<HomeworkQuestion> questions;
                    if (assignmentSnapshot.present(submission)) {
                        questions = assignmentSnapshot.questionsOf(submission);
                    } else {
                        questions = questionRepository.findByAssignment(a.getId());
                    }
                    List<HomeworkAnswer> answers = submission == null
                            ? List.of()
                            : answerRepository.findBySubmission(submission.getId());
                    return HomeworkItems.toResponse(a, submission, today, dueOns.get(a.getId()), unit, unitPosition,
                            questions, answers, null, null, unseenHomework.contains(a.getId()));
                })
                .toList();

        var sharedRows = presentationRepository.findSharedSummariesForUser(user.getId());
        List<UUID> presentationIds = sharedRows.stream().map(s -> s.id()).toList();
        Map<UUID, List<com.kuky.backend.admin.dto.PresentationFileSummary>> filesByPresentation =
                presentationRepository.listFilesGrouped(presentationIds);
        Map<UUID, List<ActivitySummary>> activitiesByPresentation =
                activityStudentService.summariesForPresentations(user.getId(), presentationIds);
        List<SharedPresentationSummary> sharedPresentations = sharedRows.stream()
                .map(s -> new SharedPresentationSummary(
                        s.id(),
                        s.title(),
                        filesByPresentation.getOrDefault(s.id(), List.of()),
                        s.unit() == null ? null
                                : new UnitRef(
                                        s.unit().id(), s.unit().level(), s.unit().subject(),
                                        s.unit().position(), unseenUnits.contains(s.unit().id())),
                        s.contentUnitPosition(),
                        activitiesByPresentation.getOrDefault(s.id(), List.of())))
                .toList();

        return new LearningResponse(presentation, pastClasses, homework, sharedPresentations);
    }

    public UnitSeenResponse markUnitSeen(String userEmail, UUID unitId) {
        User user = requireUser(userEmail);
        if (!notificationService.markUnitSeen(unitId, user.getId())) {
            throw new com.kuky.backend.units.exception.UnitNotFoundException("Unidad no encontrada.");
        }
        return new UnitSeenResponse(false);
    }

    public UnitSeenResponse markHomeworkSeen(String userEmail, UUID assignmentId) {
        User user = requireUser(userEmail);
        if (!notificationService.markHomeworkSeen(assignmentId, user.getId())) {
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
        return new UnitSeenResponse(false);
    }

    /** Downloads one file for a shared presentation (PPTX or PDF). Enforces share-gate. */
    public PresentationFile getPresentationFile(String userEmail, UUID presentationId, UUID fileId) {
        User user = requireUser(userEmail);
        if (!presentationRepository.isSharedWith(presentationId, user.getId())) {
            throw new PresentationNotFoundException("Presentación no encontrada.");
        }
        PresentationFile meta = presentationRepository.findFile(presentationId, fileId)
                .orElseThrow(() -> new PresentationNotFoundException("Archivo no encontrado."));
        byte[] data = presentationFileStore.read(fileId)
                .orElseThrow(() -> new PresentationNotFoundException("Archivo no encontrado."));
        return new PresentationFile(
                meta.id(), meta.presentationId(), meta.originalName(), meta.displayName(),
                meta.contentType(), meta.byteSize(), meta.createdAt(), data);
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }

    private ZoneId teacherZone() {
        return ZoneId.of(props.getScheduling().getTeacherTimezone());
    }
}
