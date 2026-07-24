package com.kuky.backend.presentations.service;

import com.kuky.backend.config.ImageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores image bytes on disk under {@code app.images.storage-dir}, with classpath
 * fallback under {@code images/} for seed files bundled in the jar.
 */
@Component
public class ImageFileStore {

    private static final Logger log = LoggerFactory.getLogger(ImageFileStore.class);

    private static final Map<String, String> EXT_BY_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path storageDir;

    public ImageFileStore(ImageProperties props) {
        this.storageDir = Path.of(props.getStorageDir()).toAbsolutePath().normalize();
    }

    public void write(UUID id, String contentType, byte[] data) {
        Path file = pathFor(id, contentType);
        try {
            Files.createDirectories(storageDir);
            Files.write(file, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write image " + id, e);
        }
    }

    public Optional<byte[]> read(UUID id, String contentType) {
        Path file = pathFor(id, contentType);
        if (Files.isRegularFile(file)) {
            try {
                return Optional.of(Files.readAllBytes(file));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read image " + id, e);
            }
        }
        String classpathName = "images/" + id + extension(contentType);
        ClassPathResource resource = new ClassPathResource(classpathName);
        if (!resource.exists()) {
            return Optional.empty();
        }
        try (InputStream in = resource.getInputStream()) {
            return Optional.of(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read classpath image " + id, e);
        }
    }

    public void deleteQuietly(UUID id, String contentType) {
        try {
            Files.deleteIfExists(pathFor(id, contentType));
        } catch (IOException e) {
            log.warn("Could not delete orphaned image file {}: {}", id, e.toString());
        }
    }

    private Path pathFor(UUID id, String contentType) {
        return storageDir.resolve(id + extension(contentType));
    }

    private static String extension(String contentType) {
        String ext = EXT_BY_TYPE.get(contentType);
        if (ext == null) {
            throw new IllegalArgumentException("Unsupported image content type: " + contentType);
        }
        return ext;
    }
}
