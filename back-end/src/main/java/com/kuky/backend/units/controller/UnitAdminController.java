package com.kuky.backend.units.controller;

import com.kuky.backend.admin.dto.ActivityAdminDetail;
import com.kuky.backend.admin.service.ActivityAdminService;
import com.kuky.backend.presentations.model.PresentationFile;
import com.kuky.backend.units.dto.*;
import com.kuky.backend.units.service.UnitPdfService;
import com.kuky.backend.units.service.UnitService;
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
@RequestMapping("/api/v1/admin/units")
public class UnitAdminController {

    private final UnitService service;
    private final UnitPdfService pdfService;
    private final ActivityAdminService activityService;

    public UnitAdminController(UnitService service,
                               UnitPdfService pdfService,
                               ActivityAdminService activityService) {
        this.service = service;
        this.pdfService = pdfService;
        this.activityService = activityService;
    }

    @GetMapping
    public List<UnitSummary> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<UnitDetail> create(@Valid @RequestBody CreateUnitRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req.level(), req.subject()));
    }

    @GetMapping("/{id}")
    public UnitDetail get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public UnitDetail update(@PathVariable UUID id, @Valid @RequestBody UpdateUnitRequest req) {
        return service.update(id, req.level(), req.subject());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public List<UnitSummary> reorder(@Valid @RequestBody ReorderUnitsRequest req) {
        return service.reorder(req.level(), req.orderedIds());
    }

    @PutMapping("/{id}/assignees")
    public UnitDetail setAssignees(@PathVariable UUID id,
                                   @Valid @RequestBody SetUnitAssigneesRequest req) {
        return service.setAssignees(id, req.studentIds());
    }

    @PutMapping("/{id}/pages/{page}/title")
    public UnitDetail setPageTitle(@PathVariable UUID id, @PathVariable int page,
                                   @Valid @RequestBody SetPageTitleRequest req) {
        return service.setPageTitle(id, page, req.title());
    }

    @PutMapping("/{id}/quizzes")
    public UnitDetail setQuizzes(@PathVariable UUID id, @Valid @RequestBody SetUnitQuizzesRequest req) {
        return service.setQuizzes(id, req.quizIds());
    }

    // --- the unit's PDF ------------------------------------------------------

    @PostMapping(value = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UnitDetail uploadPdf(@PathVariable UUID id,
                                @RequestParam("file") MultipartFile file,
                                @RequestParam(value = "pageCount", required = false) Integer pageCount,
                                @RequestParam(value = "removeOutOfRangeActivities", defaultValue = "false")
                                boolean removeOutOfRangeActivities) {
        pdfService.upload(id, file, pageCount, removeOutOfRangeActivities);
        return service.get(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        PresentationFile f = pdfService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(f.displayName(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(f.data());
    }

    // --- page activities -----------------------------------------------------

    @PostMapping("/{id}/activities")
    public ResponseEntity<ActivityAdminDetail> createActivity(@PathVariable UUID id,
                                                              @Valid @RequestBody CreatePageActivityRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.createOnPage(id, req.page(), req.title(), req.questions()));
    }
}
