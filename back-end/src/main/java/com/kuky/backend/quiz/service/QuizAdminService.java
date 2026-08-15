package com.kuky.backend.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuky.backend.admin.dto.HomeworkQuestionDto;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.admin.service.HomeworkAdminService;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.ListeningMedia;
import com.kuky.backend.learning.model.MediaSourceKind;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.quiz.dto.CreateQuizRequest;
import com.kuky.backend.quiz.dto.QuizAdminDetail;
import com.kuky.backend.quiz.dto.QuizAdminListItem;
import com.kuky.backend.quiz.dto.QuizAssigneeDto;
import com.kuky.backend.quiz.dto.QuizAttemptListItem;
import com.kuky.backend.quiz.dto.QuizReviewQueueItemDto;
import com.kuky.backend.quiz.dto.QuizQuestionDto;
import com.kuky.backend.quiz.dto.QuizReviewRequest;
import com.kuky.backend.quiz.dto.QuizSkillScoreDto;
import com.kuky.backend.quiz.dto.QuizTakeResponse;
import com.kuky.backend.quiz.dto.StudentQuizSummary;
import com.kuky.backend.quiz.dto.UpdateQuizRequest;
import com.kuky.backend.quiz.exception.QuizNotFoundException;
import com.kuky.backend.quiz.model.Quiz;
import com.kuky.backend.quiz.model.QuizAnswer;
import com.kuky.backend.quiz.model.QuizAttempt;
import com.kuky.backend.quiz.model.QuizAttemptStatus;
import com.kuky.backend.quiz.model.QuizQuestion;
import com.kuky.backend.quiz.model.QuizSkill;
import com.kuky.backend.quiz.repository.QuizAssigneeRepository;
import com.kuky.backend.quiz.repository.QuizAttemptRepository;
import com.kuky.backend.quiz.repository.QuizQuestionRepository;
import com.kuky.backend.quiz.repository.QuizRepository;
import com.kuky.backend.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizAdminService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizAssigneeRepository assigneeRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final HomeworkAdminService homeworkAdminService;
    private final QuizSnapshot quizSnapshot;
    private final QuizGradingService gradingService;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public QuizAdminService(QuizRepository quizRepository,
                            QuizQuestionRepository questionRepository,
                            QuizAssigneeRepository assigneeRepository,
                            QuizAttemptRepository attemptRepository,
                            UserRepository userRepository,
                            HomeworkAdminService homeworkAdminService,
                            QuizSnapshot quizSnapshot,
                            QuizGradingService gradingService,
                            ObjectMapper objectMapper,
                            NotificationService notificationService) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.assigneeRepository = assigneeRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.homeworkAdminService = homeworkAdminService;
        this.quizSnapshot = quizSnapshot;
        this.gradingService = gradingService;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    public List<QuizAdminListItem> list() {
        return quizRepository.listWithCounts().stream()
                .map(r -> new QuizAdminListItem(r.id(), r.title(), r.questionCount(), r.assigneeCount(),
                        r.attemptCount(), r.hasUnseenAttempts()))
                .toList();
    }

    public QuizAdminDetail get(UUID id) {
        Quiz quiz = requireQuiz(id);
        return toDetail(quiz);
    }

    @Transactional
    public QuizAdminDetail create(CreateQuizRequest request) {
        Quiz quiz = new Quiz();
        quiz.setTitle(request.title().strip());
        quiz.setDescription(blankToNull(request.description()));
        quiz = quizRepository.insert(quiz);
        return toDetail(quiz);
    }

    @Transactional
    public QuizAdminDetail update(UUID id, UpdateQuizRequest request) {
        Quiz quiz = requireQuiz(id);
        quiz.setTitle(request.title().strip());
        quiz.setDescription(blankToNull(request.description()));
        quizRepository.update(quiz);
        List<QuizQuestion> questions = mapQuestions(request.questions());
        questionRepository.replaceQuestions(id, questions);
        return toDetail(requireQuiz(id));
    }

    @Transactional
    public void delete(UUID id) {
        if (quizRepository.delete(id) == 0) {
            throw new QuizNotFoundException("Prueba de evaluación no encontrada.");
        }
    }

    @Transactional
    public QuizAdminDetail setAssignees(UUID id, List<UUID> studentIds) {
        requireQuiz(id);
        if (questionRepository.countLive(id) < 1) {
            throw new IllegalArgumentException("La prueba de evaluación necesita al menos una pregunta antes de asignarla.");
        }
        List<UUID> ids = studentIds == null ? List.of() : studentIds;
        validateStudents(ids);

        Set<UUID> previous = assigneeRepository.findAssignees(id).stream()
                .map(QuizAssigneeRepository.AssigneeView::userId)
                .collect(Collectors.toSet());
        Set<UUID> next = new HashSet<>(ids);
        for (UUID userId : previous) {
            if (!next.contains(userId)) {
                attemptRepository.deleteInProgress(id, userId);
            }
        }
        assigneeRepository.replaceAssignees(id, ids);
        return toDetail(requireQuiz(id));
    }

    public List<QuizReviewQueueItemDto> listReviewQueue() {
        return attemptRepository.findSubmittedQueue().stream()
                .map(r -> new QuizReviewQueueItemDto(
                        r.attemptId(), r.quizId(), r.quizTitle(), r.studentId(),
                        r.studentEmail(), r.studentFirstName(), r.studentLastName(),
                        r.studentUsername(), r.submittedAt(), r.unseen()))
                .toList();
    }

    public List<QuizAttemptListItem> listAttempts(UUID quizId) {
        requireQuiz(quizId);
        return attemptRepository.findByQuiz(quizId).stream()
                .filter(a -> a.getStatus() != QuizAttemptStatus.IN_PROGRESS)
                .map(a -> {
                    User u = userRepository.findById(a.getUserId()).orElse(null);
                    boolean unseen = a.getStatus() != QuizAttemptStatus.IN_PROGRESS
                            && a.getTeacherSeenAt() == null;
                    return new QuizAttemptListItem(
                            a.getId(),
                            a.getUserId(),
                            displayName(u),
                            u == null ? null : u.getEmail(),
                            a.getStatus().name(),
                            a.getScorePercent(),
                            a.getSubmittedAt(),
                            unseen);
                })
                .toList();
    }

    public QuizTakeResponse getAttempt(UUID quizId, UUID attemptId) {
        QuizAttempt attempt = requireAttempt(quizId, attemptId);
        notificationService.markQuizAttemptSeen(attempt.getId());
        List<QuizQuestion> questions = quizSnapshot.questionsOf(attempt.getQuizSnapshot());
        List<QuizAnswer> answers = attemptRepository.findAnswers(attempt.getId());
        QuizGradingService.GradeOutcome outcome = gradingService.summarize(questions, answers, false);
        return toReviewResponse(attempt, questions, outcome, answers);
    }

    @Transactional
    public QuizTakeResponse review(UUID quizId, UUID attemptId, QuizReviewRequest request) {
        QuizAttempt attempt = requireAttempt(quizId, attemptId);
        if (attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("El alumno aún no ha entregado esta prueba de evaluación.");
        }
        if (attempt.getStatus() == QuizAttemptStatus.GRADED) {
            throw new IllegalArgumentException("Esta prueba de evaluación ya está calificada.");
        }
        List<QuizQuestion> questions = quizSnapshot.questionsOf(attempt.getQuizSnapshot());
        Map<UUID, QuizQuestion> byId = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity(), (a, b) -> a));
        List<QuizAnswer> answers = attemptRepository.findAnswers(attempt.getId());
        Map<UUID, QuizAnswer> answerByQ = answers.stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity(), (a, b) -> a));

        if (request != null && request.answers() != null) {
            for (var item : request.answers()) {
                if (item.questionId() == null) continue;
                QuizQuestion q = byId.get(item.questionId());
                if (q == null || q.getKind() != QuestionKind.FREE_TEXT) continue;
                QuizAnswer stored = answerByQ.get(item.questionId());
                if (stored == null) {
                    stored = new QuizAnswer();
                    stored.setQuestionId(item.questionId());
                    answers.add(stored);
                    answerByQ.put(item.questionId(), stored);
                }
                if (item.formatted() != null) {
                    FormattedTextSegment.validate(item.formatted());
                    assertUnchangedWording(item.formatted(), stored.getAnswerText());
                    stored.setAnswerText(FormattedTextSegment.toJson(item.formatted()));
                }
                if (item.teacherScorePercent() != null) {
                    int pct = item.teacherScorePercent();
                    if (pct < 0 || pct > 100) {
                        throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100.");
                    }
                    stored.setTeacherPercent(pct);
                    stored.setScore(com.kuky.backend.learning.service.HomeworkCompositionSupport.scoreAsDecimal(
                            com.kuky.backend.learning.service.HomeworkCompositionSupport.teacherPercentAsScore(pct)));
                }
            }
        }

        boolean finalize = request != null && Boolean.TRUE.equals(request.finalizeGrade());
        if (finalize) {
            for (QuizQuestion q : questions) {
                if (q.getKind() != QuestionKind.FREE_TEXT) continue;
                QuizAnswer a = answerByQ.get(q.getId());
                if (a == null || a.getTeacherPercent() == null) {
                    throw new IllegalArgumentException("Califica todas las preguntas de texto libre antes de finalizar.");
                }
            }
        }

        if (request != null && request.feedbackText() != null) {
            String note = request.feedbackText().strip();
            if (note.length() > 500) {
                throw new IllegalArgumentException("La nota no puede superar 500 caracteres.");
            }
            attempt.setFeedback(note.isBlank() ? null : note);
        }

        attemptRepository.replaceAnswers(attempt.getId(), answers);
        QuizGradingService.GradeOutcome outcome = gradingService.summarize(questions, answers, !finalize);
        if (finalize) {
            attempt.setStatus(QuizAttemptStatus.GRADED);
            attempt.setScorePercent(outcome.scorePercent());
            attempt.setFullyCorrectCount(outcome.fullyCorrectCount());
            attempt.setQuestionUnitCount(outcome.questionUnitCount());
        } else if (attempt.getStatus() != QuizAttemptStatus.GRADED) {
            attempt.setStatus(QuizAttemptStatus.SUBMITTED);
            attempt.setScorePercent(outcome.questionUnitCount() == 0 ? null : outcome.scorePercent());
        } else {
            attempt.setScorePercent(outcome.scorePercent());
            attempt.setFullyCorrectCount(outcome.fullyCorrectCount());
            attempt.setQuestionUnitCount(outcome.questionUnitCount());
        }
        attemptRepository.updateAfterSubmit(attempt);
        QuizGradingService.GradeOutcome view = gradingService.summarize(
                questions, answers, attempt.getStatus() == QuizAttemptStatus.SUBMITTED);
        return toReviewResponse(attempt, questions, view, answers);
    }

    public List<StudentQuizSummary> listForStudent(UUID studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));
        return attemptRepository.findByUser(user.getId()).stream()
                .filter(a -> a.getStatus() != QuizAttemptStatus.IN_PROGRESS)
                .map(a -> {
                    Quiz quiz = quizRepository.findById(a.getQuizId()).orElse(null);
                    List<QuizQuestion> questions = quizSnapshot.questionsOf(a.getQuizSnapshot());
                    List<QuizAnswer> answers = attemptRepository.findAnswers(a.getId());
                    boolean autoOnly = a.getStatus() == QuizAttemptStatus.SUBMITTED;
                    List<QuizSkillScoreDto> skills = gradingService.summarize(questions, answers, autoOnly).skills();
                    return new StudentQuizSummary(
                            a.getQuizId(),
                            a.getId(),
                            quiz == null ? "" : quiz.getTitle(),
                            a.getStatus().name(),
                            a.getScorePercent(),
                            a.getSubmittedAt(),
                            skills,
                            a.getTeacherSeenAt() == null);
                })
                .toList();
    }

    private QuizTakeResponse toReviewResponse(
            QuizAttempt attempt,
            List<QuizQuestion> questions,
            QuizGradingService.GradeOutcome outcome,
            List<QuizAnswer> answers) {
        return new QuizTakeResponse(
                attempt.getQuizId(),
                quizSnapshot.parse(attempt.getQuizSnapshot()).title(),
                quizSnapshot.parse(attempt.getQuizSnapshot()).description(),
                attempt.getStatus().name(),
                gradingService.studentQuestions(questions, true),
                attempt.getScorePercent(),
                attempt.getFullyCorrectCount(),
                attempt.getQuestionUnitCount(),
                outcome.skills(),
                outcome.questionResults(),
                attempt.getFeedback());
    }

    private List<QuizQuestion> mapQuestions(List<QuizQuestionDto> dtos) {
        List<QuizQuestionDto> incoming = dtos == null ? List.of() : dtos;
        if (incoming.isEmpty()) return List.of();

        List<HomeworkQuestionDto> hwDtos = new ArrayList<>();
        List<QuizSkill> skills = new ArrayList<>();
        List<MediaSourceKind> mediaKinds = new ArrayList<>();
        List<String> audioUrls = new ArrayList<>();
        List<UUID> audioFileIds = new ArrayList<>();

        for (QuizQuestionDto q : incoming) {
            if (q.skill() == null || q.skill().isBlank()) {
                throw new IllegalArgumentException("Cada pregunta necesita una destreza.");
            }
            QuizSkill skill;
            try {
                skill = QuizSkill.valueOf(q.skill().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Destreza no válida.");
            }
            skills.add(skill);
            MediaSourceKind mediaKind = parseMedia(q.mediaSourceKind());
            mediaKinds.add(mediaKind);
            audioUrls.add(q.audioUrl());
            audioFileIds.add(q.audioFileId());
            hwDtos.add(new HomeworkQuestionDto(q.id(), q.kind(), q.prompt(), q.options(), q.structure()));
        }

        List<HomeworkQuestion> mapped = homeworkAdminService.validateAndMapQuestions(false, hwDtos);
        List<QuizQuestion> out = new ArrayList<>();
        for (int i = 0; i < mapped.size(); i++) {
            HomeworkQuestion h = mapped.get(i);
            QuizQuestion q = new QuizQuestion();
            q.setId(h.getId());
            q.setSkill(skills.get(i));
            q.setKind(h.getKind());
            q.setPrompt(h.getPrompt());
            q.setStructureJson(h.getStructureJson());
            q.setOptions(h.getOptions());
            q.setMediaSourceKind(mediaKinds.get(i));
            q.setAudioUrl(blankToNull(audioUrls.get(i)));
            q.setAudioFileId(audioFileIds.get(i));
            if (q.getSkill() == QuizSkill.LISTENING
                    && !ListeningMedia.isComplete(q.getMediaSourceKind(), q.getAudioUrl(), q.getAudioFileId())) {
                throw new IllegalArgumentException("Las preguntas de comprensión oral necesitan un audio o vídeo.");
            }
            if (q.getSkill() != QuizSkill.LISTENING) {
                q.setMediaSourceKind(null);
                q.setAudioUrl(null);
                q.setAudioFileId(null);
            }
            out.add(q);
        }
        return out;
    }

    private static MediaSourceKind parseMedia(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return MediaSourceKind.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Fuente de audio no válida.");
        }
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

    private QuizAdminDetail toDetail(Quiz quiz) {
        List<QuizQuestion> questions = questionRepository.findLiveByQuiz(quiz.getId());
        List<QuizAssigneeDto> assignees = assigneeRepository.findAssignees(quiz.getId()).stream()
                .map(a -> new QuizAssigneeDto(a.userId(), displayName(a.firstName(), a.lastName(), a.username(), a.email()), a.email()))
                .toList();
        return new QuizAdminDetail(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                questions.stream().map(this::toQuestionDto).toList(),
                assignees);
    }

    private QuizQuestionDto toQuestionDto(QuizQuestion q) {
        List<HomeworkQuestionDto.OptionDto> options = q.getOptions().stream()
                .map(o -> new HomeworkQuestionDto.OptionDto(o.getId(), o.getLabel(), o.isCorrect()))
                .toList();
        return new QuizQuestionDto(
                q.getId(),
                q.getSkill().name(),
                q.getKind().name(),
                q.getPrompt(),
                options,
                readStructure(q.getStructureJson()),
                q.getMediaSourceKind() == null ? null : q.getMediaSourceKind().name(),
                q.getAudioUrl(),
                q.getAudioFileId());
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

    private Quiz requireQuiz(UUID id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new QuizNotFoundException("Prueba de evaluación no encontrada."));
    }

    private QuizAttempt requireAttempt(UUID quizId, UUID attemptId) {
        requireQuiz(quizId);
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new QuizNotFoundException("Entrega no encontrada."));
        if (!quizId.equals(attempt.getQuizId())) {
            throw new QuizNotFoundException("Entrega no encontrada.");
        }
        return attempt;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.strip();
    }

    private static String displayName(User u) {
        if (u == null) return "";
        return displayName(u.getFirstName(), u.getLastName(), u.getUsername(), u.getEmail());
    }

    private static String displayName(String first, String last, String username, String email) {
        String combined = ((first == null ? "" : first) + " " + (last == null ? "" : last)).strip();
        if (!combined.isBlank()) return combined;
        if (username != null && !username.isBlank()) return username;
        return email == null ? "" : email;
    }

    private static void assertUnchangedWording(List<FormattedTextSegment> incoming, String stored) {
        if (!FormattedTextSegment.plainText(incoming)
                .equals(FormattedTextSegment.storedPlainWording(stored))) {
            throw new IllegalArgumentException("No puedes cambiar el texto del alumno, solo el formato.");
        }
    }
}
