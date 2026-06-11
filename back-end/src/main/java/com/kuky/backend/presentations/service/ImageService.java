package com.kuky.backend.presentations.service;

import com.kuky.backend.presentations.exception.InvalidImageException;
import com.kuky.backend.presentations.model.Image;
import com.kuky.backend.presentations.repository.ImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 2 * 1024 * 1024; // 2 MB

    private final ImageRepository imageRepository;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public UploadResult store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("No se ha recibido ninguna imagen.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw new InvalidImageException("Formato no permitido. Usa JPG, PNG o WEBP.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidImageException("La imagen supera el tamaño máximo de 2 MB.");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new InvalidImageException("No se pudo leer la imagen.");
        }
        if (data.length == 0 || data.length > MAX_BYTES) {
            throw new InvalidImageException("La imagen supera el tamaño máximo de 2 MB.");
        }
        UUID id = imageRepository.insert(contentType, data);
        return new UploadResult(id, contentType, data.length);
    }

    public Optional<Image> find(UUID id) {
        return imageRepository.findById(id);
    }

    public record UploadResult(UUID id, String contentType, int byteSize) {}
}
