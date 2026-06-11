package com.kuky.backend.presentations.service;

import com.kuky.backend.admin.dto.*;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.presentations.exception.PresentationNotFoundException;
import com.kuky.backend.presentations.model.Presentation;
import com.kuky.backend.presentations.model.PresentationFile;
import com.kuky.backend.presentations.repository.PresentationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class PresentationService {

    private static final long MAX_FILE_BYTES = 50L * 1024 * 1024; // 50 MB

    private final PresentationRepository repository;
    private final UserRepository userRepository;

    public PresentationService(PresentationRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public List<PresentationSummary> list() {
        return repository.listSummaries().stream()
                .map(s -> new PresentationSummary(
                        s.id(), s.title(), s.level(), s.hasFile(), s.originalFileName(),
                        s.sharedWithIds().stream().map(UUID::toString).toList(),
                        s.updatedAt()))
                .toList();
    }

    public PresentationDetail create(String title) {
        UUID id = repository.create(title);
        return detail(id);
    }

    public PresentationDetail setLevel(UUID id, String level) {
        requirePresentation(id);
        String validated = validateLevel(level);
        repository.updateLevel(id, validated);
        return detail(id);
    }

    public PresentationDetail get(UUID id) {
        return detail(id);
    }

    public PresentationDetail rename(UUID id, String title) {
        requirePresentation(id);
        repository.rename(id, title);
        return detail(id);
    }

    public void delete(UUID id) {
        if (repository.delete(id) == 0) {
            throw new PresentationNotFoundException("Presentación no encontrada.");
        }
    }

    // --- file management -----------------------------------------------------

    public PresentationDetail uploadFile(UUID id, MultipartFile file) {
        requirePresentation(id);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("El archivo no puede superar los 50 MB.");
        }
        String originalName = file.getOriginalFilename();
        boolean isPptx = (originalName != null && originalName.toLowerCase(Locale.ROOT).endsWith(".pptx"))
                || "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                        .equals(file.getContentType());
        if (!isPptx) {
            throw new IllegalArgumentException("Solo se admiten archivos PowerPoint (.pptx).");
        }
        try {
            String name = (originalName != null && !originalName.isBlank()) ? originalName : "presentacion.pptx";
            String ct = file.getContentType() != null ? file.getContentType()
                    : "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            repository.upsertFile(id, name, ct, (int) file.getSize(), file.getBytes());
            repository.touch(id);
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo.", e);
        }
        return detail(id);
    }

    public void removeFile(UUID id) {
        requirePresentation(id);
        repository.deleteFile(id);
        repository.touch(id);
    }

    public PresentationFile getFileData(UUID id) {
        requirePresentation(id);
        return repository.findFile(id)
                .orElseThrow(() -> new PresentationNotFoundException("No hay archivo para esta presentación."));
    }

    // --- slides --------------------------------------------------------------

    public List<SlideDto> reorder(UUID deckId, List<UUID> orderedIds) {
        requirePresentation(deckId);
        List<UUID> existing = repository.findSlideIds(deckId);
        if (orderedIds.size() != existing.size() || !new HashSet<>(existing).containsAll(orderedIds)) {
            throw new IllegalArgumentException("La lista de diapositivas no es una permutación válida.");
        }
        repository.updateSortOrders(deckId, orderedIds);
        return repository.findSlides(deckId);
    }

    public List<SlideDto> addSlide(UUID deckId, SlideRequest req) {
        requirePresentation(deckId);
        List<UUID> existing = repository.findSlideIds(deckId);
        if (existing.size() >= 100) {
            throw new IllegalArgumentException("No se pueden añadir más de 100 diapositivas.");
        }
        String body = req.body() != null ? req.body() : "";
        repository.insertSlide(deckId, req.heading(), body, req.imageId(), existing.size());
        repository.touch(deckId);
        return repository.findSlides(deckId);
    }

    // --- shares --------------------------------------------------------------

    public PresentationDetail setShares(UUID presentationId, List<UUID> studentIds) {
        requirePresentation(presentationId);
        validateStudents(studentIds);
        repository.replaceShares(presentationId, studentIds);
        return detail(presentationId);
    }

    // --- helpers -------------------------------------------------------------

    private Presentation requirePresentation(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new PresentationNotFoundException("Presentación no encontrada."));
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

    private PresentationDetail detail(UUID id) {
        Presentation p = requirePresentation(id);
        String originalFileName = repository.findOriginalFileName(id).orElse(null);
        List<StudentResponse> sharedWith = repository.findSharedUsers(id).stream()
                .map(u -> new StudentResponse(u.userId(), u.email(), u.firstName(), u.lastName(), u.username()))
                .toList();
        return new PresentationDetail(p.getId(), p.getTitle(), p.getLevel(),
                originalFileName != null, originalFileName, sharedWith);
    }

    private static final java.util.Set<String> VALID_LEVELS =
            java.util.Set.of("A1", "A2", "B1", "B2", "C1", "C2");

    private static String validateLevel(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String upper = raw.toUpperCase(java.util.Locale.ROOT);
        return VALID_LEVELS.contains(upper) ? upper : null;
    }
}
