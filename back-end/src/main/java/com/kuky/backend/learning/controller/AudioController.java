package com.kuky.backend.learning.controller;

import com.kuky.backend.learning.model.AudioFile;
import com.kuky.backend.learning.service.AudioService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Serves uploaded listening-homework audio. Authenticated (covered by
 * anyRequest().authenticated()); audio ids are unguessable UUIDs surfaced only
 * through the student's own homework/exercise responses. Mirrors {@code ImageController}.
 */
@RestController
@RequestMapping("/api/v1/audio")
public class AudioController {

    private final AudioService audioService;

    public AudioController(AudioService audioService) {
        this.audioService = audioService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        return audioService.find(id)
                .map(this::toResponse)
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> toResponse(AudioFile audio) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(audio.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(audio.getData());
    }
}
