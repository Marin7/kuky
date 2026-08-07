package com.kuky.backend.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.admin.dto.ActivityAdminDetail;
import com.kuky.backend.admin.dto.ActivityAdminItem;
import com.kuky.backend.admin.dto.ExerciseSubmissionResultAdminDto;
import com.kuky.backend.admin.dto.HomeworkQuestionDto;
import com.kuky.backend.admin.dto.HomeworkReviewQueueItemDto;
import com.kuky.backend.admin.dto.HomeworkSubmissionAdminDto;
import com.kuky.backend.admin.dto.SaveActivityRequest;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.exception.ActivityNotFoundException;
import com.kuky.backend.learning.exception.ActivityReorderInvalidException;
import com.kuky.backend.learning.exception.ActivityValidationException;
import com.kuky.backend.learning.exception.AlreadyReviewedException;
import com.kuky.backend.learning.exception.NotSubmittedException;
import com.kuky.backend.learning.exception.SubmissionNotFoundException;
import com.kuky.backend.learning.model.Activity;
import com.kuky.backend.learning.model.ActivityQuestion;
import com.kuky.backend.learning.model.ActivitySubmission;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.repository.ActivityQuestionRepository;
import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.learning.repository.ActivitySubmissionRepository;
import com.kuky.backend.learning.service.ActivityExerciseGradingService;
import com.kuky.backend.learning.service.ActivityInstructionsFileStore;
import com.kuky.backend.learning.util.YoutubeUrls;
import com.kuky.backend.presentations.repository.ImageRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class ActivityAdminService {

    private final ActivityRepository activityRepository;
    private final ActivityQuestionRepository questionRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final ActivityInstructionsFileStore instructionsFileStore;
    private final PresentationRepository presentationRepository;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final HomeworkAdminService homeworkAdminService;
    private final ActivityExerciseGradingService exerciseGradingService;
    private final ObjectMapper objectMapper;

    public ActivityAdminService(ActivityRepository activityRepository,
                                ActivityQuestionRepository questionRepository,
                                ActivitySubmissionRepository submissionRepository,
                                ActivityInstructionsFileStore instructionsFileStore,
                                PresentationRepository presentationRepository,
                                ImageRepository imageRepository,
                                UserRepository userRepository,
                                HomeworkAdminService homeworkAdminService,
                                ActivityExerciseGradingService exerciseGradingService,
                                ObjectMapper objectMapper) {
        this.activityRepository = activityRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.instructionsFileStore = instructionsFileStore;
        this.presentationRepository = presentationRepository;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.homeworkAdminService = homeworkAdminService;
        this.exerciseGradingService = exerciseGradingService;
        this.objectMapper = objectMapper;
    }

    public List<ActivityAdminItem> list(UUID presentationId) {
        return activityRepository.listAll(presentationId).stream()
                .map(row -> toItem(row.activity(), row.presentationTitle(), row.hasInstructions()))
                .toList();
    }

    public ActivityAdminDetail get(UUID id) {
        return toDetail(requireActivity(id));
    }

    public ActivityAdminDetail create(SaveActivityRequest request) {
        return create(
                request.title(),
                request.presentationId(),
                request.format(),
                request.level(),
                request.homeworkType(),
                request.triggerFileId(),
                request.triggerPage(),
                request.instructionsText(),
                request.youtubeUrl(),
                request.imageId(),
                request.questions());
    }

    public ActivityAdminDetail update(UUID id, SaveActivityRequest request) {
        return update(
                id,
                request.title(),
                request.presentationId(),
                request.format(),
                request.level(),
                request.homeworkType(),
                request.triggerFileId(),
                request.triggerPage(),
                request.instructionsText(),
                request.youtubeUrl(),
                request.imageId(),
                request.questions());
    }

    public ActivityAdminDetail create(String title, UUID presentationId, String formatRaw,
                                      String level, String homeworkType,
                                      UUID triggerFileId, Integer triggerPage,
                                      String instructionsText, String youtubeUrl, UUID imageId,
                                      List<HomeworkQuestionDto> questionDtos) {
        if (title == null || title.isBlank()) {
            throw new ActivityValidationException("El título es obligatorio.");
        }
        if (presentationId == null || !activityRepository.presentationExists(presentationId)) {
            throw new ActivityValidationException("La presentación no existe.");
        }
        HomeworkFormat format = parseFormat(formatRaw);
        requirePageTrigger(presentationId, triggerFileId, triggerPage);
        String resolvedInstructions = requireInstructionsText(instructionsText);
        String resolvedYoutube = normalizeYoutubeUrl(youtubeUrl);
        UUID resolvedImageId = requireMedia(resolvedYoutube, imageId);
        List<ActivityQuestion> questions = mapQuestions(format, questionDtos);

        Activity activity = new Activity();
        activity.setPresentationId(presentationId);
        activity.setTitle(title.strip());
        activity.setFormat(format);
        activity.setLevel(blankToNull(level));
        activity.setHomeworkType(blankToNull(homeworkType));
        activity.setPosition(activityRepository.maxPosition(presentationId) + 1);
        activity.setTriggerFileId(triggerFileId);
        activity.setTriggerPage(triggerPage);
        activity.setInstructionsText(resolvedInstructions);
        activity.setYoutubeUrl(resolvedYoutube);
        activity.setImageId(resolvedImageId);
        activityRepository.insert(activity);

        questionRepository.replaceQuestions(activity.getId(), questions);
        return toDetail(requireActivity(activity.getId()));
    }

    public ActivityAdminDetail update(UUID id, String title, UUID presentationId, String formatRaw,
                                      String level, String homeworkType,
                                      UUID triggerFileId, Integer triggerPage,
                                      String instructionsText, String youtubeUrl, UUID imageId,
                                      List<HomeworkQuestionDto> questionDtos) {
        Activity existing = requireActivity(id);
        if (title == null || title.isBlank()) {
            throw new ActivityValidationException("El título es obligatorio.");
        }
        UUID targetPresentationId = presentationId != null ? presentationId : existing.getPresentationId();
        if (!activityRepository.presentationExists(targetPresentationId)) {
            throw new ActivityValidationException("La presentación no existe.");
        }
        HomeworkFormat format = parseFormat(formatRaw);
        UUID resolvedTriggerFile = triggerFileId;
        Integer resolvedTriggerPage = triggerPage;
        // Changing presentation clears invalid triggers
        if (!targetPresentationId.equals(existing.getPresentationId())) {
            if (resolvedTriggerFile != null
                    && !activityRepository.fileBelongsToPresentation(targetPresentationId, resolvedTriggerFile)) {
                resolvedTriggerFile = null;
                resolvedTriggerPage = null;
            }
            existing.setPosition(activityRepository.maxPosition(targetPresentationId) + 1);
        }
        requirePageTrigger(targetPresentationId, resolvedTriggerFile, resolvedTriggerPage);
        String resolvedInstructions = requireInstructionsText(instructionsText);
        String resolvedYoutube = normalizeYoutubeUrl(youtubeUrl);
        UUID resolvedImageId = requireMedia(resolvedYoutube, imageId);
        List<ActivityQuestion> questions = mapQuestions(format, questionDtos);

        existing.setPresentationId(targetPresentationId);
        existing.setTitle(title.strip());
        existing.setFormat(format);
        existing.setLevel(blankToNull(level));
        existing.setHomeworkType(blankToNull(homeworkType));
        existing.setTriggerFileId(resolvedTriggerFile);
        existing.setTriggerPage(resolvedTriggerPage);
        existing.setInstructionsText(resolvedInstructions);
        existing.setYoutubeUrl(resolvedYoutube);
        existing.setImageId(resolvedImageId);
        activityRepository.update(existing);
        questionRepository.replaceQuestions(id, questions);
        return toDetail(requireActivity(id));
    }

    public void delete(UUID id) {
        Activity activity = requireActivity(id);
        activityRepository.findInstructionsByActivityId(id).ifPresent(f -> {
            instructionsFileStore.deleteQuietly(f.getId());
        });
        if (activityRepository.delete(activity.getId()) == 0) {
            throw new ActivityNotFoundException("Actividad no encontrada.");
        }
    }

    public void reorder(UUID presentationId, List<UUID> activityIds) {
        if (!activityRepository.presentationExists(presentationId)) {
            throw new ActivityValidationException("La presentaciÃ³n no existe.");
        }
        List<UUID> existing = activityRepository.listByPresentationId(presentationId).stream()
                .map(Activity::getId).toList();
        if (activityIds == null
                || activityIds.size() != existing.size()
                || !new HashSet<>(existing).equals(new HashSet<>(activityIds))) {
            throw new ActivityReorderInvalidException(
                    "La lista de actividades no es una permutaciÃ³n completa de la presentaciÃ³n.");
        }
        activityRepository.reorderPositions(presentationId, activityIds);
    }

    // --- review --------------------------------------------------------------

    public List<HomeworkReviewQueueItemDto> getReviewQueue() {
        return submissionRepository.findSubmittedManualQueue().stream()
                .map(r -> new HomeworkReviewQueueItemDto(
                        r.submissionId(), r.studentId(), r.studentEmail(), r.studentFirstName(),
                        r.studentLastName(), r.studentUsername(), r.activityTitle(), r.submittedAt()))
                .toList();
    }

    public HomeworkSubmissionAdminDto getSubmissionDetail(UUID submissionId) {
        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        return toSubmissionAdminDto(submission);
    }

    public ExerciseSubmissionResultAdminDto getExerciseResult(UUID submissionId) {
        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        if (!HomeworkStatus.GRADED.name().equals(submission.getStatus())) {
            throw new NotSubmittedException("Esta entrega todavÃ­a no ha sido calificada automÃ¡ticamente.");
        }
        Activity activity = requireActivity(submission.getActivityId());
        if (activity.getFormat() != HomeworkFormat.EXERCISE) {
            throw new ActivityNotFoundException("Esta entrega no es un ejercicio auto-corregible.");
        }
        User student = userRepository.findById(submission.getUserId())
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));
        var view = exerciseGradingService.viewGradedSubmission(submission);
        return new ExerciseSubmissionResultAdminDto(
                submission.getId(),
                activity.getId(),
                activity.getTitle(),
                student.getId(),
                student.getEmail(),
                student.getFirstName(),
                student.getLastName(),
                student.getUsername(),
                view.questions(),
                view.result(),
                FormattedTextSegment.decodePlainFeedback(submission.getFeedback()));
    }

    public HomeworkSubmissionAdminDto saveFeedback(UUID submissionId, List<FormattedTextSegment> feedback) {
        FormattedTextSegment.validate(feedback);
        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        if (HomeworkStatus.REVIEWED.name().equals(submission.getStatus())) {
            throw new AlreadyReviewedException("Esta entrega ya ha sido revisada.");
        }
        if (!HomeworkStatus.SUBMITTED.name().equals(submission.getStatus())) {
            throw new NotSubmittedException("Esta entrega todavÃ­a no ha sido enviada por el alumno.");
        }
        ActivitySubmission updated = submissionRepository.saveFeedback(
                submissionId, FormattedTextSegment.toJson(feedback));
        return toSubmissionAdminDto(updated);
    }

    public ExerciseSubmissionResultAdminDto saveExerciseFeedback(UUID submissionId, String feedback) {
        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        if (!HomeworkStatus.GRADED.name().equals(submission.getStatus())) {
            throw new NotSubmittedException("Esta entrega todavÃ­a no ha sido calificada automÃ¡ticamente.");
        }
        Activity activity = requireActivity(submission.getActivityId());
        if (activity.getFormat() != HomeworkFormat.EXERCISE) {
            throw new ActivityNotFoundException("Esta entrega no es un ejercicio auto-corregible.");
        }
        String encoded = FormattedTextSegment.encodePlainFeedback(feedback);
        submissionRepository.saveExerciseFeedback(submissionId, encoded);
        return getExerciseResult(submissionId);
    }

    // --- helpers -------------------------------------------------------------

    private Activity requireActivity(UUID id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ActivityNotFoundException("Actividad no encontrada."));
    }

    private void requirePageTrigger(UUID presentationId, UUID triggerFileId, Integer triggerPage) {
        if (triggerFileId == null || triggerPage == null) {
            throw new ActivityValidationException(
                    "Debes indicar el PDF y la página tras la que se inserta la actividad.");
        }
        if (triggerPage < 1) {
            throw new ActivityValidationException("La página debe ser al menos 1.");
        }
        if (!activityRepository.fileBelongsToPresentation(presentationId, triggerFileId)) {
            throw new ActivityValidationException(
                    "El archivo del disparador no pertenece a la presentación.");
        }
    }

    private static String requireInstructionsText(String instructionsText) {
        if (instructionsText == null || instructionsText.isBlank()) {
            throw new ActivityValidationException("Las instrucciones son obligatorias.");
        }
        return instructionsText.strip();
    }

    /** Blank → null; non-blank must be a valid YouTube URL. */
    private static String normalizeYoutubeUrl(String youtubeUrl) {
        if (youtubeUrl == null || youtubeUrl.isBlank()) {
            return null;
        }
        return YoutubeUrls.extractVideoId(youtubeUrl)
                .map(id -> "https://www.youtube.com/watch?v=" + id)
                .orElseThrow(() -> new ActivityValidationException(
                        "Indica una URL de YouTube válida."));
    }

    private UUID requireMedia(String youtubeUrl, UUID imageId) {
        if ((youtubeUrl == null || youtubeUrl.isBlank()) && imageId == null) {
            throw new ActivityValidationException(
                    "Añade un vídeo de YouTube o una foto (o ambos).");
        }
        if (imageId != null && imageRepository.findById(imageId).isEmpty()) {
            throw new ActivityValidationException("La imagen no existe.");
        }
        return imageId;
    }


    private List<ActivityQuestion> mapQuestions(HomeworkFormat format, List<HomeworkQuestionDto> questionDtos) {
        List<HomeworkQuestionDto> dtos = questionDtos == null ? List.of() : questionDtos;
        List<HomeworkQuestion> mapped;
        try {
            mapped = homeworkAdminService.validateAndMapQuestions(format, dtos);
        } catch (IllegalArgumentException e) {
            throw new ActivityValidationException(e.getMessage());
        }
        return mapped.stream()
                .map(q -> ActivityQuestion.fromHomeworkQuestion(q, null))
                .toList();
    }

    private static HomeworkFormat parseFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            return HomeworkFormat.MANUAL;
        }
        try {
            return HomeworkFormat.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ActivityValidationException("Formato de actividad no válido.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private ActivityAdminItem toItem(Activity a, String presentationTitle, boolean hasInstructions) {
        return new ActivityAdminItem(
                a.getId(), a.getTitle(), a.getFormat().name(), a.getLevel(), a.getHomeworkType(),
                a.getPresentationId(), presentationTitle, a.getPosition(),
                a.getTriggerFileId(), a.getTriggerPage(),
                a.getInstructionsText(), a.getYoutubeUrl(), a.getImageId(), hasInstructions,
                a.getCreatedAt(), a.getUpdatedAt());
    }

    private ActivityAdminDetail toDetail(Activity a) {
        String presentationTitle = presentationRepository.findById(a.getPresentationId())
                .map(p -> p.getTitle())
                .orElse("");
        var instructions = activityRepository.findInstructionsByActivityId(a.getId()).orElse(null);
        List<HomeworkQuestionDto> questions = a.getFormat() == HomeworkFormat.EXERCISE
                ? questionRepository.findByActivityId(a.getId()).stream().map(this::toQuestionDto).toList()
                : List.of();
        ActivityAdminDetail.InstructionsMeta meta = instructions == null ? null
                : new ActivityAdminDetail.InstructionsMeta(
                        instructions.getId(), instructions.getOriginalName(),
                        instructions.getContentType(), instructions.getByteSize());
        return new ActivityAdminDetail(
                a.getId(), a.getTitle(), a.getFormat().name(), a.getLevel(), a.getHomeworkType(),
                a.getPresentationId(), presentationTitle, a.getPosition(),
                a.getTriggerFileId(), a.getTriggerPage(),
                a.getInstructionsText(), a.getYoutubeUrl(), a.getImageId(), instructions != null,
                a.getCreatedAt(), a.getUpdatedAt(), questions, meta);
    }

    private HomeworkQuestionDto toQuestionDto(ActivityQuestion q) {
        List<HomeworkQuestionDto.OptionDto> options = q.getOptions().stream()
                .map(o -> new HomeworkQuestionDto.OptionDto(o.getId(), o.getLabel(), o.isCorrect()))
                .toList();
        return new HomeworkQuestionDto(
                q.getId(), q.getKind().name(), q.getPrompt(), options, readStructure(q.getStructureJson()));
    }

    private JsonNode readStructure(String structureJson) {
        if (structureJson == null || structureJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(structureJson);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer la estructura de la pregunta.", e);
        }
    }

    private HomeworkSubmissionAdminDto toSubmissionAdminDto(ActivitySubmission submission) {
        User student = userRepository.findById(submission.getUserId())
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));
        Activity activity = requireActivity(submission.getActivityId());
        return new HomeworkSubmissionAdminDto(
                submission.getId(),
                student.getId(),
                student.getEmail(),
                student.getFirstName(),
                student.getLastName(),
                student.getUsername(),
                activity.getTitle(),
                submission.getStatus(),
                FormattedTextSegment.fromJson(submission.getResponseText()),
                FormattedTextSegment.fromJson(submission.getFeedback()),
                submission.getSubmittedAt(),
                submission.getReviewedAt());
    }
}
