package com.kuky.backend.admin.service;

import com.kuky.backend.admin.dto.AssigneeDto;
import com.kuky.backend.admin.dto.CreateHomeworkRequest;
import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.HomeworkQuestionDto;
import com.kuky.backend.admin.dto.UpdateHomeworkRequest;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkLevel;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Teacher-side homework authoring + assignment + submission review. */
@Service
@Transactional
public class HomeworkAdminService {

    private final ContentRepository contentRepository;
    private final HomeworkTargetRepository targetRepository;
    private final HomeworkQuestionRepository questionRepository;
    private final UserRepository userRepository;

    public HomeworkAdminService(ContentRepository contentRepository,
                                HomeworkTargetRepository targetRepository,
                                HomeworkQuestionRepository questionRepository,
                                UserRepository userRepository) {
        this.contentRepository = contentRepository;
        this.targetRepository = targetRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
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
        HomeworkFormat format = parseFormat(req.format());
        List<HomeworkQuestion> questions = validateAndMapQuestions(format, req.questions());

        UUID id = contentRepository.insertAssignment(req.title(), req.instructions(), req.dueOn(), type, level, format);
        questionRepository.replaceQuestions(id, questions);
        if (!assignees.isEmpty()) {
            targetRepository.replaceTargets(id, assignees);
        }
        return toItem(requireAssignment(id));
    }

    public HomeworkAdminItem update(UUID id, UpdateHomeworkRequest req) {
        requireAssignment(id);
        HomeworkType type = parseType(req.homeworkType());
        HomeworkLevel level = parseLevel(req.level());
        HomeworkFormat format = parseFormat(req.format());
        List<HomeworkQuestion> questions = validateAndMapQuestions(format, req.questions());

        contentRepository.updateAssignment(id, req.title(), req.instructions(), req.dueOn(), type, level, format);
        // Full replace of questions (preserves existing GRADED submissions — they are not re-graded).
        questionRepository.replaceQuestions(id, questions);
        return toItem(requireAssignment(id));
    }

    public HomeworkAdminItem setAssignees(UUID id, List<UUID> assigneeIds) {
        requireAssignment(id);
        validateStudents(assigneeIds);
        targetRepository.replaceTargets(id, assigneeIds);
        return toItem(requireAssignment(id));
    }

    public void delete(UUID id) {
        if (contentRepository.deleteAssignment(id) == 0) {
            throw new AssignmentNotFoundException("Tarea no encontrada.");
        }
    }

    // --- exercise validation + mapping --------------------------------------

    /**
     * Validates the authored questions against the format rules and maps the
     * teacher DTOs to persistence models. Throws {@link IllegalArgumentException}
     * (→ VALIDATION_ERROR) on any rule violation.
     */
    private List<HomeworkQuestion> validateAndMapQuestions(HomeworkFormat format, List<HomeworkQuestionDto> dtos) {
        List<HomeworkQuestionDto> questions = dtos == null ? List.of() : dtos;

        if (format == HomeworkFormat.MANUAL) {
            if (!questions.isEmpty()) {
                throw new IllegalArgumentException("Una tarea manual no puede tener preguntas.");
            }
            return List.of();
        }

        // EXERCISE
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Un ejercicio autocorregible necesita al menos una pregunta.");
        }
        List<HomeworkQuestion> mapped = new ArrayList<>();
        for (HomeworkQuestionDto q : questions) {
            if (q.prompt() == null || q.prompt().isBlank()) {
                throw new IllegalArgumentException("Cada pregunta necesita un enunciado.");
            }
            QuestionKind kind = parseKind(q.kind());
            List<HomeworkQuestionDto.OptionDto> opts = q.options() == null ? List.of() : q.options();
            validateOptions(kind, opts);

            HomeworkQuestion model = new HomeworkQuestion();
            model.setKind(kind);
            model.setPrompt(q.prompt().strip());
            List<QuestionOption> optionModels = new ArrayList<>();
            for (HomeworkQuestionDto.OptionDto o : opts) {
                if (o.label() == null || o.label().isBlank()) {
                    throw new IllegalArgumentException("Las opciones y respuestas no pueden estar vacías.");
                }
                QuestionOption om = new QuestionOption();
                om.setLabel(o.label().strip());
                // Fill-blank accepted answers are always part of the key.
                om.setCorrect(kind == QuestionKind.FILL_BLANK || o.correct());
                optionModels.add(om);
            }
            model.setOptions(optionModels);
            mapped.add(model);
        }
        return mapped;
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
            case FILL_BLANK -> {
                if (opts.isEmpty()) {
                    throw new IllegalArgumentException("Una pregunta de rellenar el hueco necesita al menos una respuesta aceptada.");
                }
            }
        }
    }

    // --- helpers -------------------------------------------------------------

    private HomeworkAssignment requireAssignment(UUID id) {
        return contentRepository.findAssignmentById(id)
                .orElseThrow(() -> new AssignmentNotFoundException("Tarea no encontrada."));
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
                        v.status(), v.responseText(), v.submittedAt(), v.scorePercent()))
                .toList();
        String type = a.getHomeworkType() == null ? null : a.getHomeworkType().name();
        String level = a.getLevel() == null ? null : a.getLevel().name();
        String format = a.getFormat() == null ? HomeworkFormat.MANUAL.name() : a.getFormat().name();

        List<HomeworkQuestionDto> questions = a.getFormat() == HomeworkFormat.EXERCISE
                ? questionRepository.findByAssignment(a.getId()).stream().map(this::toQuestionDto).toList()
                : List.of();

        return new HomeworkAdminItem(a.getId(), a.getTitle(), a.getInstructions(), a.getDueOn(),
                type, level, format, questions, assignees);
    }

    private HomeworkQuestionDto toQuestionDto(HomeworkQuestion q) {
        List<HomeworkQuestionDto.OptionDto> options = q.getOptions().stream()
                .map(o -> new HomeworkQuestionDto.OptionDto(o.getId(), o.getLabel(), o.isCorrect()))
                .toList();
        return new HomeworkQuestionDto(q.getId(), q.getKind().name(), q.getPrompt(), options);
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
