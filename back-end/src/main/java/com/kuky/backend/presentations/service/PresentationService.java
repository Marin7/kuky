package com.kuky.backend.presentations.service;

import com.kuky.backend.admin.dto.*;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.learning.service.ActivityInstructionsFileStore;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PresentationService {

    static final int MAX_FILES_PER_PRESENTATION = 10;
    private static final long MAX_FILE_BYTES = 50L * 1024 * 1024; // 50 MB
    private static final String PPTX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final PresentationRepository repository;
    private final UserRepository userRepository;
    private final PresentationFileStore fileStore;
    private final ActivityRepository activityRepository;
    private final ActivityInstructionsFileStore activityInstructionsFileStore;

    public PresentationService(PresentationRepository repository,
                               UserRepository userRepository,
                               PresentationFileStore fileStore,
                               ActivityRepository activityRepository,
                               ActivityInstructionsFileStore activityInstructionsFileStore) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.fileStore = fileStore;
        this.activityRepository = activityRepository;
        this.activityInstructionsFileStore = activityInstructionsFileStore;
    }

    public List<PresentationSummary> list() {
        List<PresentationRepository.Summary> summaries = repository.listSummaries();
        List<UUID> ids = summaries.stream().map(PresentationRepository.Summary::id).toList();
        Map<UUID, List<PresentationFileSummary>> filesByPresentation = repository.listFilesGrouped(ids);
        return summaries.stream()
                .map(s -> new PresentationSummary(
                        s.id(), s.title(), s.level(),
                        filesByPresentation.getOrDefault(s.id(), List.of()),
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
        List<UUID> fileIds = repository.listFileIds(id);
        List<UUID> instructionFileIds = activityRepository.findInstructionFileIdsByPresentationId(id);
        if (repository.delete(id) == 0) {
            throw new PresentationNotFoundException("Presentación no encontrada.");
        }
        for (UUID fileId : fileIds) {
            fileStore.deleteQuietly(fileId);
        }
        for (UUID instructionFileId : instructionFileIds) {
            activityInstructionsFileStore.deleteQuietly(instructionFileId);
        }
    }

    // --- file management -----------------------------------------------------

    public PresentationDetail uploadFile(UUID presentationId, MultipartFile file) {
        requirePresentation(presentationId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("El archivo no puede superar los 50 MB.");
        }
        if (repository.countFiles(presentationId) >= MAX_FILES_PER_PRESENTATION) {
            throw new IllegalArgumentException(
                    "No se pueden adjuntar más de " + MAX_FILES_PER_PRESENTATION + " archivos por presentación.");
        }
        String originalName = file.getOriginalFilename();
        String lowerName = originalName != null ? originalName.toLowerCase(Locale.ROOT) : "";
        String contentType = file.getContentType();
        final boolean isPdf;
        if (lowerName.endsWith(".pdf")) {
            isPdf = true;
        } else if (lowerName.endsWith(".pptx")) {
            isPdf = false;
        } else if (PDF_CONTENT_TYPE.equals(contentType)) {
            isPdf = true;
        } else if (PPTX_CONTENT_TYPE.equals(contentType)) {
            isPdf = false;
        } else {
            throw new IllegalArgumentException("Solo se admiten archivos PowerPoint (.pptx) o PDF (.pdf).");
        }
        try {
            String name;
            String ct;
            if (isPdf) {
                name = (originalName != null && !originalName.isBlank()) ? originalName : "presentacion.pdf";
                ct = PDF_CONTENT_TYPE;
            } else {
                name = (originalName != null && !originalName.isBlank()) ? originalName : "presentacion.pptx";
                ct = PPTX_CONTENT_TYPE;
            }
            String displayName = allocateDisplayName(presentationId, name);
            UUID fileId = UUID.randomUUID();
            byte[] data = file.getBytes();
            fileStore.write(fileId, data);
            try {
                repository.insertFile(fileId, presentationId, name, displayName, ct, data.length);
            } catch (RuntimeException e) {
                fileStore.deleteQuietly(fileId);
                throw e;
            }
            repository.touch(presentationId);
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo.", e);
        }
        return detail(presentationId);
    }

    public void removeFile(UUID presentationId, UUID fileId) {
        requirePresentation(presentationId);
        if (repository.deleteFile(presentationId, fileId) == 0) {
            throw new PresentationNotFoundException("Archivo no encontrado.");
        }
        activityRepository.clearTriggerForFile(fileId);
        fileStore.deleteQuietly(fileId);
        repository.touch(presentationId);
    }

    public PresentationFile getFileData(UUID presentationId, UUID fileId) {
        requirePresentation(presentationId);
        PresentationFile meta = repository.findFile(presentationId, fileId)
                .orElseThrow(() -> new PresentationNotFoundException("Archivo no encontrado."));
        byte[] data = fileStore.read(fileId)
                .orElseThrow(() -> new PresentationNotFoundException("Archivo no encontrado."));
        return new PresentationFile(
                meta.id(), meta.presentationId(), meta.originalName(), meta.displayName(),
                meta.contentType(), meta.byteSize(), meta.createdAt(), data);
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

    /** Visible for tests. */
    public static String allocateDisplayName(String originalName, Set<String> existingDisplayNames) {
        Set<String> used = existingDisplayNames.stream()
                .map(n -> n.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!used.contains(originalName.toLowerCase(Locale.ROOT))) {
            return originalName;
        }
        String base;
        String ext;
        int dot = originalName.lastIndexOf('.');
        if (dot > 0) {
            base = originalName.substring(0, dot);
            ext = originalName.substring(dot);
        } else {
            base = originalName;
            ext = "";
        }
        for (int n = 2; n < 10_000; n++) {
            String candidate = base + " (" + n + ")" + ext;
            if (!used.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        throw new IllegalStateException("No se pudo asignar un nombre de archivo único.");
    }

    private String allocateDisplayName(UUID presentationId, String originalName) {
        return allocateDisplayName(originalName, new HashSet<>(repository.listDisplayNames(presentationId)));
    }

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
        List<PresentationFileSummary> files = repository.listFiles(id);
        List<StudentResponse> sharedWith = repository.findSharedUsers(id).stream()
                .map(u -> new StudentResponse(u.userId(), u.email(), u.firstName(), u.lastName(), u.username()))
                .toList();
        return new PresentationDetail(p.getId(), p.getTitle(), p.getLevel(), files, sharedWith);
    }

    private static final java.util.Set<String> VALID_LEVELS =
            java.util.Set.of("A1", "A2", "B1", "B2", "C1", "C2");

    private static String validateLevel(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String upper = raw.toUpperCase(java.util.Locale.ROOT);
        return VALID_LEVELS.contains(upper) ? upper : null;
    }
}
