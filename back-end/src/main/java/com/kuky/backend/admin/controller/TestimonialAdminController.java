package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.ReorderTestimonialsRequest;
import com.kuky.backend.admin.dto.TestimonialAdminResponse;
import com.kuky.backend.admin.dto.UpdateTestimonialTextRequest;
import com.kuky.backend.testimonials.service.TestimonialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/testimonials")
public class TestimonialAdminController {

    private final TestimonialService service;

    public TestimonialAdminController(TestimonialService service) {
        this.service = service;
    }

    @GetMapping
    public List<TestimonialAdminResponse> list() {
        return service.listAll();
    }

    @PostMapping("/{id}/approve")
    public TestimonialAdminResponse approve(@PathVariable UUID id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    public TestimonialAdminResponse reject(@PathVariable UUID id) {
        return service.reject(id);
    }

    @PostMapping("/{id}/unpublish")
    public TestimonialAdminResponse unpublish(@PathVariable UUID id) {
        return service.unpublish(id);
    }

    @PutMapping("/{id}")
    public TestimonialAdminResponse updateText(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateTestimonialTextRequest req) {
        return service.updateText(id, req.text());
    }

    @PutMapping("/reorder")
    public List<TestimonialAdminResponse> reorder(@Valid @RequestBody ReorderTestimonialsRequest req) {
        return service.reorder(req.orderedIds());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
