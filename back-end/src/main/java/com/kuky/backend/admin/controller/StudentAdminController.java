package com.kuky.backend.admin.controller;

import com.kuky.backend.admin.dto.ExtendedClassEligibilityResponse;
import com.kuky.backend.admin.dto.RegisteredUserResponse;
import com.kuky.backend.admin.dto.StudentProfileResponse;
import com.kuky.backend.admin.dto.StudentResponse;
import com.kuky.backend.admin.dto.UserRoleResponse;
import com.kuky.backend.admin.exception.UserNotFoundException;
import com.kuky.backend.admin.service.StudentProfileAdminService;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.auth.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Student roster for the homework-assignment and presentation-sharing pickers,
 * per-student profile view, and the registered-user promotion/revocation flow.
 * Admin-only (covered by the /api/v1/admin/** security matcher).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class StudentAdminController {

    private static final Logger log = LoggerFactory.getLogger(StudentAdminController.class);

    private final UserRepository userRepository;
    private final StudentProfileAdminService profileService;
    private final EmailService emailService;

    public StudentAdminController(UserRepository userRepository,
                                  StudentProfileAdminService profileService,
                                  EmailService emailService) {
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.emailService = emailService;
    }

    @GetMapping("/students")
    public List<StudentResponse> getStudents() {
        return userRepository.findStudents().stream()
                .map(u -> new StudentResponse(u.getId(), u.getEmail(),
                        u.getFirstName(), u.getLastName(), u.getUsername()))
                .toList();
    }

    /**
     * Ids of current students who hold "extended class" eligibility. Kept separate from
     * {@link #getStudents()} because that roster is also reused by the homework-assignment
     * and presentation-sharing pickers, which have no need for booking-duration eligibility.
     */
    @GetMapping("/students/extended-class-eligible-ids")
    public List<UUID> getExtendedClassEligibleStudentIds() {
        return userRepository.findExtendedClassEligibleStudentIds();
    }

    @GetMapping("/students/{id}/profile")
    public StudentProfileResponse getProfile(@PathVariable UUID id) {
        return profileService.getProfile(id);
    }

    @GetMapping("/users")
    public List<RegisteredUserResponse> getRegisteredUsers() {
        return userRepository.findRegisteredUsers().stream()
                .map(u -> new RegisteredUserResponse(u.getId(), u.getEmail(),
                        u.getFirstName(), u.getLastName(), u.getUsername()))
                .toList();
    }

    @PostMapping("/users/{id}/student")
    public UserRoleResponse grantStudent(@PathVariable UUID id) {
        User user = requireGrantableOrRevocableUser(id);
        if (!"STUDENT".equals(user.getRole())) {
            userRepository.promoteToStudentById(id);
            try {
                emailService.sendStudentGrantedEmail(user.getEmail());
            } catch (Exception e) {
                log.warn("EmailService — failed to send student-granted email to {}: {}",
                        user.getEmail(), e.getMessage());
            }
        }
        return new UserRoleResponse(id, "STUDENT");
    }

    @DeleteMapping("/users/{id}/student")
    public UserRoleResponse revokeStudent(@PathVariable UUID id) {
        User user = requireGrantableOrRevocableUser(id);
        if ("STUDENT".equals(user.getRole())) {
            userRepository.revokeStudentById(id);
            try {
                emailService.sendStudentRevokedEmail(user.getEmail());
            } catch (Exception e) {
                log.warn("EmailService — failed to send student-revoked email to {}: {}",
                        user.getEmail(), e.getMessage());
            }
        }
        return new UserRoleResponse(id, "USER");
    }

    @PostMapping("/users/{id}/extended-class")
    public ExtendedClassEligibilityResponse grantExtendedClass(@PathVariable UUID id) {
        User user = requireGrantableOrRevocableUser(id);
        if (!user.isExtendedClassEligible()) {
            userRepository.grantExtendedClassById(id);
            try {
                emailService.sendExtendedClassGrantedEmail(user.getEmail());
            } catch (Exception e) {
                log.warn("EmailService — failed to send extended-class-granted email to {}: {}",
                        user.getEmail(), e.getMessage());
            }
        }
        return new ExtendedClassEligibilityResponse(id, true);
    }

    @DeleteMapping("/users/{id}/extended-class")
    public ExtendedClassEligibilityResponse revokeExtendedClass(@PathVariable UUID id) {
        User user = requireGrantableOrRevocableUser(id);
        if (user.isExtendedClassEligible()) {
            userRepository.revokeExtendedClassById(id);
            try {
                emailService.sendExtendedClassRevokedEmail(user.getEmail());
            } catch (Exception e) {
                log.warn("EmailService — failed to send extended-class-revoked email to {}: {}",
                        user.getEmail(), e.getMessage());
            }
        }
        return new ExtendedClassEligibilityResponse(id, false);
    }

    /** Loads the user or 404s; an ADMIN id is treated as not-found since admins are out of scope. */
    private User requireGrantableOrRevocableUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado."));
        if ("ADMIN".equals(user.getRole())) {
            throw new UserNotFoundException("Usuario no encontrado.");
        }
        return user;
    }
}
