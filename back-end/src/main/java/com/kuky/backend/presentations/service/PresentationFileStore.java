package com.kuky.backend.presentations.service;

import com.kuky.backend.config.PresentationFileProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores presentation file bytes (PPTX or PDF) on disk under {@code app.presentation-files.storage-dir}.
 * On-disk names use a fixed {@code .pptx} suffix for historical reasons; content type and original
 * filename live in the DB. Keys are presentation <em>file</em> ids (not presentation ids).
 * Classpath fallback under {@code presentation-files/} for seed files.
 */
@Component
public class PresentationFileStore {

    private static final Logger log = LoggerFactory.getLogger(PresentationFileStore.class);
    /** Opaque storage suffix — not the uploaded file's real extension. */
    private static final String EXT = ".pptx";

    private final Path storageDir;

    public PresentationFileStore(PresentationFileProperties props) {
        this.storageDir = Path.of(props.getStorageDir()).toAbsolutePath().normalize();
    }

    public Path getStorageDir() {
        return storageDir;
    }

    public void write(UUID fileId, byte[] data) {
        Path file = pathFor(fileId);
        try {
            Files.createDirectories(storageDir);
            Files.write(file, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write presentation file " + fileId, e);
        }
    }

    public Optional<byte[]> read(UUID fileId) {
        Path file = pathFor(fileId);
        if (Files.isRegularFile(file)) {
            try {
                return Optional.of(Files.readAllBytes(file));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read presentation file " + fileId, e);
            }
        }
        ClassPathResource resource = new ClassPathResource("presentation-files/" + fileId + EXT);
        if (!resource.exists()) {
            return Optional.empty();
        }
        try (InputStream in = resource.getInputStream()) {
            return Optional.of(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read classpath presentation file " + fileId, e);
        }
    }

    public void deleteQuietly(UUID fileId) {
        try {
            Files.deleteIfExists(pathFor(fileId));
        } catch (IOException e) {
            log.warn("Could not delete orphaned presentation file {}: {}", fileId, e.toString());
        }
    }

    /**
     * One-shot legacy remap: {@code {presentationId}.pptx} → {@code {fileId}.pptx}.
     * No-op if the new path already exists or the old path is missing.
     */
    public void renameLegacyIfNeeded(UUID presentationId, UUID fileId) {
        Path oldPath = storageDir.resolve(presentationId + EXT);
        Path newPath = pathFor(fileId);
        if (!Files.isRegularFile(oldPath)) {
            return;
        }
        if (Files.isRegularFile(newPath)) {
            try {
                Files.deleteIfExists(oldPath);
            } catch (IOException e) {
                log.warn("Could not delete legacy presentation file {}: {}", oldPath, e.toString());
            }
            return;
        }
        try {
            Files.createDirectories(storageDir);
            Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.copy(oldPath, newPath);
                Files.deleteIfExists(oldPath);
            } catch (IOException e2) {
                log.warn("Could not remap presentation file {} → {}: {}", presentationId, fileId, e2.toString());
            }
        }
    }

    private Path pathFor(UUID fileId) {
        return storageDir.resolve(fileId + EXT);
    }
}
