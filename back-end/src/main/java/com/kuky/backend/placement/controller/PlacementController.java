package com.kuky.backend.placement.controller;

import com.kuky.backend.placement.dto.AttemptResultResponse;
import com.kuky.backend.placement.dto.FullEvaluationResponse;
import com.kuky.backend.placement.dto.PlacementTestResponse;
import com.kuky.backend.placement.dto.SectionResultResponse;
import com.kuky.backend.placement.dto.StartSectionResponse;
import com.kuky.backend.placement.dto.SubmitSectionRequest;
import com.kuky.backend.placement.dto.SubmitWritingRequest;
import com.kuky.backend.placement.dto.WritingStartResponse;
import com.kuky.backend.placement.dto.WritingSubmissionDto;
import com.kuky.backend.placement.model.Skill;
import com.kuky.backend.placement.service.PlacementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** Student-facing placement endpoints. Authenticated (covered by anyRequest().authenticated()). */
@RestController
@RequestMapping("/api/v1/placement")
public class PlacementController {

    private final PlacementService service;

    public PlacementController(PlacementService service) {
        this.service = service;
    }

    @GetMapping("/test")
    public PlacementTestResponse getTest(@AuthenticationPrincipal String email) {
        return service.getTest(email);
    }

    @PostMapping("/attempts")
    public ResponseEntity<Map<String, Object>> startAttempt(@AuthenticationPrincipal String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.startAttempt(email));
    }

    @PostMapping("/attempts/{attemptId}/sections/{skill}/start")
    public StartSectionResponse startSection(@AuthenticationPrincipal String email,
                                             @PathVariable UUID attemptId,
                                             @PathVariable Skill skill) {
        return service.startSection(email, attemptId, skill);
    }

    @PostMapping("/attempts/{attemptId}/sections/{skill}/submit")
    public SectionResultResponse submitSection(@AuthenticationPrincipal String email,
                                               @PathVariable UUID attemptId,
                                               @PathVariable Skill skill,
                                               @RequestBody(required = false) SubmitSectionRequest request) {
        return service.submitSection(email, attemptId, skill, request);
    }

    @GetMapping("/attempts/{attemptId}/result")
    public AttemptResultResponse getResult(@AuthenticationPrincipal String email, @PathVariable UUID attemptId) {
        return service.getResult(email, attemptId);
    }

    @GetMapping("/full-evaluation")
    public FullEvaluationResponse getFullEvaluation(@AuthenticationPrincipal String email) {
        return service.getFullEvaluation(email);
    }

    @PostMapping("/writing/start")
    public WritingStartResponse startWriting(@AuthenticationPrincipal String email) {
        return service.startWriting(email);
    }

    @PostMapping("/writing")
    public ResponseEntity<WritingSubmissionDto> submitWriting(@AuthenticationPrincipal String email,
                                                               @RequestBody SubmitWritingRequest request) {
        WritingSubmissionDto saved = service.submitWriting(email, request == null ? null : request.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
