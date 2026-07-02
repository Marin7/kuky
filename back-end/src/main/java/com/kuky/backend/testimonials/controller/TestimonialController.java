package com.kuky.backend.testimonials.controller;

import com.kuky.backend.testimonials.dto.MyTestimonialResponse;
import com.kuky.backend.testimonials.dto.SubmitTestimonialRequest;
import com.kuky.backend.testimonials.dto.TestimonialResponse;
import com.kuky.backend.testimonials.service.TestimonialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/testimonials")
public class TestimonialController {

    private final TestimonialService service;

    public TestimonialController(TestimonialService service) {
        this.service = service;
    }

    @GetMapping
    public List<TestimonialResponse> list() {
        return service.listApproved();
    }

    @PostMapping
    public MyTestimonialResponse submit(@AuthenticationPrincipal String email,
                                        @Valid @RequestBody SubmitTestimonialRequest request) {
        return service.submit(email, request.text());
    }

    @GetMapping("/me")
    public ResponseEntity<MyTestimonialResponse> getMine(@AuthenticationPrincipal String email) {
        return service.getMyTestimonial(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
