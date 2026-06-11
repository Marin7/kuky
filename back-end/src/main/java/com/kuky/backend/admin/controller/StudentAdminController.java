package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.StudentProfileResponse;
import com.kuky.backend.admin.dto.StudentResponse;
import com.kuky.backend.admin.service.StudentProfileAdminService;
import com.kuky.backend.auth.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Student roster for the homework-assignment and presentation-sharing pickers,
 * plus per-student profile view for the admin panel.
 * Admin-only (covered by the /api/v1/admin/** security matcher).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class StudentAdminController {

    private final UserRepository userRepository;
    private final StudentProfileAdminService profileService;

    public StudentAdminController(UserRepository userRepository,
                                  StudentProfileAdminService profileService) {
        this.userRepository = userRepository;
        this.profileService = profileService;
    }

    @GetMapping("/students")
    public List<StudentResponse> getStudents() {
        return userRepository.findStudents().stream()
                .map(u -> new StudentResponse(u.getId(), u.getEmail(),
                        u.getFirstName(), u.getLastName(), u.getUsername()))
                .toList();
    }

    @GetMapping("/students/{id}/profile")
    public StudentProfileResponse getProfile(@PathVariable UUID id) {
        return profileService.getProfile(id);
    }
}
