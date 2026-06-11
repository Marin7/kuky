package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.StudentResponse;
import com.kuky.backend.auth.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Student roster for the homework-assignment and presentation-sharing pickers.
 * Admin-only (covered by the /api/v1/admin/** security matcher).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class StudentAdminController {

    private final UserRepository userRepository;

    public StudentAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/students")
    public List<StudentResponse> getStudents() {
        return userRepository.findStudents().stream()
                .map(u -> new StudentResponse(u.getId(), u.getEmail()))
                .toList();
    }
}
