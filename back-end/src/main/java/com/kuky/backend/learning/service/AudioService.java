package com.kuky.backend.learning.service;

import com.kuky.backend.learning.exception.InvalidAudioException;
import com.kuky.backend.learning.model.AudioFile;
import com.kuky.backend.learning.repository.AudioFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Stores and serves uploaded listening-homework audio. Mirrors {@code ImageService}. */
@Service
public class AudioService {

    private static final Set<String> ALLOWED = Set.of(
            "audio/mpeg",   // .mp3
            "audio/mp4",    // .m4a
            "audio/x-m4a",
            "audio/aac",
            "audio/ogg",
            "audio/wav",
            "audio/x-wav",
            "audio/webm");
    private static final long MAX_BYTES = 25L * 1024 * 1024; // 25 MB (see V16 CHECK)

    private final AudioFileRepository audioRepository;

    public AudioService(AudioFileRepository audioRepository) {
        this.audioRepository = audioRepository;
    }

    public UploadResult store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAudioException("No se ha recibido ningún audio.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw new InvalidAudioException("Formato no permitido. Usa MP3, M4A, OGG, WAV o WEBM.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidAudioException("El audio supera el tamaño máximo de 25 MB.");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new InvalidAudioException("No se pudo leer el audio.");
        }
        if (data.length == 0 || data.length > MAX_BYTES) {
            throw new InvalidAudioException("El audio supera el tamaño máximo de 25 MB.");
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) name = "audio";
        if (name.length() > 255) name = name.substring(0, 255);
        UUID id = audioRepository.insert(name, contentType, data);
        return new UploadResult(id, name, contentType, data.length);
    }

    public Optional<AudioFile> find(UUID id) {
        return audioRepository.findById(id);
    }

    public record UploadResult(UUID id, String originalName, String contentType, int byteSize) {}
}
