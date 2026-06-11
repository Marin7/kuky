package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.ImageUploadResponse;
import com.kuky.backend.presentations.service.ImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/images")
public class ImageAdminController {

    private final ImageService imageService;

    public ImageAdminController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping
    public ResponseEntity<ImageUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        ImageService.UploadResult r = imageService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ImageUploadResponse(r.id(), r.contentType(), r.byteSize()));
    }
}
