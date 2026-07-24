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
import java.util.Optional;
import java.util.UUID;

/**
 * Stores presentation PPTX bytes on disk under {@code app.presentation-files.storage-dir},
 * with classpath fallback under {@code presentation-files/} for seed files bundled in the jar.
 */
@Component
public class PresentationFileStore {

    private static final Logger log = LoggerFactory.getLogger(PresentationFileStore.class);
    private static final String EXT = ".pptx";

    private final Path storageDir;

    public PresentationFileStore(PresentationFileProperties props) {
        this.storageDir = Path.of(props.getStorageDir()).toAbsolutePath().normalize();
    }

    public void write(UUID presentationId, byte[] data) {
        Path file = pathFor(presentationId);
        try {
            Files.createDirectories(storageDir);
            Files.write(file, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write presentation file " + presentationId, e);
        }
    }

    public Optional<byte[]> read(UUID presentationId) {
        Path file = pathFor(presentationId);
        if (Files.isRegularFile(file)) {
            try {
                return Optional.of(Files.readAllBytes(file));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read presentation file " + presentationId, e);
            }
        }
        ClassPathResource resource = new ClassPathResource("presentation-files/" + presentationId + EXT);
        if (!resource.exists()) {
            return Optional.empty();
        }
        try (InputStream in = resource.getInputStream()) {
            return Optional.of(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read classpath presentation file " + presentationId, e);
        }
    }

    public void deleteQuietly(UUID presentationId) {
        try {
            Files.deleteIfExists(pathFor(presentationId));
        } catch (IOException e) {
            log.warn("Could not delete orphaned presentation file {}: {}", presentationId, e.toString());
        }
    }

    private Path pathFor(UUID presentationId) {
        return storageDir.resolve(presentationId + EXT);
    }
}
