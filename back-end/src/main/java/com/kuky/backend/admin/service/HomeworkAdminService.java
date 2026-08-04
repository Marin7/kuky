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
import com.kuky.backend.admin.dto.UpdateHomeworkRequest;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.exception.AlreadyReviewedException;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.NotSubmittedException;
import com.kuky.backend.learning.exception.SubmissionNotFoundException;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkLevel;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.QuestionKind;
import com.kuky.backend.learning.model.QuestionOption;
import com.kuky.backend.learning.repository.AudioFileRepository;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.learning.repository.HomeworkQuestionRepository;
import com.kuky.backend.learning.repository.HomeworkSubmissionRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.learning.service.BlankPassageParser;
import com.kuky.backend.learning.service.ExerciseGradingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Teacher-side homework authoring + assignment + submission review. */
@Service
@Transactional
public class HomeworkAdminService {

    private final ContentRepository contentRepository;
    private final HomeworkTargetRepository targetRepository;
    private final HomeworkQuestionRepository questionRepository;
    private final AudioFileRepository audioFileRepository;
    private final UserRepository userRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final ExerciseGradingService exerciseGradingService;
    private final ObjectMapper objectMapper;

    public HomeworkAdminService(ContentRepository contentRepository,
                                HomeworkTargetRepository targetRepository,
                                HomeworkQuestionRepository questionRepository,
                                AudioFileRepository audioFileRepository,
                                UserRepository userRepository,
                                HomeworkSubmissionRepository submissionRepository,
                                ExerciseGradingService exerciseGradingService,
                                ObjectMapper objectMapper) {
        this.contentRepository = contentRepository;
        this.targetRepository = targetRepository;
        this.questionRepository = questionRepository;
        this.audioFileRepository = audioFileRepository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.exerciseGradingService = exerciseGradingService;
        this.objectMapper = objectMapper;
    }

    // --- Teacher review of MANUAL submissions --------------------------------

    public List<HomeworkReviewQueueItemDto> getReviewQueue() {
        return submissionRepository.findSubmittedManualQueue().stream()
                .map(r -> new HomeworkReviewQueueItemDto(
                        r.submissionId(), r.studentId(), r.studentEmail(), r.studentFirstName(),
                        r.studentLastName(), r.studentUsername(), r.assignmentTitle(), r.submittedAt()))
                .toList();
    }

    public HomeworkSubmissionAdminDto getSubmissionDetail(UUID submissionId) {
        var row = submissionRepository.findDetailById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        return toSubmissionAdminDto(row);
    }

    /**
     * Graded exercise detail for the teacher: questions + the student's answers
     * and automatic score breakdown. Only valid for {@code GRADED} exercise submissions.
     */
    public ExerciseSubmissionResultAdminDto getExerciseResult(UUID submissionId) {
        HomeworkSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        if (!HomeworkStatus.GRADED.name().equals(submission.getStatus())) {
            throw new NotSubmittedException("Esta entrega todavía no ha sido calificada automáticamente.");
        }
        HomeworkAssignment assignment = requireAssignment(submission.getAssignmentId());
        if (assignment.getFormat() != HomeworkFormat.EXERCISE) {
            throw new AssignmentNotFoundException("Esta entrega no es un ejercicio auto-corregible.");
        }
        User student = userRepository.findById(submission.getUserId())
                .orElseThrow(() -> new StudentNotFoundException("Alumno no encontrado."));
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
                view.result());
    }

    /**
     * Saves the teacher's formatted feedback and transitions the submission to
     * REVIEWED. Feedback goes through the same {@link FormattedTextSegment}
     * validator used by the student-submit path — an over-length or malformed
     * feedback array is rejected exactly like an over-length answer.
     */
    public HomeworkSubmissionAdminDto saveFeedback(UUID submissionId, List<FormattedTextSegment> feedback) {
        FormattedTextSegment.validate(feedback);
        var row = submissionRepository.findDetailById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Entrega no encontrada."));
        if (HomeworkStatus.REVIEWED.name().equals(row.status())) {
            throw new AlreadyReviewedException("Esta entrega ya ha sido revisada.");
        }
        if (!HomeworkStatus.SUBMITTED.name().equals(row.status())) {
            throw new NotSubmittedException("Esta entrega todavía no ha sido enviada por el alumno.");
        }
        submissionRepository.updateFeedback(submissionId, FormattedTextSegment.toJson(feedback), Instant.now());
        var updated = submissionRepository.findDetailById(submissionId).orElseThrow();
        return toSubmissionAdminDto(updated);
    }

    private HomeworkSubmissionAdminDto toSubmissionAdminDto(HomeworkSubmissionRepository.SubmissionDetailRow row) {
        return new HomeworkSubmissionAdminDto(
                row.submissionId(), row.studentId(), row.studentEmail(), row.studentFirstName(),
                row.studentLastName(), row.studentUsername(), row.assignmentTitle(), row.status(),
                FormattedTextSegment.fromJson(row.responseText()),
                FormattedTextSegment.fromJson(row.feedback()),
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
        HomeworkFormat format = parseFormat(req.format());
        validateTypeFormat(type, format);
        List<HomeworkQuestion> questions = validateAndMapQuestions(format, req.questions());
        Audio audio = resolveAudio(type, req.audioUrl(), req.audioFileId());

        UUID id = contentRepository.insertAssignment(req.title(), req.instructions(), req.dueOn(), type, level, format,
                audio.url(), audio.fileId());
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
        validateTypeFormat(type, format);
        List<HomeworkQuestion> questions = validateAndMapQuestions(format, req.questions());
        Audio audio = resolveAudio(type, req.audioUrl(), req.audioFileId());

        contentRepository.updateAssignment(id, req.title(), req.instructions(), req.dueOn(), type, level, format,
                audio.url(), audio.fileId());
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
     * Writing homework is always reviewed by the teacher; it can never be an
     * auto-graded exercise. Throws {@link IllegalArgumentException}
     * (→ VALIDATION_ERROR) on violation.
     */
    private static void validateTypeFormat(HomeworkType type, HomeworkFormat format) {
        if (type == HomeworkType.WRITE && format == HomeworkFormat.EXERCISE) {
            throw new IllegalArgumentException("Las tareas de escritura no pueden ser autocorregibles.");
        }
    }

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

            HomeworkQuestion model = new HomeworkQuestion();
            model.setKind(kind);
            model.setPrompt(q.prompt().strip());

            if (kind.isStructured()) {
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
                    om.setLabel(o.label().strip());
                    // Fill-blank accepted answers are always part of the key.
                    om.setCorrect(kind == QuestionKind.FILL_BLANK || o.correct());
                    optionModels.add(om);
                }
                model.setOptions(optionModels);
            }
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
        if (blankCount < 2 || blankCount > 20) {
            throw new IllegalArgumentException("El enunciado debe tener entre 2 y 20 huecos (___).");
        }
        JsonNode blanksNode = structure.get("blanks");
        if (blanksNode == null || !blanksNode.isArray() || blanksNode.size() != blankCount) {
            throw new IllegalArgumentException("El número de respuestas no coincide con el número de huecos del enunciado.");
        }
        ArrayNode blanks = objectMapper.createArrayNode();
        for (JsonNode blankNode : blanksNode) {
            ObjectNode entry = objectMapper.createObjectNode();
            entry.set("acceptedAnswers", normalizeAnswerList(blankNode == null ? null : blankNode.get("acceptedAnswers")));
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
        if (bankNode == null || !bankNode.isArray() || bankNode.size() != blankCount) {
            throw new IllegalArgumentException("El banco de palabras debe tener el mismo número de elementos que huecos.");
        }
        ArrayNode bank = objectMapper.createArrayNode();
        Set<String> seenIds = new HashSet<>();
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
        }
        return objectMapper.createObjectNode().set("bank", bank);
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

    /** Non-empty (after trim) accepted-answer list, normalized to trimmed strings. */
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

    private record Audio(String url, UUID fileId) {}

    /**
     * Resolves the listening-audio source. Audio is only kept for {@code AUDIO}
     * homework; for any other type both fields are cleared so a type change does
     * not leave a dangling source. A blank URL becomes null, and an uploaded file
     * id is verified to exist (→ VALIDATION_ERROR otherwise).
     */
    private Audio resolveAudio(HomeworkType type, String rawUrl, UUID fileId) {
        if (type != HomeworkType.AUDIO) {
            return new Audio(null, null);
        }
        String url = rawUrl == null || rawUrl.isBlank() ? null : rawUrl.strip();
        if (fileId != null && audioFileRepository.findOriginalName(fileId).isEmpty()) {
            throw new IllegalArgumentException("El audio subido no existe.");
        }
        return new Audio(url, fileId);
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
                        v.status(), v.responseText(), v.submittedAt(), v.scorePercent(), v.submissionId()))
                .toList();
        String type = a.getHomeworkType() == null ? null : a.getHomeworkType().name();
        String level = a.getLevel() == null ? null : a.getLevel().name();
        String format = a.getFormat() == null ? HomeworkFormat.MANUAL.name() : a.getFormat().name();

        List<HomeworkQuestionDto> questions = a.getFormat() == HomeworkFormat.EXERCISE
                ? questionRepository.findByAssignment(a.getId()).stream().map(this::toQuestionDto).toList()
                : List.of();

        String audioFileName = a.getAudioFileId() == null
                ? null
                : audioFileRepository.findOriginalName(a.getAudioFileId()).orElse(null);

        return new HomeworkAdminItem(a.getId(), a.getTitle(), a.getInstructions(), a.getDueOn(),
                type, level, format, questions, a.getAudioUrl(), a.getAudioFileId(), audioFileName, assignees);
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
