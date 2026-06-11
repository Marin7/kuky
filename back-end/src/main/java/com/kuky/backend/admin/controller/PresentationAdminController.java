package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.*;
import com.kuky.backend.presentations.model.PresentationFile;
import com.kuky.backend.presentations.service.PresentationService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/presentations")
public class PresentationAdminController {

    private final PresentationService service;

    public PresentationAdminController(PresentationService service) {
        this.service = service;
    }

    @GetMapping
    public List<PresentationSummary> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<PresentationDetail> create(@Valid @RequestBody CreatePresentationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req.title()));
    }

    @GetMapping("/{id}")
    public PresentationDetail get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public PresentationDetail rename(@PathVariable UUID id, @Valid @RequestBody CreatePresentationRequest req) {
        return service.rename(id, req.title());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- file management -----------------------------------------------------

    @PostMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PresentationDetail uploadFile(@PathVariable UUID id,
                                         @RequestParam("file") MultipartFile file) {
        return service.uploadFile(id, file);
    }

    @DeleteMapping("/{id}/file")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
        service.removeFile(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable UUID id) {
        PresentationFile f = service.getFileData(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(f.originalName(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(f.data());
    }

    // --- shares --------------------------------------------------------------

    @PutMapping("/{id}/shares")
    public PresentationDetail setShares(@PathVariable UUID id, @Valid @RequestBody SetSharesRequest req) {
        return service.setShares(id, req.studentIds());
    }
}
