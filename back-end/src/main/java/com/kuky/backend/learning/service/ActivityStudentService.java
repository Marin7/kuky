package com.kuky.backend.learning.service;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.dto.ActivityItemResponse;
import com.kuky.backend.learning.dto.ActivitySummary;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.SubmitExerciseRequest;
import com.kuky.backend.learning.exception.ActivityAlreadySubmittedException;
import com.kuky.backend.learning.exception.ActivityNotFoundException;
import com.kuky.backend.learning.exception.ActivityValidationException;
import com.kuky.backend.learning.model.Activity;
import com.kuky.backend.learning.model.ActivityInstructionsFile;
import com.kuky.backend.learning.model.ActivitySubmission;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.learning.repository.ActivitySubmissionRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityStudentService {

    private final ActivityRepository activityRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final PresentationRepository presentationRepository;
    private final UserRepository userRepository;
    private final ActivityInstructionsFileStore instructionsFileStore;
    private final ActivityExerciseGradingService gradingService;

    public ActivityStudentService(ActivityRepository activityRepository,
                                  ActivitySubmissionRepository submissionRepository,
                                  PresentationRepository presentationRepository,
                                  UserRepository userRepository,
                                  ActivityInstructionsFileStore instructionsFileStore,
                                  ActivityExerciseGradingService gradingService) {
        this.activityRepository = activityRepository;
        this.submissionRepository = submissionRepository;
        this.presentationRepository = presentationRepository;
        this.userRepository = userRepository;
        this.instructionsFileStore = instructionsFileStore;
        this.gradingService = gradingService;
    }

    /** Activities for the given presentations, keyed by presentation id. */
    public Map<UUID, List<ActivitySummary>> summariesForPresentations(UUID userId, List<UUID> presentationIds) {
        if (presentationIds == null || presentationIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ActivitySubmission> byActivity = submissionRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(ActivitySubmission::getActivityId, s -> s, (a, b) -> a));

        Map<UUID, List<ActivitySummary>> result = new HashMap<>();
        for (UUID presentationId : presentationIds) {
            List<ActivitySummary> list = new ArrayList<>();
            for (Activity a : activityRepository.listByPresentationId(presentationId)) {
                ActivitySubmission sub = byActivity.get(a.getId());
                String status = sub == null ? HomeworkStatus.PENDING.name() : sub.getStatus();
                Integer score = sub == null ? null : sub.getScorePercent();
                list.add(new ActivitySummary(
                        a.getId(), a.getTitle(), a.getFormat().name(), a.getPosition(),
                        status, score, a.getTriggerFileId(), a.getTriggerPage(),
                        a.getInstructionsText(), a.getYoutubeUrl(), a.getImageId()));
            }
            result.put(presentationId, list);
        }
        return result;
    }

    public ActivityItemResponse get(String email, UUID activityId) {
        User user = requireUser(email);
        Activity activity = requireAccessible(activityId, user.getId());
        Optional<ActivitySubmission> existing =
                submissionRepository.findByUserAndActivity(user.getId(), activityId);
        return toItemResponse(activity, existing.orElse(null));
    }

    @Transactional
    public ActivityItemResponse submitManual(String email, UUID activityId, List<FormattedTextSegment> response) {
        User user = requireUser(email);
        Activity activity = requireAccessible(activityId, user.getId());
        if (activity.getFormat() != HomeworkFormat.MANUAL) {
            throw new ActivityValidationException("Este ejercicio se entrega desde su propia página.");
        }
        Optional<ActivitySubmission> existing =
                submissionRepository.findByUserAndActivity(user.getId(), activityId);
        if (existing.isPresent() && HomeworkStatus.REVIEWED.name().equals(existing.get().getStatus())) {
            throw new ActivityAlreadySubmittedException(
                    "Esta actividad ya ha sido revisada y no puede modificarse.");
        }
        if (response != null) {
            FormattedTextSegment.validate(response);
        }
        ActivitySubmission saved = submissionRepository.upsertManual(
                user.getId(),
                activityId,
                HomeworkStatus.SUBMITTED.name(),
                FormattedTextSegment.toJson(response),
                Instant.now());
        return toItemResponse(activity, saved);
    }

    public ExerciseResultResponse submitExercise(String email, UUID activityId, SubmitExerciseRequest request) {
        return gradingService.submit(email, activityId, request);
    }

    public record InstructionsPdf(ActivityInstructionsFile meta, byte[] data) {}

    public InstructionsPdf getInstructions(String email, UUID activityId) {
        User user = requireUser(email);
        requireAccessible(activityId, user.getId());
        ActivityInstructionsFile meta = activityRepository.findInstructionsByActivityId(activityId)
                .orElseThrow(() -> new ActivityNotFoundException("Instrucciones no encontradas."));
        byte[] data = instructionsFileStore.read(meta.getId())
                .orElseThrow(() -> new ActivityNotFoundException("Instrucciones no encontradas."));
        return new InstructionsPdf(meta, data);
    }

    private ActivityItemResponse toItemResponse(Activity activity, ActivitySubmission submission) {
        String status = submission == null ? HomeworkStatus.PENDING.name() : submission.getStatus();

        if (activity.getFormat() == HomeworkFormat.EXERCISE) {
            ExerciseResultResponse result = null;
            String teacherFeedback = null;
            if (submission != null && HomeworkStatus.GRADED.name().equals(submission.getStatus())) {
                result = gradingService.storedResultFor(submission);
                teacherFeedback = FormattedTextSegment.decodePlainFeedback(submission.getFeedback());
            }
            return new ActivityItemResponse(
                    activity.getId(),
                    activity.getTitle(),
                    HomeworkFormat.EXERCISE.name(),
                    status,
                    activity.getLevel(),
                    activity.getHomeworkType(),
                    activity.getTriggerFileId(),
                    activity.getTriggerPage(),
                    activity.getInstructionsText(),
                    activity.getYoutubeUrl(),
                    activity.getImageId(),
                    List.of(),
                    List.of(),
                    submission == null ? null : submission.getScorePercent(),
                    gradingService.studentQuestionsFor(activity.getId()),
                    result,
                    teacherFeedback);
        }

        return new ActivityItemResponse(
                activity.getId(),
                activity.getTitle(),
                HomeworkFormat.MANUAL.name(),
                status,
                activity.getLevel(),
                activity.getHomeworkType(),
                activity.getTriggerFileId(),
                activity.getTriggerPage(),
                activity.getInstructionsText(),
                activity.getYoutubeUrl(),
                activity.getImageId(),
                submission == null ? List.of() : FormattedTextSegment.fromJson(submission.getResponseText()),
                submission == null ? List.of() : FormattedTextSegment.fromJson(submission.getFeedback()),
                null,
                List.of(),
                null,
                null);
    }

    private Activity requireAccessible(UUID activityId, UUID userId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException("Actividad no encontrada."));
        if (!presentationRepository.isSharedWith(activity.getPresentationId(), userId)) {
            throw new ActivityNotFoundException("Actividad no encontrada.");
        }
        return activity;
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }
}
