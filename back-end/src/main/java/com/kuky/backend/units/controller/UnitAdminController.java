package com.kuky.backend.units.controller;

import com.kuky.backend.units.dto.*;
import com.kuky.backend.units.service.UnitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/units")
public class UnitAdminController {

    private final UnitService service;

    public UnitAdminController(UnitService service) {
        this.service = service;
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

    @PutMapping("/{id}/presentations")
    public UnitDetail setPresentations(@PathVariable UUID id,
                                       @Valid @RequestBody SetUnitPresentationsRequest req) {
        return service.setPresentations(id, req.presentationIds());
    }

    @PutMapping("/{id}/homeworks")
    public UnitDetail setHomeworks(@PathVariable UUID id,
                                   @Valid @RequestBody SetUnitHomeworksRequest req) {
        return service.setHomeworks(id, req.homeworkIds());
    }

    @PutMapping("/{id}/assignees")
    public UnitDetail setAssignees(@PathVariable UUID id,
                                   @Valid @RequestBody SetUnitAssigneesRequest req) {
        return service.setAssignees(id, req.studentIds());
    }
}
