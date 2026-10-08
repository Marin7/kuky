package com.kuky.backend.units.service;

import com.kuky.backend.learning.repository.ActivityRepository;
import com.kuky.backend.presentations.exception.PresentationNotFoundException;
import com.kuky.backend.presentations.model.PresentationFile;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.presentations.service.PresentationFileStore;
import com.kuky.backend.units.exception.ActivitiesOutOfRangeException;
import com.kuky.backend.units.exception.UnitNotFoundException;
import com.kuky.backend.units.model.Unit;
import com.kuky.backend.units.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * The unit's single PDF (stored as the unit-owned presentation's one file). The page count
 * comes from pdf.js in the teacher's browser and bounds which pages can be activities.
 */
@Service
@Transactional
public class UnitPdfService {

    static final long MAX_FILE_BYTES = 50L * 1024 * 1024; // same limit as presentation files
    static final int MAX_PAGES = 500;
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final UnitRepository unitRepository;
    private final PresentationRepository presentationRepository;
    private final ActivityRepository activityRepository;
    private final PresentationFileStore fileStore;

    public UnitPdfService(UnitRepository unitRepository,
                          PresentationRepository presentationRepository,
                          ActivityRepository activityRepository,
                          PresentationFileStore fileStore) {
        this.unitRepository = unitRepository;
        this.presentationRepository = presentationRepository;
        this.activityRepository = activityRepository;
        this.fileStore = fileStore;
    }

    /**
     * Uploads or replaces the unit's PDF. Replacing with fewer pages deletes the activities
     * (and their submissions) on the vanished pages — only when the teacher confirmed it.
     */
    public void upload(UUID unitId, MultipartFile file, Integer pageCount, boolean removeOutOfRangeActivities) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new UnitNotFoundException("Unidad no encontrada."));
        byte[] data = validatedPdfBytes(file);
        if (pageCount == null || pageCount < 1 || pageCount > MAX_PAGES) {
            throw new IllegalArgumentException("El PDF debe tener entre 1 y " + MAX_PAGES + " páginas.");
        }

        var owned = presentationRepository.findOwnedByUnit(unitId);
        if (owned.isPresent()) {
            int outOfRange = activityRepository.countBeyondPage(owned.get().presentationId(), pageCount);
            if (outOfRange > 0 && !removeOutOfRangeActivities) {
                throw new ActivitiesOutOfRangeException("El nuevo PDF tiene " + pageCount
                        + " páginas: se borrarían " + outOfRange + " actividad(es) y sus respuestas.");
            }
        }

        String originalName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "unidad.pdf" : file.getOriginalFilename();
        UUID newFileId = UUID.randomUUID();
        fileStore.write(newFileId, data);
        try {
            UUID presentationId;
            UUID oldFileId = null;
            if (owned.isPresent()) {
                presentationId = owned.get().presentationId();
                oldFileId = owned.get().fileId();
                if (removeOutOfRangeActivities) {
                    activityRepository.deleteBeyondPage(presentationId, pageCount);
                }
                if (oldFileId != null) {
                    presentationRepository.deleteFile(presentationId, oldFileId);
                }
            } else {
                presentationId = presentationRepository.insertOwned(
                        unitId, unit.getSubject(), unit.getLevel(), unitRepository.nextContentPosition(unitId));
            }
            presentationRepository.insertFile(newFileId, presentationId, originalName, originalName,
                    PDF_CONTENT_TYPE, data.length, pageCount);
            presentationRepository.touch(presentationId);
            unitRepository.touch(unitId);
            if (oldFileId != null) {
                UUID replaced = oldFileId;
                afterCommit(() -> fileStore.deleteQuietly(replaced));
            }
            onRollback(() -> fileStore.deleteQuietly(newFileId));
        } catch (RuntimeException e) {
            fileStore.deleteQuietly(newFileId);
            throw e;
        }
    }

    public PresentationFile download(UUID unitId) {
        unitRepository.findById(unitId)
                .orElseThrow(() -> new UnitNotFoundException("Unidad no encontrada."));
        var owned = presentationRepository.findOwnedByUnit(unitId)
                .filter(p -> p.fileId() != null)
                .orElseThrow(() -> new PresentationNotFoundException("La unidad no tiene PDF."));
        PresentationFile meta = presentationRepository.findFile(owned.presentationId(), owned.fileId())
                .orElseThrow(() -> new PresentationNotFoundException("Archivo no encontrado."));
        byte[] bytes = fileStore.read(meta.id())
                .orElseThrow(() -> new PresentationNotFoundException("Archivo no encontrado."));
        return new PresentationFile(meta.id(), meta.presentationId(), meta.originalName(), meta.displayName(),
                meta.contentType(), meta.byteSize(), meta.createdAt(), bytes);
    }

    /** Removes the unit's PDF bytes from disk once the unit's deletion has committed. */
    public void deleteFileAfterCommit(UUID unitId) {
        presentationRepository.findOwnedByUnit(unitId)
                .map(p -> p.fileId())
                .ifPresent(fileId -> afterCommit(() -> fileStore.deleteQuietly(fileId)));
    }

    // --- helpers -------------------------------------------------------------

    private static byte[] validatedPdfBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("El archivo no puede superar los 50 MB.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".pdf") && !PDF_CONTENT_TYPE.equals(file.getContentType())) {
            throw new IllegalArgumentException("Solo se admiten archivos PDF (.pdf).");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Error al leer el archivo.", e);
        }
        if (data.length < PDF_MAGIC.length
                || !Arrays.equals(Arrays.copyOf(data, PDF_MAGIC.length), PDF_MAGIC)) {
            throw new IllegalArgumentException("El archivo no es un PDF válido.");
        }
        return data;
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private static void onRollback(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        action.run();
                    }
                }
            });
        }
    }
}
