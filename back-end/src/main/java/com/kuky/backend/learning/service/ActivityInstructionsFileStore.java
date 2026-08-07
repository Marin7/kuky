package com.kuky.backend.learning.service;

import com.kuky.backend.config.ActivityInstructionsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/** Stores activity instruction PDF bytes under {@code app.activity-instructions.storage-dir}. */
@Component
public class ActivityInstructionsFileStore {

    private static final Logger log = LoggerFactory.getLogger(ActivityInstructionsFileStore.class);
    private static final String EXT = ".pdf";

    private final Path storageDir;

    public ActivityInstructionsFileStore(ActivityInstructionsProperties props) {
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
            throw new UncheckedIOException("Failed to write activity instructions " + fileId, e);
        }
    }

    public Optional<byte[]> read(UUID fileId) {
        Path file = pathFor(fileId);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read activity instructions " + fileId, e);
        }
    }

    public void deleteQuietly(UUID fileId) {
        try {
            Files.deleteIfExists(pathFor(fileId));
        } catch (IOException e) {
            log.warn("Could not delete orphaned activity instructions {}: {}", fileId, e.toString());
        }
    }

    private Path pathFor(UUID fileId) {
        return storageDir.resolve(fileId + EXT);
    }
}
