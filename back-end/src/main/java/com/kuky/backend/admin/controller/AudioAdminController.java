package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.AudioUploadResponse;
import com.kuky.backend.learning.service.AudioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/audio")
public class AudioAdminController {

    private final AudioService audioService;

    public AudioAdminController(AudioService audioService) {
        this.audioService = audioService;
    }

    @PostMapping
    public ResponseEntity<AudioUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        AudioService.UploadResult r = audioService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AudioUploadResponse(r.id(), r.originalName(), r.contentType(), r.byteSize()));
    }
}
