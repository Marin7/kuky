package com.kuky.backend.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kuky.backend.admin.dto.AssigneeDto;
import com.kuky.backend.admin.dto.CreateHomeworkRequest;
import com.kuky.backend.admin.dto.ExerciseSubmissionResultAdminDto;
import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.HomeworkQuestionDto;
import com.kuky.backend.admin.dto.HomeworkReviewQueueItemDto;
import com.kuky.backend.admin.dto.HomeworkSubmissionAdminDto;
import com.kuky.backend.admin.dto.SaveHomeworkFeedbackRequest;
import com.kuky.backend.admin.dto.UpdateHomeworkRequest;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.ExerciseStructureLimits;
import com.kuky.backend.learning.exception.AlreadyReviewedException;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.NotSubmittedException;
import com.kuky.backend.learning.exception.SubmissionNotFoundException;
import com.kuky.backend.learning.dto.ManualAnswerViewDto;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkComposition;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkLevel;
import com.kuky.backend.learning.model.ListeningMedia;
import com.kuky.backend.learning.model.MediaSourceKind;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.learning.repository.AudioFileRepository;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkAnswerRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.learning.service.AssignmentSnapshot;
import com.kuky.backend.learning.service.BlankPassageParser;
import com.kuky.backend.learning.service.ExerciseGradingService;
import com.kuky.backend.learning.service.HomeworkCompositionSupport;
import com.kuky.backend.learning.service.SingleChoiceMarkerParser;
import com.kuky.backend.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/** Teacher-side homework authoring + assignment + submission review. */
@Service
@Transactional
public class HomeworkAdminService {

    private final ContentRepository contentRepository;
    private final HomeworkTargetRepository targetRepository;
    private final HomeworkQuestionRepository questionRepository;
    private final HomeworkAnswerRepository answerRepository;
    private final AudioFileRepository audioFileRepository;
    private final UserRepository userRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final ExerciseGradingService exerciseGradingService;
    private final ObjectMapper objectMapper;
    private final AssignmentSnapshot assignmentSnapshot;
    private final NotificationService notificationService;

    public HomeworkAdminService(ContentRepository contentRepository,
                                HomeworkTargetRepository targetRepository,
                                HomeworkQuestionRepository questionRepository,
                                HomeworkAnswerRepository answerRepository,
                                AudioFileRepository audioFileRepository,
                                UserRepository userRepository,
                                HomeworkSubmissionRepository submissionRepository,
                                ExerciseGradingService exerciseGradingService,
                                ObjectMapper objectMapper,
                                NotificationService notificationService) {
        this.contentRepository = contentRepository;
        this.targetRepository = targetRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.audioFileRepository = audioFileRepository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.exerciseGradingService = exerciseGradingService;
        this.objectMapper = objectMapper;
        this.assignmentSnapshot = new AssignmentSnapshot(objectMapper);
        this.notificationService = notificationService;
    }

    // --- Teacher review of MANUAL submissions --------------------------------

    public List<HomeworkReviewQueueItemDto> getReviewQueue() {
        return submissionRepository.findSubmittedManualQueue().stream()
                .map(r -> new HomeworkReviewQueueItemDto(
                        r.submissionId(), r.studentId(), r.studentEmail(), r.studentFirstName(),
                        r.studentLastName(), r.studentUsername(), r.assignmentTitle(), r.submittedAt(), r.unseen()))
                .toList();
    }

    public HomeworkSubmissionAdminDto getSubmissionDetail(UUID submissionId) {
        notificationService.markHomeworkSeen(submissionId);
        var row = submissionRepository.findDetailById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        return toSubmissionAdminDto(row);
    }

    /**
     * Graded exercise detail for the teacher: questions + the student's answers
     * and automatic score breakdown. Only valid for {@code GRADED} exercise submissions.
     */
    public ExerciseSubmissionResultAdminDto getExerciseResult(UUID submissionId) {
        notificationService.markHomeworkSeen(submissionId);
        HomeworkSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        if (!HomeworkStatus.GRADED.name().equals(submission.getStatus())) {
            throw new NotSubmittedException("Esta entrega todavía no ha sido calificada automáticamente.");
        }
        HomeworkAssignment assignment = requireAssignment(submission.getAssignmentId());
        if (assignment.getFormat() != HomeworkFormat.EXERCISE
                && assignment.getFormat() != HomeworkFormat.MIXED) {
            throw new AssignmentNotFoundException("Esta entrega no es un ejercicio auto-corregible.");
        }
        User student = userRepository.findById(submission.getUserId())
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));
        if (assignmentSnapshot.present(submission)) {
            assignmentSnapshot.applyContent(assignment, submission);
        }
        ExerciseGradingService.GradedExerciseView view =
                exerciseGradingService.viewGradedSubmission(submission);
        return new ExerciseSubmissionResultAdminDto(
                submission.getId(),
                assignment.getId(),
                assignment.getTitle(),
                student.getId(),
                student.getEmail(),
                student.getFirstName(),
                student.getLastName(),
                student.getUsername(),
                view.questions(),
                view.result(),
                FormattedTextSegment.decodePlainFeedback(submission.getFeedback()));
    }

    /**
     * Saves, updates, or clears plain-text teacher feedback on a GRADED exercise
     * submission without changing status or score.
     */
    public ExerciseSubmissionResultAdminDto saveExerciseFeedback(UUID submissionId, String feedback) {
        HomeworkSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        if (!HomeworkStatus.GRADED.name().equals(submission.getStatus())) {
            throw new NotSubmittedException("Esta entrega todavía no ha sido calificada automáticamente.");
        }
        HomeworkAssignment assignment = requireAssignment(submission.getAssignmentId());
        if (assignment.getFormat() != HomeworkFormat.EXERCISE
                && assignment.getFormat() != HomeworkFormat.MIXED) {
            throw new AssignmentNotFoundException("Esta entrega no es un ejercicio auto-corregible.");
        }
        String encoded = FormattedTextSegment.encodePlainFeedback(feedback);
        submissionRepository.updateExerciseFeedback(submissionId, encoded);
        return getExerciseResult(submissionId);
    }

    /** Saves progress or finalizes percent grades + annotations for a MANUAL / MIXED / WRITE submission. */
    public HomeworkSubmissionAdminDto saveFeedback(UUID submissionId, SaveHomeworkFeedbackRequest request) {
        var row = submissionRepository.findDetailById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        if ("LEGACY_RICH".equals(row.reviewModel())) {
            throw new AlreadyReviewedException("Esta entrega ya ha sido revisada.");
        }

        HomeworkAssignment assignment = requireAssignment(
                submissionRepository.findById(submissionId)
                        .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."))
                        .getAssignmentId());
        HomeworkSubmission submissionEntity = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        List<HomeworkQuestion> questions = assignmentSnapshot.present(submissionEntity)
                ? assignmentSnapshot.questionsOf(submissionEntity)
                : questionRepository.findByAssignment(assignment.getId());
        HomeworkComposition composition = HomeworkCompositionSupport.compositionFromQuestions(
                assignment.getHomeworkType(),
                questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());

        boolean firstReview = HomeworkStatus.SUBMITTED.name().equals(row.status());
        boolean scoredReEdit = (composition == HomeworkComposition.MIXED
                || composition == HomeworkComposition.ALL_MANUAL
                || composition == HomeworkComposition.WRITE)
                && HomeworkStatus.GRADED.name().equals(row.status())
                && "ANNOTATED".equals(row.reviewModel());
        boolean legacyManualReEdit = composition != HomeworkComposition.MIXED
                && composition != HomeworkComposition.ALL_MANUAL
                && composition != HomeworkComposition.WRITE
                && HomeworkStatus.REVIEWED.name().equals(row.status())
                && "ANNOTATED".equals(row.reviewModel());

        if (!firstReview && !scoredReEdit && !legacyManualReEdit) {
            if (HomeworkStatus.REVIEWED.name().equals(row.status())
                    || HomeworkStatus.GRADED.name().equals(row.status())) {
                throw new AlreadyReviewedException("Esta entrega ya ha sido revisada.");
            }
            throw new NotSubmittedException("Esta entrega todavía no ha sido enviada por el alumno.");
        }

        String feedbackJson = FormattedTextSegment.encodePlainFeedback(
                request == null ? null : request.feedbackText(),
                FormattedTextSegment.MAX_MANUAL_FEEDBACK_LENGTH);

        List<com.kuky.backend.learning.model.HomeworkAnswer> storedAnswers =
                answerRepository.findBySubmission(submissionId);
        Map<UUID, QuestionKind> kindByQuestion = questions.stream()
                .collect(Collectors.toMap(HomeworkQuestion::getId, HomeworkQuestion::getKind, (a, b) -> a));

        // GRADED re-edits always finalize; otherwise honor request.finalize (default progress).
        boolean finalize = scoredReEdit
                || (request != null && Boolean.TRUE.equals(request.finalizeGrade()));

        if (storedAnswers.isEmpty()) {
            saveWriteFeedback(submissionId, request, row, feedbackJson, firstReview, finalize);
        } else if (composition == HomeworkComposition.MIXED
                || composition == HomeworkComposition.ALL_MANUAL) {
            saveQuestionFeedback(submissionId, request, storedAnswers, kindByQuestion, questions,
                    feedbackJson, firstReview, finalize);
        } else {
            throw new IllegalStateException("Unexpected composition for manual review: " + composition);
        }
        var updated = submissionRepository.findDetailById(submissionId).orElseThrow();
        return toSubmissionAdminDto(updated);
    }

    private void saveWriteFeedback(UUID submissionId, SaveHomeworkFeedbackRequest request,
                                   HomeworkSubmissionRepository.SubmissionDetailRow row,
                                   String feedbackJson, boolean firstReview, boolean finalize) {
        List<FormattedTextSegment> response = request == null ? null : request.response();
        String responseText = null;
        if (response != null) {
            FormattedTextSegment.validate(response);
            assertUnchangedWording(response, row.responseText());
            responseText = FormattedTextSegment.toJson(response);
        }

        Integer requestedPercent = request == null ? null : request.teacherScorePercent();
        Integer effectivePercent = requestedPercent != null ? requestedPercent : row.teacherScorePercent();

        if (!finalize) {
            Integer draftPercent = requestedPercent == null ? null : parseTeacherPercent(requestedPercent);
            submissionRepository.saveAnnotatedProgress(submissionId, feedbackJson, responseText, draftPercent);
            return;
        }

        int percent = parseTeacherPercent(effectivePercent);
        submissionRepository.saveScoredAnnotatedReview(
                submissionId, feedbackJson, responseText, percent, percent, firstReview);
    }

    private void saveQuestionFeedback(UUID submissionId, SaveHomeworkFeedbackRequest request,
                                      List<com.kuky.backend.learning.model.HomeworkAnswer> storedAnswers,
                                      Map<UUID, QuestionKind> kindByQuestion,
                                      List<HomeworkQuestion> questions,
                                      String feedbackJson, boolean firstReview, boolean finalize) {
        List<SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest> requested =
                request == null || request.answers() == null ? List.of() : request.answers();
        Map<UUID, SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest> byQuestion = requested.stream()
                .filter(a -> a.questionId() != null)
                .collect(Collectors.toMap(SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest::questionId,
                        a -> a, (a, b) -> a));

        List<com.kuky.backend.learning.model.HomeworkAnswer> freeTextAnswers = storedAnswers.stream()
                .filter(a -> a.getQuestionId() != null
                        && kindByQuestion.get(a.getQuestionId()) == QuestionKind.FREE_TEXT)
                .toList();

        for (var stored : freeTextAnswers) {
            var annotation = byQuestion.get(stored.getQuestionId());
            if (annotation == null) {
                if (finalize && stored.getTeacherScorePercent() == null) {
                    throw new IllegalArgumentException(
                            "Debes asignar una puntuación a cada respuesta de texto libre.");
                }
                continue;
            }

            if (annotation.formatted() != null) {
                FormattedTextSegment.validate(annotation.formatted());
                assertUnchangedWording(annotation.formatted(), stored.getAnswerText());
            }

            Integer requestedPercent = annotation.teacherScorePercent();
            if (requestedPercent != null) {
                int percent = parseTeacherPercent(requestedPercent);
                String answerText = annotation.formatted() != null
                        ? FormattedTextSegment.toJson(annotation.formatted())
                        : stored.getAnswerText();
                BigDecimal score = HomeworkCompositionSupport.scoreAsDecimal(
                        HomeworkCompositionSupport.teacherPercentAsScore(percent));
                answerRepository.updateManualReview(stored.getId(), answerText, percent, score);
                stored.setTeacherScorePercent(percent);
                stored.setScore(score);
                if (annotation.formatted() != null) {
                    stored.setAnswerText(answerText);
                }
            } else if (annotation.formatted() != null) {
                String answerText = FormattedTextSegment.toJson(annotation.formatted());
                if (stored.getTeacherScorePercent() != null) {
                    answerRepository.updateManualReview(
                            stored.getId(), answerText, stored.getTeacherScorePercent(), stored.getScore());
                } else if (finalize) {
                    throw new IllegalArgumentException(
                            "Debes asignar una puntuación a cada respuesta de texto libre.");
                } else {
                    answerRepository.updateAnswerText(stored.getId(), answerText);
                }
                stored.setAnswerText(answerText);
            } else if (finalize && stored.getTeacherScorePercent() == null) {
                throw new IllegalArgumentException(
                        "Debes asignar una puntuación a cada respuesta de texto libre.");
            }
        }

        if (!finalize) {
            submissionRepository.saveAnnotatedProgress(submissionId, feedbackJson, null, null);
            return;
        }

        Map<UUID, com.kuky.backend.learning.model.HomeworkAnswer> answersByQ = new LinkedHashMap<>();
        for (var refreshed : answerRepository.findBySubmission(submissionId)) {
            if (refreshed.getQuestionId() != null) {
                answersByQ.put(refreshed.getQuestionId(), refreshed);
            }
        }

        for (var stored : freeTextAnswers) {
            var refreshed = answersByQ.get(stored.getQuestionId());
            Integer percent = refreshed == null ? null : refreshed.getTeacherScorePercent();
            if (percent == null) {
                throw new IllegalArgumentException(
                        "Debes asignar una puntuación a cada respuesta de texto libre.");
            }
        }

        List<BigDecimal> scores = new ArrayList<>();
        for (HomeworkQuestion q : questions) {
            scores.addAll(HomeworkCompositionSupport.contributions(q, answersByQ.get(q.getId())));
        }
        int scorePercent = HomeworkCompositionSupport.scorePercentFromScores(scores);
        submissionRepository.saveScoredAnnotatedReview(submissionId, feedbackJson, null, scorePercent, firstReview);
    }

    private static int parseTeacherPercent(Integer raw) {
        if (raw == null) {
            throw new IllegalArgumentException(
                    "Debes asignar una puntuación a cada respuesta de texto libre.");
        }
        if (raw < 0 || raw > 100) {
            throw new IllegalArgumentException("La puntuación debe ser un entero entre 0 y 100.");
        }
        return raw;
    }

    private static void assertUnchangedWording(List<FormattedTextSegment> incoming, String stored) {
        if (!FormattedTextSegment.plainText(incoming)
                .equals(FormattedTextSegment.storedPlainWording(stored))) {
            throw new IllegalArgumentException("No se puede modificar el texto de la respuesta del alumno.");
        }
    }

    private HomeworkSubmissionAdminDto toSubmissionAdminDto(HomeworkSubmissionRepository.SubmissionDetailRow row) {
        List<ManualAnswerViewDto> answers = answerRepository.findBySubmission(row.submissionId()).stream()
                .filter(a -> a.getPromptSnapshot() != null || a.getAnswerText() != null)
                .map(a -> ManualAnswerViewDto.fromStored(
                        a.getQuestionId(), a.getPromptSnapshot(), a.getAnswerText(),
                        a.getTeacherScorePercent(),
                        a.getScore() == null ? null : a.getScore().doubleValue()))
                .toList();
        List<FormattedTextSegment> response = answers.isEmpty()
                ? FormattedTextSegment.fromJson(row.responseText())
                : null;
        HomeworkFormat format = row.format() == null ? HomeworkFormat.MANUAL : HomeworkFormat.valueOf(row.format());
        HomeworkType type = row.homeworkType() == null ? null : HomeworkType.valueOf(row.homeworkType());
        HomeworkComposition composition;
        if (type == HomeworkType.WRITE) {
            composition = HomeworkComposition.WRITE;
        } else if (format == HomeworkFormat.MIXED) {
            composition = HomeworkComposition.MIXED;
        } else if (format == HomeworkFormat.EXERCISE) {
            composition = HomeworkComposition.ALL_AUTO;
        } else {
            composition = HomeworkComposition.ALL_MANUAL;
        }
        String title = row.assignmentTitle();
        HomeworkSubmission submission = submissionRepository.findById(row.submissionId()).orElse(null);
        if (assignmentSnapshot.present(submission)) {
            HomeworkAssignment overlay = new HomeworkAssignment();
            overlay.setTitle(title);
            assignmentSnapshot.applyContent(overlay, submission);
            title = overlay.getTitle();
        }
        return new HomeworkSubmissionAdminDto(
                row.submissionId(), row.studentId(), row.studentEmail(), row.studentFirstName(),
                row.studentLastName(), row.studentUsername(), title, row.status(),
                format.name(),
                composition.name(),
                row.reviewModel(),
                response,
                answers,
                "LEGACY_RICH".equals(row.reviewModel()) ? FormattedTextSegment.fromJson(row.feedback()) : null,
                "ANNOTATED".equals(row.reviewModel())
                        ? FormattedTextSegment.decodePlainFeedback(row.feedback()) : null,
                row.scorePercent(),
                row.teacherScorePercent(),
                row.submittedAt(), row.reviewedAt());
    }

    public List<HomeworkAdminItem> list() {
        return contentRepository.findAllAssignments().stream()
                .map(this::toItem)
                .toList();
    }

    public HomeworkAdminItem findById(UUID id) {
        return toItem(requireAssignment(id));
    }

    public HomeworkAdminItem create(CreateHomeworkRequest req) {
        List<UUID> assignees = req.assigneeIds() == null ? List.of() : req.assigneeIds();
        validateStudents(assignees);
        HomeworkType type = parseType(req.homeworkType());
        HomeworkLevel level = parseLevel(req.level());
        boolean write = type == HomeworkType.WRITE;
        List<HomeworkQuestion> questions = validateAndMapQuestions(write, req.questions());
        HomeworkComposition composition = HomeworkCompositionSupport.compositionFromQuestions(
                type, questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
        HomeworkFormat format = HomeworkCompositionSupport.formatFromComposition(composition);
        Audio audio = resolveAudio(type, req.mediaSourceKind(), req.audioUrl(), req.audioFileId());

        UUID id = contentRepository.insertAssignment(req.title(), req.instructions(), req.dueOn(), type, level, format,
                audio.url(), audio.fileId(), audio.kind(), normalizeLabels(req.labels()));
        questionRepository.replaceQuestions(id, questions);
        if (!assignees.isEmpty()) {
            targetRepository.replaceTargets(id, assignees);
        }
        return toItem(requireAssignment(id));
    }

    public HomeworkAdminItem update(UUID id, UpdateHomeworkRequest req) {
        HomeworkAssignment existing = requireAssignment(id);
        HomeworkType type = parseType(req.homeworkType());
        HomeworkLevel level = parseLevel(req.level());
        boolean write = type == HomeworkType.WRITE;
        List<HomeworkQuestion> questions = validateAndMapQuestions(write, req.questions());
        HomeworkComposition composition = HomeworkCompositionSupport.compositionFromQuestions(
                type, questions.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
        HomeworkFormat format = HomeworkCompositionSupport.formatFromComposition(composition);
        Audio audio = resolveAudio(type, req.mediaSourceKind(), req.audioUrl(), req.audioFileId());

        Instant contentRevisedAt = contentChanged(existing, req, type, level, audio, questions)
                ? Instant.now() : null;
        contentRepository.updateAssignment(id, req.title(), req.instructions(), req.dueOn(), type, level, format,
                audio.url(), audio.fileId(), audio.kind(), normalizeLabels(req.labels()), contentRevisedAt);
        // Upsert by question/option id so existing submissions keep their answers linked.
        questionRepository.replaceQuestions(id, questions);
        return toItem(requireAssignment(id));
    }

    private boolean contentChanged(HomeworkAssignment existing, UpdateHomeworkRequest req,
                                   HomeworkType type, HomeworkLevel level, Audio audio,
                                   List<HomeworkQuestion> incoming) {
        if (!Objects.equals(existing.getTitle(), req.title())) return true;
        if (!Objects.equals(existing.getInstructions(), req.instructions())) return true;
        if (!Objects.equals(existing.getHomeworkType(), type)) return true;
        if (!Objects.equals(existing.getLevel(), level)) return true;
        if (!Objects.equals(existing.getAudioUrl(), audio.url())) return true;
        if (!Objects.equals(existing.getAudioFileId(), audio.fileId())) return true;
        if (!Objects.equals(existing.getMediaSourceKind(), audio.kind())) return true;
        return !questionsMatch(questionRepository.findByAssignment(existing.getId()), incoming);
    }

    private boolean questionsMatch(List<HomeworkQuestion> live, List<HomeworkQuestion> incoming) {
        List<HomeworkQuestion> a = live == null ? List.of() : live;
        List<HomeworkQuestion> b = incoming == null ? List.of() : incoming;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            HomeworkQuestion left = a.get(i);
            HomeworkQuestion right = b.get(i);
            if (!Objects.equals(left.getId(), right.getId())) return false;
            if (left.getKind() != right.getKind()) return false;
            if (!Objects.equals(left.getPrompt(), right.getPrompt())) return false;
            if (!structureEquals(left.getStructureJson(), right.getStructureJson())) return false;
            List<QuestionOption> lo = left.getOptions() == null ? List.of() : left.getOptions();
            List<QuestionOption> ro = right.getOptions() == null ? List.of() : right.getOptions();
            if (lo.size() != ro.size()) return false;
            for (int j = 0; j < lo.size(); j++) {
                QuestionOption ol = lo.get(j);
                QuestionOption or = ro.get(j);
                if (!Objects.equals(ol.getId(), or.getId())) return false;
                if (!Objects.equals(ol.getLabel(), or.getLabel())) return false;
                if (ol.isCorrect() != or.isCorrect()) return false;
            }
        }
        return true;
    }

    private boolean structureEquals(String left, String right) {
        String l = left == null || left.isBlank() ? "{}" : left;
        String r = right == null || right.isBlank() ? "{}" : right;
        try {
            return objectMapper.readTree(l).equals(objectMapper.readTree(r));
        } catch (JsonProcessingException e) {
            return Objects.equals(l, r);
        }
    }

    public HomeworkAdminItem setAssignees(UUID id, List<UUID> assigneeIds) {
        requireAssignment(id);
        validateStudents(assigneeIds);
        targetRepository.replaceTargets(id, assigneeIds);
        return toItem(requireAssignment(id));
    }

    public HomeworkAdminItem updateLabels(UUID id, List<String> raw) {
        requireAssignment(id);
        if (contentRepository.updateLabels(id, normalizeLabels(raw)) == 0) {
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
        return toItem(requireAssignment(id));
    }

    public void delete(UUID id) {
        if (contentRepository.deleteAssignment(id) == 0) {
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
    }

    // --- question validation + mapping --------------------------------------

    /**
     * Public entry for activity authoring (and tests). {@code writeHomework=true} requires
     * an empty question list (WRITE homework); otherwise ≥1 question of any mix of kinds.
     */
    public List<HomeworkQuestion> validateAndMapQuestions(boolean writeHomework, List<HomeworkQuestionDto> dtos) {
        List<HomeworkQuestionDto> questions = dtos == null ? List.of() : dtos;

        if (writeHomework) {
            if (!questions.isEmpty()) {
                throw new IllegalArgumentException("Una tarea de escritura no puede tener preguntas.");
            }
            return List.of();
        }

        if (questions.isEmpty()) {
            throw new IllegalArgumentException("La tarea necesita al menos una pregunta.");
        }

        List<HomeworkQuestion> mapped = new ArrayList<>();
        for (HomeworkQuestionDto q : questions) {
            if (q.prompt() == null || q.prompt().isBlank()) {
                throw new IllegalArgumentException("Cada pregunta necesita un enunciado.");
            }
            QuestionKind kind = parseKind(q.kind());
            List<HomeworkQuestionDto.OptionDto> opts = q.options() == null ? List.of() : q.options();

            HomeworkQuestion model = new HomeworkQuestion();
            // Preserve client-supplied ids so updates upsert instead of wiping submissions.
            model.setId(q.id());
            model.setKind(kind);
            model.setPrompt(q.prompt().strip());

            if (kind == QuestionKind.FREE_TEXT) {
                if (!opts.isEmpty()) {
                    throw new IllegalArgumentException("Las preguntas de texto libre no admiten opciones.");
                }
                if (!isStructureEmpty(q.structure())) {
                    throw new IllegalArgumentException("Las preguntas de texto libre no admiten una estructura adicional.");
                }
                model.setStructureJson("{}");
                model.setOptions(List.of());
            } else if (kind == QuestionKind.SINGLE_CHOICE) {
                mapSingleChoice(model, model.getPrompt(), opts, q.structure());
            } else if (kind.isStructured()) {
                if (!opts.isEmpty()) {
                    throw new IllegalArgumentException("Este tipo de pregunta no admite opciones.");
                }
                JsonNode normalized = validateStructure(kind, model.getPrompt(), q.structure());
                model.setStructureJson(writeStructure(normalized));
                model.setOptions(List.of());
            } else {
                if (!isStructureEmpty(q.structure())) {
                    throw new IllegalArgumentException("Este tipo de pregunta no admite una estructura adicional.");
                }
                model.setStructureJson("{}");
                validateOptions(kind, opts);
                List<QuestionOption> optionModels = new ArrayList<>();
                for (HomeworkQuestionDto.OptionDto o : opts) {
                    if (o.label() == null || o.label().isBlank()) {
                        throw new IllegalArgumentException("Las opciones y respuestas no pueden estar vacías.");
                    }
                    QuestionOption om = new QuestionOption();
                    om.setId(o.id());
                    om.setLabel(o.label().strip());
                    om.setCorrect(o.correct());
                    optionModels.add(om);
                }
                model.setOptions(optionModels);
            }
            mapped.add(model);
        }
        return mapped;
    }

    /**
     * @deprecated Prefer {@link #validateAndMapQuestions(boolean, List)}.
     */
    @Deprecated
    public List<HomeworkQuestion> validateAndMapQuestions(HomeworkFormat format, List<HomeworkQuestionDto> dtos,
                                                          boolean allowManualFreeText) {
        if (format == HomeworkFormat.MANUAL && !allowManualFreeText) {
            return validateAndMapQuestions(true, dtos);
        }
        return validateAndMapQuestions(false, dtos);
    }

    /**
     * Numbered mode when the prompt has {@code (N)} markers: persist {@code structure.items}
     * and zero options-table rows. No markers: classic options and {@code {}}.
     */
    private void mapSingleChoice(HomeworkQuestion model, String prompt,
                                 List<HomeworkQuestionDto.OptionDto> opts, JsonNode structure) {
        SingleChoiceMarkerParser.ParseResult parsed = SingleChoiceMarkerParser.parse(prompt);
        if (parsed.numbered()) {
            if (!parsed.valid()) {
                throw new IllegalArgumentException(parsed.errorMessage());
            }
            JsonNode itemsNode = structure == null ? null : structure.get("items");
            JsonNode normalized = validateNumberedSingleChoice(parsed.n(), itemsNode, opts);
            model.setStructureJson(writeStructure(normalized));
            model.setOptions(List.of());
            return;
        }
        model.setStructureJson("{}");
        validateOptions(QuestionKind.SINGLE_CHOICE, opts);
        List<QuestionOption> optionModels = new ArrayList<>();
        for (HomeworkQuestionDto.OptionDto o : opts) {
            if (o.label() == null || o.label().isBlank()) {
                throw new IllegalArgumentException("Las opciones y respuestas no pueden estar vacías.");
            }
            QuestionOption om = new QuestionOption();
            om.setId(o.id());
            om.setLabel(o.label().strip());
            om.setCorrect(o.correct());
            optionModels.add(om);
        }
        model.setOptions(optionModels);
    }

    private JsonNode validateNumberedSingleChoice(
            int n, JsonNode itemsNode, List<HomeworkQuestionDto.OptionDto> classicOpts) {
        Map<Integer, JsonNode> byNumber = new LinkedHashMap<>();
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                int number = item.path("number").asInt(0);
                if (number >= 1) byNumber.put(number, item);
            }
        }
        JsonNode item1 = byNumber.get(1);
        boolean item1Empty = item1 == null || !item1.path("options").isArray()
                || item1.path("options").isEmpty();
        if (item1Empty && classicOpts != null && !classicOpts.isEmpty()) {
            ObjectNode seeded = objectMapper.createObjectNode();
            seeded.put("number", 1);
            ArrayNode seededOpts = objectMapper.createArrayNode();
            for (HomeworkQuestionDto.OptionDto o : classicOpts) {
                ObjectNode opt = objectMapper.createObjectNode();
                if (o.id() != null) opt.put("id", o.id().toString());
                opt.put("label", o.label() == null ? "" : o.label());
                opt.put("correct", o.correct());
                seededOpts.add(opt);
            }
            seeded.set("options", seededOpts);
            byNumber.put(1, seeded);
        }

        ArrayNode items = objectMapper.createArrayNode();
        for (int i = 1; i <= n; i++) {
            JsonNode src = byNumber.get(i);
            if (src == null) {
                throw new IllegalArgumentException("Faltan las opciones del ítem (" + i + ").");
            }
            JsonNode optsNode = src.path("options");
            if (!optsNode.isArray()) {
                throw new IllegalArgumentException(
                        "El ítem (" + i + ") necesita al menos dos opciones.");
            }
            List<HomeworkQuestionDto.OptionDto> mapped = new ArrayList<>();
            ArrayNode normalizedOpts = objectMapper.createArrayNode();
            for (JsonNode opt : optsNode) {
                String label = opt.path("label").asText("");
                boolean correct = opt.path("correct").asBoolean(false);
                String id = normalizeOrGenerateId(opt);
                mapped.add(new HomeworkQuestionDto.OptionDto(parseOptionUuid(id), label, correct));
                ObjectNode out = objectMapper.createObjectNode();
                out.put("id", id);
                out.put("label", label);
                out.put("correct", correct);
                normalizedOpts.add(out);
            }
            try {
                validateOptions(QuestionKind.SINGLE_CHOICE, mapped);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(itemizeSingleChoiceError(i, e.getMessage()));
            }
            for (int j = 0; j < mapped.size(); j++) {
                HomeworkQuestionDto.OptionDto o = mapped.get(j);
                if (o.label() == null || o.label().isBlank()) {
                    throw new IllegalArgumentException(
                            "Las opciones del ítem (" + i + ") no pueden estar vacías.");
                }
                ((ObjectNode) normalizedOpts.get(j)).put("label", o.label().strip());
            }
            ObjectNode item = objectMapper.createObjectNode();
            item.put("number", i);
            item.set("options", normalizedOpts);
            items.add(item);
        }
        ObjectNode structure = objectMapper.createObjectNode();
        structure.set("items", items);
        return structure;
    }

    private static UUID parseOptionUuid(String id) {
        if (id == null || id.isBlank()) return null;
        try {
            return UUID.fromString(id.strip());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String itemizeSingleChoiceError(int itemNumber, String message) {
        if (message == null) return "El ítem (" + itemNumber + ") no es válido.";
        if (message.contains("al menos dos opciones")) {
            return "El ítem (" + itemNumber + ") necesita al menos dos opciones.";
        }
        if (message.contains("exactamente una opción correcta")) {
            return "Marca exactamente una opción correcta en el ítem (" + itemNumber + ").";
        }
        return message;
    }

    private void validateOptions(QuestionKind kind, List<HomeworkQuestionDto.OptionDto> opts) {
        switch (kind) {
            case SINGLE_CHOICE -> {
                if (opts.size() < 2) {
                    throw new IllegalArgumentException("Una pregunta de opción única necesita al menos dos opciones.");
                }
                long correct = opts.stream().filter(HomeworkQuestionDto.OptionDto::correct).count();
                if (correct != 1) {
                    throw new IllegalArgumentException("Marca exactamente una opción correcta en la pregunta de opción única.");
                }
            }
            case MULTI_CHOICE -> {
                if (opts.size() < 2) {
                    throw new IllegalArgumentException("Una pregunta de opción múltiple necesita al menos dos opciones.");
                }
                long correct = opts.stream().filter(HomeworkQuestionDto.OptionDto::correct).count();
                if (correct < 1) {
                    throw new IllegalArgumentException("Marca al menos una opción correcta en la pregunta de opción múltiple.");
                }
            }
            case TRUE_FALSE -> {
                if (opts.size() != 2) {
                    throw new IllegalArgumentException("Una pregunta de verdadero/falso necesita exactamente dos opciones.");
                }
                String label0 = opts.get(0).label() == null ? "" : opts.get(0).label().strip();
                String label1 = opts.get(1).label() == null ? "" : opts.get(1).label().strip();
                if (!"true".equals(label0) || !"false".equals(label1)) {
                    throw new IllegalArgumentException(
                            "Las opciones de verdadero/falso deben ser «true» y «false», en ese orden.");
                }
                long correct = opts.stream().filter(HomeworkQuestionDto.OptionDto::correct).count();
                if (correct != 1) {
                    throw new IllegalArgumentException(
                            "Marca exactamente una opción correcta en la pregunta de verdadero/falso.");
                }
            }
            default -> { /* structured kinds are validated via validateStructure */ }
        }
    }

    // --- structured question validation (MULTI_BLANK, DRAG_DROP, TABLE_FILL, MATCHING) -------

    /** {@code true} for {@code null}, a missing node, or an empty JSON object — i.e. "no structure". */
    private static boolean isStructureEmpty(JsonNode structure) {
        return structure == null || structure.isNull() || structure.isMissingNode()
                || (structure.isObject() && structure.isEmpty());
    }

    private String writeStructure(JsonNode structure) {
        try {
            return objectMapper.writeValueAsString(structure);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo guardar la estructura de la pregunta.", e);
        }
    }

    /**
     * Validates the kind-specific {@code structure} payload and returns a
     * normalized {@link JsonNode} (trimmed strings, ids generated where
     * missing) ready to persist as {@code structure_json}. Throws
     * {@link IllegalArgumentException} (→ VALIDATION_ERROR) on any rule
     * violation, per {@code specs/024-new-exercise-types/data-model.md}.
     */
    private JsonNode validateStructure(QuestionKind kind, String prompt, JsonNode structure) {
        if (isStructureEmpty(structure) || !structure.isObject()) {
            throw new IllegalArgumentException("Esta pregunta necesita una estructura válida.");
        }
        return switch (kind) {
            case MULTI_BLANK -> validateMultiBlank(prompt, structure);
            case DRAG_DROP -> validateDragDrop(prompt, structure);
            case TABLE_FILL -> validateTableFill(structure);
            case MATCHING -> validateMatching(structure);
            default -> throw new IllegalStateException("Tipo de pregunta no estructurado: " + kind);
        };
    }

    private JsonNode validateMultiBlank(String prompt, JsonNode structure) {
        int blankCount = BlankPassageParser.countBlanks(prompt);
        if (blankCount < 1 || blankCount > 20) {
            throw new IllegalArgumentException("El enunciado debe tener entre 1 y 20 huecos (___).");
        }
        JsonNode blanksNode = structure.get("blanks");
        if (blanksNode == null || !blanksNode.isArray() || blanksNode.size() != blankCount) {
            throw new IllegalArgumentException("El número de respuestas no coincide con el número de huecos del enunciado.");
        }
        ArrayNode blanks = objectMapper.createArrayNode();
        for (JsonNode blankNode : blanksNode) {
            ObjectNode entry = objectMapper.createObjectNode();
            entry.set("acceptedAnswers", normalizeMultiBlankAnswerList(
                    blankNode == null ? null : blankNode.get("acceptedAnswers")));
            blanks.add(entry);
        }
        return objectMapper.createObjectNode().set("blanks", blanks);
    }

    private JsonNode validateDragDrop(String prompt, JsonNode structure) {
        int blankCount = BlankPassageParser.countBlanks(prompt);
        if (blankCount < 2 || blankCount > 20) {
            throw new IllegalArgumentException("El enunciado debe tener entre 2 y 20 huecos (___).");
        }
        JsonNode bankNode = structure.get("bank");
        if (bankNode == null || !bankNode.isArray()) {
            throw new IllegalArgumentException("El banco de palabras es obligatorio.");
        }
        if (bankNode.size() < 1 || bankNode.size() > ExerciseStructureLimits.MAX_BANK_ITEMS) {
            throw new IllegalArgumentException(
                    "El banco de palabras debe tener entre 1 y "
                            + ExerciseStructureLimits.MAX_BANK_ITEMS + " elementos.");
        }

        ArrayNode bank = objectMapper.createArrayNode();
        Set<String> seenIds = new HashSet<>();
        Map<String, String> idToLabel = new LinkedHashMap<>();
        for (JsonNode item : bankNode) {
            String label = textOrNull(item, "label");
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("Las palabras del banco no pueden estar vacías.");
            }
            String id = normalizeOrGenerateId(item);
            if (!seenIds.add(id)) {
                throw new IllegalArgumentException("Los identificadores del banco de palabras deben ser únicos.");
            }
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("id", id);
            entry.put("label", label.strip());
            bank.add(entry);
            idToLabel.put(id, label.strip());
        }

        JsonNode blanksNode = structure.get("blanks");
        boolean legacy = blanksNode == null || !blanksNode.isArray() || blanksNode.isEmpty();
        if (legacy) {
            if (bank.size() != blankCount) {
                throw new IllegalArgumentException(
                        "El banco de palabras debe tener el mismo número de elementos que huecos.");
            }
            ArrayNode blanks = objectMapper.createArrayNode();
            for (JsonNode item : bank) {
                ObjectNode blank = objectMapper.createObjectNode();
                ArrayNode ids = objectMapper.createArrayNode();
                ids.add(item.path("id").asText());
                blank.set("correctBankIds", ids);
                blanks.add(blank);
            }
            ObjectNode result = objectMapper.createObjectNode();
            result.set("bank", bank);
            result.set("blanks", blanks);
            return result;
        }

        if (blanksNode.size() != blankCount) {
            throw new IllegalArgumentException(
                    "El número de respuestas no coincide con el número de huecos del enunciado.");
        }

        ArrayNode blanks = objectMapper.createArrayNode();
        for (JsonNode blankNode : blanksNode) {
            JsonNode idsNode = blankNode == null ? null : blankNode.get("correctBankIds");
            if (idsNode == null || !idsNode.isArray() || idsNode.isEmpty()) {
                throw new IllegalArgumentException("Cada hueco necesita al menos una palabra correcta del banco.");
            }
            if (idsNode.size() > ExerciseStructureLimits.MAX_ACCEPTED_PER_BLANK) {
                throw new IllegalArgumentException(
                        "Cada hueco puede tener como máximo "
                                + ExerciseStructureLimits.MAX_ACCEPTED_PER_BLANK
                                + " palabras correctas.");
            }
            ArrayNode ids = objectMapper.createArrayNode();
            Set<String> seenInBlank = new HashSet<>();
            for (JsonNode idNode : idsNode) {
                String id = idNode != null && idNode.isTextual() ? idNode.asText().strip() : null;
                if (id == null || id.isBlank()) {
                    throw new IllegalArgumentException("Los identificadores correctos del banco no pueden estar vacíos.");
                }
                if (!idToLabel.containsKey(id)) {
                    throw new IllegalArgumentException("Hay una palabra correcta que no está en el banco.");
                }
                if (!seenInBlank.add(id)) {
                    continue; // silent dedupe within blank
                }
                ids.add(id);
            }
            if (ids.isEmpty()) {
                throw new IllegalArgumentException("Cada hueco necesita al menos una palabra correcta del banco.");
            }
            ObjectNode blank = objectMapper.createObjectNode();
            blank.set("correctBankIds", ids);
            blanks.add(blank);
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.set("bank", bank);
        result.set("blanks", blanks);
        return result;
    }

    private JsonNode validateTableFill(JsonNode structure) {
        JsonNode rowHeadersNode = structure.get("rowHeaders");
        JsonNode colHeadersNode = structure.get("colHeaders");
        if (rowHeadersNode == null || !rowHeadersNode.isArray() || colHeadersNode == null || !colHeadersNode.isArray()) {
            throw new IllegalArgumentException("La tabla necesita encabezados de filas y columnas.");
        }
        int rows = rowHeadersNode.size();
        int cols = colHeadersNode.size();
        if (rows < 1 || rows > 12 || cols < 1 || cols > 12) {
            throw new IllegalArgumentException("La tabla debe tener entre 1 y 12 filas y entre 1 y 12 columnas.");
        }
        ArrayNode rowHeaders = objectMapper.createArrayNode();
        rowHeadersNode.forEach(h -> rowHeaders.add(headerText(h)));
        ArrayNode colHeaders = objectMapper.createArrayNode();
        colHeadersNode.forEach(h -> colHeaders.add(headerText(h)));

        JsonNode cellsNode = structure.get("cells");
        if (cellsNode == null || !cellsNode.isArray()) {
            throw new IllegalArgumentException("La tabla necesita las celdas.");
        }
        Map<Integer, ObjectNode> byCoord = new LinkedHashMap<>();
        int blankCount = 0;
        for (JsonNode cell : cellsNode) {
            int r = requiredInt(cell, "r");
            int c = requiredInt(cell, "c");
            if (r < 0 || r >= rows || c < 0 || c >= cols) {
                throw new IllegalArgumentException("Las celdas de la tabla tienen coordenadas fuera de rango.");
            }
            int key = r * cols + c;
            if (byCoord.containsKey(key)) {
                throw new IllegalArgumentException("Cada celda de la tabla debe aparecer una sola vez.");
            }
            String type = textOrNull(cell, "type");
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("r", r);
            entry.put("c", c);
            if ("blank".equals(type)) {
                entry.put("type", "blank");
                entry.set("acceptedAnswers", normalizeAnswerList(cell.get("acceptedAnswers")));
                blankCount++;
            } else if ("fixed".equals(type)) {
                JsonNode textNode = cell.get("text");
                if (textNode == null || !textNode.isTextual()) {
                    throw new IllegalArgumentException("Las celdas fijas necesitan un texto (puede estar vacío).");
                }
                entry.put("type", "fixed");
                entry.put("text", textNode.asText());
            } else {
                throw new IllegalArgumentException("El tipo de celda no es válido.");
            }
            byCoord.put(key, entry);
        }
        if (byCoord.size() != rows * cols) {
            throw new IllegalArgumentException("La tabla debe tener una celda en cada posición de la cuadrícula.");
        }
        if (blankCount < 1 || blankCount > 50) {
            throw new IllegalArgumentException("La tabla debe tener entre 1 y 50 huecos.");
        }
        ArrayNode cells = objectMapper.createArrayNode();
        for (int key : new TreeSet<>(byCoord.keySet())) {
            cells.add(byCoord.get(key));
        }
        ObjectNode result = objectMapper.createObjectNode();
        result.set("rowHeaders", rowHeaders);
        result.set("colHeaders", colHeaders);
        result.set("cells", cells);
        return result;
    }

    private JsonNode validateMatching(JsonNode structure) {
        JsonNode leftNode = structure.get("left");
        JsonNode rightNode = structure.get("right");
        if (leftNode == null || !leftNode.isArray() || rightNode == null || !rightNode.isArray()) {
            throw new IllegalArgumentException("La pregunta de emparejar necesita las listas de la izquierda y la derecha.");
        }
        if (leftNode.isEmpty() || leftNode.size() > 20 || rightNode.isEmpty() || rightNode.size() > 20) {
            throw new IllegalArgumentException("Cada lista debe tener entre 1 y 20 elementos.");
        }
        Map<String, ObjectNode> leftById = normalizeMatchingSide(leftNode);
        Map<String, ObjectNode> rightById = normalizeMatchingSide(rightNode);

        JsonNode pairsNode = structure.get("pairs");
        if (pairsNode == null || !pairsNode.isArray() || pairsNode.isEmpty()) {
            throw new IllegalArgumentException("La pregunta de emparejar necesita al menos una pareja correcta.");
        }
        ArrayNode pairs = objectMapper.createArrayNode();
        Set<String> usedLeft = new HashSet<>();
        Set<String> usedRight = new HashSet<>();
        for (JsonNode pair : pairsNode) {
            String leftId = textOrNull(pair, "leftId");
            String rightId = textOrNull(pair, "rightId");
            if (leftId == null || !leftById.containsKey(leftId) || rightId == null || !rightById.containsKey(rightId)) {
                throw new IllegalArgumentException("Las parejas hacen referencia a elementos que no existen.");
            }
            if (!usedLeft.add(leftId) || !usedRight.add(rightId)) {
                throw new IllegalArgumentException("Cada elemento solo puede aparecer en una pareja.");
            }
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("leftId", leftId);
            entry.put("rightId", rightId);
            pairs.add(entry);
        }

        ArrayNode left = objectMapper.createArrayNode();
        leftById.values().forEach(left::add);
        ArrayNode right = objectMapper.createArrayNode();
        rightById.values().forEach(right::add);

        ObjectNode result = objectMapper.createObjectNode();
        result.set("left", left);
        result.set("right", right);
        result.set("pairs", pairs);
        return result;
    }

    private LinkedHashMap<String, ObjectNode> normalizeMatchingSide(JsonNode sideNode) {
        LinkedHashMap<String, ObjectNode> byId = new LinkedHashMap<>();
        for (JsonNode item : sideNode) {
            String label = textOrNull(item, "label");
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("Los elementos de la pregunta de emparejar no pueden estar vacíos.");
            }
            String id = normalizeOrGenerateId(item);
            if (byId.containsKey(id)) {
                throw new IllegalArgumentException("Los identificadores de la pregunta de emparejar deben ser únicos.");
            }
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("id", id);
            entry.put("label", label.strip());
            byId.put(id, entry);
        }
        return byId;
    }

    /**
     * MULTI_BLANK accepted answers: trim, silent dedupe by accent-exact normalize key,
     * enforce 1–{@link ExerciseStructureLimits#MAX_ACCEPTED_PER_BLANK}.
     * TABLE_FILL keeps {@link #normalizeAnswerList} without the cap.
     */
    private ArrayNode normalizeMultiBlankAnswerList(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            throw new IllegalArgumentException("Cada hueco necesita al menos una respuesta aceptada.");
        }
        ArrayNode result = objectMapper.createArrayNode();
        Set<String> seenNormalized = new HashSet<>();
        for (JsonNode item : node) {
            String value = item != null && item.isTextual() ? item.asText() : null;
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Las respuestas aceptadas no pueden estar vacías.");
            }
            String stripped = value.strip();
            String key = stripped.toLowerCase(Locale.ROOT);
            if (!seenNormalized.add(key)) {
                continue;
            }
            result.add(stripped);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Cada hueco necesita al menos una respuesta aceptada.");
        }
        if (result.size() > ExerciseStructureLimits.MAX_ACCEPTED_PER_BLANK) {
            throw new IllegalArgumentException(
                    "Cada hueco puede tener como máximo "
                            + ExerciseStructureLimits.MAX_ACCEPTED_PER_BLANK
                            + " respuestas aceptadas.");
        }
        return result;
    }

    /** Non-empty (after trim) accepted-answer list, normalized to trimmed strings (TABLE_FILL). */
    private ArrayNode normalizeAnswerList(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            throw new IllegalArgumentException("Cada hueco necesita al menos una respuesta aceptada.");
        }
        ArrayNode result = objectMapper.createArrayNode();
        for (JsonNode item : node) {
            String value = item != null && item.isTextual() ? item.asText() : null;
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Las respuestas aceptadas no pueden estar vacías.");
            }
            result.add(value.strip());
        }
        return result;
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static String normalizeOrGenerateId(JsonNode node) {
        String id = textOrNull(node, "id");
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id.strip();
    }

    private static String headerText(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : "";
    }

    private static int requiredInt(JsonNode cell, String field) {
        JsonNode value = cell == null ? null : cell.get(field);
        if (value == null || !value.isInt()) {
            throw new IllegalArgumentException("Las coordenadas de las celdas no son válidas.");
        }
        return value.asInt();
    }

    // --- audio source -------------------------------------------------------

    private record Audio(String url, UUID fileId, MediaSourceKind kind) {}

    /**
     * Resolves listening media. Non-AUDIO types clear all fields. AUDIO requires a
     * complete {@code mediaSourceKind} + matching payload (see ListeningMedia).
     */
    private Audio resolveAudio(HomeworkType type, String rawKind, String rawUrl, UUID fileId) {
        if (type != HomeworkType.AUDIO) {
            return new Audio(null, null, null);
        }
        MediaSourceKind kind = parseMediaSourceKind(rawKind);
        if (kind == null) {
            throw new IllegalArgumentException("La tarea de audio necesita una fuente de medios.");
        }
        String url = rawUrl == null || rawUrl.isBlank() ? null : rawUrl.strip();
        return switch (kind) {
            case UPLOADED_FILE -> {
                if (fileId == null) {
                    throw new IllegalArgumentException("Sube un archivo de audio o elige otra fuente.");
                }
                if (audioFileRepository.findOriginalName(fileId).isEmpty()) {
                    throw new IllegalArgumentException("El audio subido no existe.");
                }
                yield new Audio(null, fileId, kind);
            }
            case AUDIO_URL, VIDEO_PAGE -> {
                if (url == null) {
                    throw new IllegalArgumentException("Indica un enlace válido para la fuente de audio.");
                }
                yield new Audio(url, null, kind);
            }
            case YOUTUBE -> {
                if (ListeningMedia.extractYouTubeId(url) == null) {
                    throw new IllegalArgumentException("Indica un enlace de YouTube válido.");
                }
                yield new Audio(url, null, kind);
            }
        };
    }

    private static MediaSourceKind parseMediaSourceKind(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return MediaSourceKind.valueOf(raw.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de fuente de audio no válido.");
        }
    }

    // --- helpers -------------------------------------------------------------

    private HomeworkAssignment requireAssignment(UUID id) {
        return contentRepository.findAssignmentById(id)
                .orElseThrow(() -> new AssignmentNotFoundException("Tarea no encontrada."));
    }

    static final int MAX_LABEL_LENGTH = 40;

    /** Trim each value; drop blanks; case-insensitive dedupe (first spelling wins). */
    static List<String> normalizeLabels(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        for (String item : raw) {
            String normalized = normalizeLabel(item);
            if (normalized == null) {
                continue;
            }
            unique.putIfAbsent(labelGroupKey(normalized), normalized);
        }
        return List.copyOf(unique.values());
    }

    /** Trim; blank → null; reject more than 40 characters after trim. */
    static String normalizeLabel(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException("La etiqueta no puede superar los 40 caracteres.");
        }
        return trimmed;
    }

    static String labelGroupKey(String label) {
        return label.toLowerCase(Locale.forLanguageTag("es"));
    }

    private void validateStudents(List<UUID> userIds) {
        for (UUID userId : userIds) {
            User u = userRepository.findById(userId)
                    .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));
            if (!"STUDENT".equals(u.getRole())) {
                throw new StudentNotFoundException("El destinatario no es un alumno.");
            }
        }
    }

    private HomeworkAdminItem toItem(HomeworkAssignment a) {
        List<AssigneeDto> assignees = targetRepository.findAssigneesWithSubmissions(a.getId()).stream()
                .map(v -> new AssigneeDto(v.userId(), v.email(), v.firstName(), v.lastName(), v.username(),
                        v.status(), v.responseText(), v.submittedAt(), v.scorePercent(), v.submissionId(),
                        v.hasTeacherFeedback(), v.unseen()))
                .toList();
        String type = a.getHomeworkType() == null ? null : a.getHomeworkType().name();
        String level = a.getLevel() == null ? null : a.getLevel().name();
        String format = a.getFormat() == null ? HomeworkFormat.MANUAL.name() : a.getFormat().name();

        List<HomeworkQuestion> questionModels = a.getHomeworkType() == HomeworkType.WRITE
                ? List.of()
                : questionRepository.findByAssignment(a.getId());
        List<HomeworkQuestionDto> questions = questionModels.stream().map(this::toQuestionDto).toList();
        HomeworkComposition composition = HomeworkCompositionSupport.compositionFromQuestions(
                a.getHomeworkType(),
                questionModels.stream().map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());

        String audioFileName = a.getAudioFileId() == null
                ? null
                : audioFileRepository.findOriginalName(a.getAudioFileId()).orElse(null);

        return new HomeworkAdminItem(a.getId(), a.getTitle(), a.getInstructions(), a.getDueOn(),
                type, level, format, composition.name(), questions,
                a.getAudioUrl(), a.getAudioFileId(), audioFileName,
                a.getMediaSourceKind() == null ? null : a.getMediaSourceKind().name(),
                a.getLabels(),
                assignees, assignees.stream().anyMatch(AssigneeDto::unseen));
    }

    private HomeworkQuestionDto toQuestionDto(HomeworkQuestion q) {
        List<HomeworkQuestionDto.OptionDto> options = q.getOptions().stream()
                .map(o -> new HomeworkQuestionDto.OptionDto(o.getId(), o.getLabel(), o.isCorrect()))
                .toList();
        return new HomeworkQuestionDto(q.getId(), q.getKind().name(), q.getPrompt(), options, readStructure(q.getStructureJson()));
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

    private static HomeworkType parseType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return HomeworkType.valueOf(raw.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private static HomeworkLevel parseLevel(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return HomeworkLevel.valueOf(raw.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private static HomeworkFormat parseFormat(String raw) {
        if (raw == null || raw.isBlank()) return HomeworkFormat.MANUAL;
        try {
            return HomeworkFormat.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Formato de tarea no válido.");
        }
    }

    // parseFormat retained only for any leftover callers; create/update ignore client format.

    private static QuestionKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Cada pregunta necesita un tipo.");
        }
        try {
            return QuestionKind.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de pregunta no válido.");
        }
    }
}
