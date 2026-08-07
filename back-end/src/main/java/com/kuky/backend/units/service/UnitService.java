package com.kuky.backend.units.service;

import com.kuky.backend.admin.dto.AssigneeDto;
import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.PresentationFileSummary;
import com.kuky.backend.admin.dto.PresentationSummary;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.repository.HomeworkTargetRepository;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.units.dto.*;
import com.kuky.backend.units.exception.InvalidContentOrderException;
import com.kuky.backend.units.exception.UnitNotFoundException;
import com.kuky.backend.units.model.Unit;
import com.kuky.backend.units.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class UnitService {

    private static final Set<String> VALID_LEVELS = Set.of("A1", "A2", "B1", "B2", "C1", "C2");

    private final UnitRepository repository;
    private final UserRepository userRepository;
    private final PresentationRepository presentationRepository;
    private final HomeworkTargetRepository targetRepository;

    public UnitService(UnitRepository repository,
                       UserRepository userRepository,
                       PresentationRepository presentationRepository,
                       HomeworkTargetRepository targetRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.presentationRepository = presentationRepository;
        this.targetRepository = targetRepository;
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
        return detail(id);
    }

    public void delete(UUID id) {
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

    public UnitDetail reorderContents(UUID id, List<UnitContentRef> items) {
        requireUnit(id);
        List<UnitContentRef> normalized = items == null ? List.of() : items;
        List<UnitRepository.ContentMember> current = repository.findContentMembers(id);

        if (normalized.size() != current.size()) {
            throw new InvalidContentOrderException(
                    "La lista de contenidos no es una permutación válida de la unidad.");
        }

        Set<String> currentKeys = current.stream()
                .map(m -> key(m.type(), m.id()))
                .collect(Collectors.toSet());
        Set<String> requestKeys = new HashSet<>();
        List<UnitContentRef> cleaned = new ArrayList<>();
        for (UnitContentRef ref : normalized) {
            String type = normalizeType(ref.type());
            String k = key(type, ref.id());
            if (!requestKeys.add(k) || !currentKeys.contains(k)) {
                throw new InvalidContentOrderException(
                        "La lista de contenidos no es una permutación válida de la unidad.");
            }
            cleaned.add(new UnitContentRef(type, ref.id()));
        }
        if (!requestKeys.equals(currentKeys)) {
            throw new InvalidContentOrderException(
                    "La lista de contenidos no es una permutación válida de la unidad.");
        }

        repository.reorderContents(id, cleaned);
        return detail(id);
    }

    public UnitDetail setPresentations(UUID id, List<UUID> presentationIds) {
        requireUnit(id);
        repository.setPresentations(id, presentationIds);
        return detail(id);
    }

    public UnitDetail setHomeworks(UUID id, List<UUID> homeworkIds) {
        requireUnit(id);
        List<UUID> previousHomeworks = repository.findHomeworkIds(id);
        List<UUID> assignees = repository.findAssigneeIds(id);
        LinkedHashSet<UUID> desired = new LinkedHashSet<>(
                homeworkIds == null ? List.of() : homeworkIds);

        for (UUID hw : previousHomeworks) {
            if (!desired.contains(hw)) {
                targetRepository.removeTargets(hw, assignees);
            }
        }
        for (UUID hw : desired) {
            if (!previousHomeworks.contains(hw)) {
                repository.findUnitIdForHomework(hw).ifPresent(priorUnit -> {
                    if (!priorUnit.equals(id)) {
                        targetRepository.removeTargets(hw, repository.findAssigneeIds(priorUnit));
                    }
                });
            }
        }

        repository.setHomeworks(id, List.copyOf(desired));

        for (UUID hw : repository.findHomeworkIds(id)) {
            targetRepository.addTargets(hw, assignees);
        }
        return detail(id);
    }

    /**
     * Replaces unit assignees. Newly assigned students receive every homework in the
     * unit (via {@code homework_targets}); removed students lose those targets.
     * Presentations remain accessible via {@code unit_assignments} as before.
     */
    public UnitDetail setAssignees(UUID id, List<UUID> studentIds) {
        requireUnit(id);
        List<UUID> next = studentIds == null ? List.of() : studentIds;
        validateStudents(next);

        List<UUID> previous = repository.findAssigneeIds(id);
        repository.replaceAssignees(id, next);

        Set<UUID> prevSet = new HashSet<>(previous);
        Set<UUID> nextSet = new HashSet<>(next);
        List<UUID> added = next.stream().filter(s -> !prevSet.contains(s)).toList();
        List<UUID> removed = previous.stream().filter(s -> !nextSet.contains(s)).toList();
        for (UUID hw : repository.findHomeworkIds(id)) {
            targetRepository.addTargets(hw, added);
            targetRepository.removeTargets(hw, removed);
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
        List<PresentationSummary> presentations = repository.findPresentations(id);
        Map<UUID, List<PresentationFileSummary>> filesByPresentation =
                presentationRepository.listFilesGrouped(
                        presentations.stream().map(PresentationSummary::id).toList());
        Map<UUID, PresentationSummary> presentationsById = presentations.stream()
                .map(p -> new PresentationSummary(
                        p.id(), p.title(), p.level(),
                        filesByPresentation.getOrDefault(p.id(), List.of()),
                        p.sharedWithIds(), p.updatedAt()))
                .collect(Collectors.toMap(PresentationSummary::id, Function.identity()));

        Map<UUID, HomeworkAdminItem> homeworksById = repository.findHomeworks(id).stream()
                .map(this::withAssignees)
                .collect(Collectors.toMap(HomeworkAdminItem::id, Function.identity()));

        List<UnitContentItem> contents = new ArrayList<>();
        for (UnitRepository.ContentMember m : repository.findContentMembers(id)) {
            if (UnitContentItem.PRESENTATION.equals(m.type())) {
                PresentationSummary p = presentationsById.get(m.id());
                if (p != null) {
                    contents.add(new UnitContentItem(
                            UnitContentItem.PRESENTATION, m.unitPosition(), p, null));
                }
            } else if (UnitContentItem.HOMEWORK.equals(m.type())) {
                HomeworkAdminItem h = homeworksById.get(m.id());
                if (h != null) {
                    contents.add(new UnitContentItem(
                            UnitContentItem.HOMEWORK, m.unitPosition(), null, h));
                }
            }
        }

        return new UnitDetail(
                u.getId(),
                u.getLevel(),
                u.getSubject(),
                u.getPosition(),
                contents,
                repository.findAssignedStudents(id));
    }

    private HomeworkAdminItem withAssignees(HomeworkAdminItem h) {
        List<AssigneeDto> assignees = targetRepository.findAssigneesWithSubmissions(h.id()).stream()
                .map(v -> new AssigneeDto(v.userId(), v.email(), v.firstName(), v.lastName(), v.username(),
                        v.status(), v.responseText(), v.submittedAt(), v.scorePercent(), v.submissionId(),
                        v.hasTeacherFeedback()))
                .toList();
        return new HomeworkAdminItem(
                h.id(), h.title(), h.instructions(), h.dueOn(), h.homeworkType(), h.level(), h.format(),
                h.questions(), h.audioUrl(), h.audioFileId(), h.audioFileName(), assignees);
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

    private static String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidContentOrderException("Tipo de contenido inválido.");
        }
        String upper = raw.toUpperCase(Locale.ROOT);
        if (!UnitContentItem.PRESENTATION.equals(upper) && !UnitContentItem.HOMEWORK.equals(upper)) {
            throw new InvalidContentOrderException("Tipo de contenido inválido: " + raw);
        }
        return upper;
    }

    private static String key(String type, UUID id) {
        return type + ":" + Objects.requireNonNull(id);
    }
}
