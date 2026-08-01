package com.kuky.backend.university.controller;

import com.kuky.backend.university.repository.UniversityAvailabilityRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/university/levels/{level}")
public class UniversityAvailabilityAdminController {
    private final UniversityAvailabilityRepository repository;
    public UniversityAvailabilityAdminController(UniversityAvailabilityRepository repository){this.repository=repository;}
    public record AssignmentIds(List<UUID> assignmentIds){}
    public record PresentationIds(List<UUID> presentationIds){}
    @GetMapping("/homeworks") public List<UUID> homeworks(@PathVariable String level){validate(level);return repository.ids("homeworks",level);}
    @PutMapping("/homeworks") public List<UUID> homeworks(@PathVariable String level,@RequestBody AssignmentIds x){validate(level);repository.replace("homeworks",level,x.assignmentIds()==null?List.of():x.assignmentIds());return repository.ids("homeworks",level);}
    @GetMapping("/presentations") public List<UUID> presentations(@PathVariable String level){validate(level);return repository.ids("presentations",level);}
    @PutMapping("/presentations") public List<UUID> presentations(@PathVariable String level,@RequestBody PresentationIds x){validate(level);repository.replace("presentations",level,x.presentationIds()==null?List.of():x.presentationIds());return repository.ids("presentations",level);}
    private static void validate(String level){UniversityAdminController.validateLevel(level);}
}
