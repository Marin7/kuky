package com.kuky.backend.presentations.controller;

import com.kuky.backend.presentations.model.Image;
import com.kuky.backend.presentations.service.ImageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Serves uploaded images. Authenticated (covered by anyRequest().authenticated()); image ids
 * are unguessable UUIDs reachable only via the share-gated deck endpoints (see research §3).
 */
@RestController
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        return imageService.find(id)
                .map(this::toResponse)
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> toResponse(Image img) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .body(img.getData());
    }
}
