package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.CreateHomeworkRequest;
import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.SetAssigneesRequest;
import com.kuky.backend.admin.dto.UpdateHomeworkRequest;
import com.kuky.backend.admin.service.HomeworkAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/homework")
public class HomeworkAdminController {

    private final HomeworkAdminService service;

    public HomeworkAdminController(HomeworkAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<HomeworkAdminItem> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<HomeworkAdminItem> create(@Valid @RequestBody CreateHomeworkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public HomeworkAdminItem update(@PathVariable UUID id,
                                    @Valid @RequestBody UpdateHomeworkRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/assignees")
    public HomeworkAdminItem setAssignees(@PathVariable UUID id,
                                          @Valid @RequestBody SetAssigneesRequest request) {
        return service.setAssignees(id, request.assigneeIds());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
