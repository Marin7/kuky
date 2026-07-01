package com.kuky.backend.placement.controller;

import com.kuky.backend.placement.dto.AdminQuestionDto;
import com.kuky.backend.placement.dto.LevelThresholdDto;
import com.kuky.backend.placement.dto.PlacementConfigDto;
import com.kuky.backend.placement.dto.ReorderQuestionsRequest;
import com.kuky.backend.placement.dto.StudentEvaluationResponse;
import com.kuky.backend.placement.dto.UpsertQuestionRequest;
import com.kuky.backend.placement.model.Skill;
import com.kuky.backend.placement.service.PlacementAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Teacher-only placement authoring (covered by the /api/v1/admin/** ADMIN-role security matcher). */
@RestController
@RequestMapping("/api/v1/admin/placement")
public class PlacementAdminController {

    private final PlacementAdminService service;

    public PlacementAdminController(PlacementAdminService service) {
        this.service = service;
    }

    @GetMapping("/config")
    public PlacementConfigDto getConfig() {
        return service.getConfig();
    }

    @PutMapping("/config")
    public PlacementConfigDto updateConfig(@RequestBody PlacementConfigDto request) {
        return service.updateConfig(request);
    }

    @GetMapping("/questions")
    public List<AdminQuestionDto> listQuestions(@RequestParam Skill skill) {
        return service.listQuestions(skill);
    }

    @PostMapping("/questions")
    public ResponseEntity<AdminQuestionDto> createQuestion(@RequestBody UpsertQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createQuestion(request));
    }

    @PutMapping("/questions/{id}")
    public AdminQuestionDto updateQuestion(@PathVariable UUID id, @RequestBody UpsertQuestionRequest request) {
        return service.updateQuestion(id, request);
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID id) {
        service.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/questions/reorder")
    public ResponseEntity<Void> reorder(@RequestParam Skill skill, @RequestBody ReorderQuestionsRequest request) {
        service.reorder(skill, request.orderedIds());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/students/{studentId}/evaluation")
    public StudentEvaluationResponse getStudentEvaluation(@PathVariable UUID studentId) {
        return service.getStudentEvaluation(studentId);
    }

    @GetMapping("/levels")
    public List<LevelThresholdDto> getLevelThresholds() {
        return service.getLevelThresholds();
    }

    @PutMapping("/levels")
    public List<LevelThresholdDto> updateLevelThresholds(@RequestBody List<LevelThresholdDto> request) {
        return service.updateLevelThresholds(request);
    }
}
