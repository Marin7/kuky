package com.kuky.backend.units.service;

import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.quiz.exception.QuizNotFoundException;
import com.kuky.backend.quiz.repository.QuizAssigneeRepository;
import com.kuky.backend.quiz.repository.QuizQuestionRepository;
import com.kuky.backend.quiz.repository.QuizRepository;
import com.kuky.backend.units.dto.*;
import com.kuky.backend.units.exception.UnitNotFoundException;
import com.kuky.backend.units.model.Unit;
import com.kuky.backend.units.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Units: a PDF with page activities, optionally followed by quizzes, assigned to students.
 * Assigning a unit assigns its quizzes; homework is not part of a unit.
 */
@Service
@Transactional
public class UnitService {

    private static final Set<String> VALID_LEVELS = Set.of("A1", "A2", "B1", "B2", "C1", "C2");

    private final UnitRepository repository;
    private final UserRepository userRepository;
    private final PresentationRepository presentationRepository;
    private final UnitPdfService unitPdfService;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAssigneeRepository quizAssigneeRepository;

    public UnitService(UnitRepository repository,
                       UserRepository userRepository,
                       PresentationRepository presentationRepository,
                       UnitPdfService unitPdfService,
                       QuizRepository quizRepository,
                       QuizQuestionRepository quizQuestionRepository,
                       QuizAssigneeRepository quizAssigneeRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.presentationRepository = presentationRepository;
        this.unitPdfService = unitPdfService;
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAssigneeRepository = quizAssigneeRepository;
    }

    public List<UnitSummary> list() {
        return repository.listSummaries();
    }

    public UnitDetail create(String level, String subject) {
        String validLevel = validateLevel(level);
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("El nombre del tema no puede estar vacío.");
        }
        UUID id = repository.create(validLevel, subject.trim());
        return detail(id);
    }

    public UnitDetail get(UUID id) {
        return detail(id);
    }

    public UnitDetail update(UUID id, String level, String subject) {
        requireUnit(id);
        String validLevel = validateLevel(level);
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("El nombre del tema no puede estar vacío.");
        }
        repository.updateLevelSubject(id, validLevel, subject.trim());
        presentationRepository.updateOwnedTitleLevel(id, subject.trim(), validLevel);
        return detail(id);
    }

    /** Cascades to the unit's PDF, its activities and their submissions. */
    public void delete(UUID id) {
        unitPdfService.deleteFileAfterCommit(id);
        if (repository.delete(id) == 0) {
            throw new UnitNotFoundException("Unidad no encontrada.");
        }
    }

    public List<UnitSummary> reorder(String level, List<UUID> orderedIds) {
        validateLevel(level);
        List<UUID> existing = repository.findIdsByLevel(level);
        if (orderedIds.size() != existing.size() || !new HashSet<>(existing).containsAll(orderedIds)) {
            throw new IllegalArgumentException("La lista de unidades no es una permutación válida para el nivel " + level + ".");
        }
        repository.reorder(level, orderedIds);
        return repository.listSummaries().stream()
                .filter(u -> u.level().equals(level))
                .toList();
    }

    /**
     * Replaces unit assignees: assigned students can open the unit's PDF, do its activities and
     * take its quizzes. Removed students lose the unit's quizzes they have not started yet.
     */
    public UnitDetail setAssignees(UUID id, List<UUID> studentIds) {
        requireUnit(id);
        List<UUID> next = studentIds == null ? List.of() : studentIds;
        validateStudents(next);

        Set<UUID> previous = new HashSet<>(repository.findAssigneeIds(id));
        Set<UUID> nextSet = new HashSet<>(next);
        repository.replaceAssignees(id, next);

        List<UUID> added = next.stream().filter(s -> !previous.contains(s)).toList();
        List<UUID> removed = previous.stream().filter(s -> !nextSet.contains(s)).toList();
        for (UnitRepository.UnitQuiz quiz : repository.findQuizzes(id)) {
            quizAssigneeRepository.addUnitAssignees(quiz.id(), added);
            quizAssigneeRepository.removeUnstartedAssignees(quiz.id(), removed);
        }
        return detail(id);
    }

    /**
     * Sets the quizzes shown at the end of the unit, in order. A quiz belongs to at most one
     * unit, so linking it here moves it out of its previous unit. The unit's students get every
     * linked quiz; a quiz that leaves the unit is unassigned from them unless they started it.
     */
    public UnitDetail setQuizzes(UUID id, List<UUID> quizIds) {
        requireUnit(id);
        List<UUID> desired = List.copyOf(new LinkedHashSet<>(quizIds == null ? List.of() : quizIds));
        for (UUID quizId : desired) {
            quizRepository.findById(quizId)
                    .orElseThrow(() -> new QuizNotFoundException("Prueba de evaluación no encontrada."));
            if (quizQuestionRepository.countLive(quizId) < 1) {
                throw new IllegalArgumentException(
                        "La prueba de evaluación necesita al menos una pregunta antes de añadirla a una unidad.");
            }
        }

        List<UUID> assignees = repository.findAssigneeIds(id);
        Set<UUID> previous = new HashSet<>(repository.findQuizzes(id).stream().map(UnitRepository.UnitQuiz::id).toList());
        for (UUID quizId : previous) {
            if (!desired.contains(quizId)) {
                quizAssigneeRepository.removeUnstartedAssignees(quizId, assignees);
            }
        }
        for (UUID quizId : desired) {
            if (previous.contains(quizId)) continue;
            repository.findUnitIdForQuiz(quizId)
                    .filter(prior -> !prior.equals(id))
                    .ifPresent(prior -> quizAssigneeRepository.removeUnstartedAssignees(
                            quizId, repository.findAssigneeIds(prior)));
        }

        repository.setQuizzes(id, desired);
        for (UUID quizId : desired) {
            quizAssigneeRepository.addUnitAssignees(quizId, assignees);
        }
        return detail(id);
    }

    /** Renames one page of the unit's PDF (every page keeps a non-blank title). */
    public UnitDetail setPageTitle(UUID id, int page, String title) {
        requireUnit(id);
        PresentationRepository.OwnedPdf pdf = presentationRepository.findOwnedByUnit(id)
                .filter(p -> p.fileId() != null && p.pageCount() != null)
                .orElseThrow(() -> new IllegalArgumentException("La unidad no tiene PDF."));
        String trimmed = title == null ? "" : title.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Cada página necesita un título.");
        }
        if (trimmed.length() > 200) {
            throw new IllegalArgumentException("El título no puede superar los 200 caracteres.");
        }
        if (page < 1 || page > pdf.pageCount()
                || presentationRepository.setPageTitle(pdf.presentationId(), page, trimmed) == 0) {
            throw new IllegalArgumentException("La página debe estar entre 1 y " + pdf.pageCount() + ".");
        }
        return detail(id);
    }

    // --- helpers -------------------------------------------------------------

    private Unit requireUnit(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new UnitNotFoundException("Unidad no encontrada."));
    }

    private UnitDetail detail(UUID id) {
        Unit u = requireUnit(id);
        UnitPdfInfo pdf = presentationRepository.findOwnedByUnit(id)
                .filter(p -> p.fileId() != null && p.pageCount() != null)
                .map(p -> new UnitPdfInfo(p.presentationId(), p.fileId(), p.originalName(),
                        p.byteSize() == null ? 0 : p.byteSize(), p.pageCount(),
                        presentationRepository.findPageTitles(p.presentationId())))
                .orElse(null);

        List<UnitActivitySummary> activities = repository.findActivityCounts(id).stream()
                .map(a -> new UnitActivitySummary(
                        a.id(), a.page(), a.format(), compositionOfFormat(a.format()),
                        a.submitted(), a.awaitingCorrection(), a.graded(), a.hasUnseen()))
                .toList();

        return new UnitDetail(
                u.getId(),
                u.getLevel(),
                u.getSubject(),
                u.getPosition(),
                pdf,
                activities,
                repository.findQuizzes(id).stream()
                        .map(q -> new UnitQuizItem(q.id(), q.title(), q.questionCount()))
                        .toList(),
                repository.findAssignedStudents(id));
    }

    private static String compositionOfFormat(String format) {
        return switch (format) {
            case "MIXED" -> "MIXED";
            case "EXERCISE" -> "ALL_AUTO";
            default -> "ALL_MANUAL";
        };
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

    private static String validateLevel(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("El nivel es obligatorio.");
        }
        String upper = raw.toUpperCase(Locale.ROOT);
        if (!VALID_LEVELS.contains(upper)) {
            throw new IllegalArgumentException("Nivel inválido: " + raw + ". Debe ser uno de A1, A2, B1, B2, C1, C2.");
        }
        return upper;
    }
}
