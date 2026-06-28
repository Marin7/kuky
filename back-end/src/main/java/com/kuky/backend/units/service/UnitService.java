package com.kuky.backend.units.service;

import com.kuky.backend.admin.dto.StudentResponse;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.units.dto.*;
import com.kuky.backend.units.exception.UnitNotFoundException;
import com.kuky.backend.units.model.Unit;
import com.kuky.backend.units.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UnitService {

    private static final Set<String> VALID_LEVELS = Set.of("A1", "A2", "B1", "B2", "C1", "C2");

    private final UnitRepository repository;
    private final UserRepository userRepository;

    public UnitService(UnitRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
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

    public UnitDetail setPresentations(UUID id, List<UUID> presentationIds) {
        requireUnit(id);
        repository.setPresentations(id, presentationIds);
        return detail(id);
    }

    public UnitDetail setHomeworks(UUID id, List<UUID> homeworkIds) {
        requireUnit(id);
        repository.setHomeworks(id, homeworkIds);
        return detail(id);
    }

    public UnitDetail setAssignees(UUID id, List<UUID> studentIds) {
        requireUnit(id);
        validateStudents(studentIds);
        repository.replaceAssignees(id, studentIds);
        return detail(id);
    }

    // --- helpers -------------------------------------------------------------

    private Unit requireUnit(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new UnitNotFoundException("Unidad no encontrada."));
    }

    private UnitDetail detail(UUID id) {
        Unit u = requireUnit(id);
        return new UnitDetail(
                u.getId(),
                u.getLevel(),
                u.getSubject(),
                u.getPosition(),
                repository.findPresentations(id),
                repository.findHomeworks(id),
                repository.findAssignedStudents(id));
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
        String upper = raw.toUpperCase(java.util.Locale.ROOT);
        if (!VALID_LEVELS.contains(upper)) {
            throw new IllegalArgumentException("Nivel inválido: " + raw + ". Debe ser uno de A1, A2, B1, B2, C1, C2.");
        }
        return upper;
    }
}
