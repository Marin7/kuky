package com.kuky.backend.university.controller;

import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.university.repository.UniversityRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/university/schedule")
public class UniversityScheduleController {
    private final UniversityRepository repository; private final UserRepository users;
    public UniversityScheduleController(UniversityRepository repository, UserRepository users){this.repository=repository;this.users=users;}
    public record Response(String viewerMode,String level,List<UniversityRepository.Session> templateSessions,List<UniversityRepository.ExceptionRow> exceptions){}
    @GetMapping public Response get(Authentication authentication,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(required=false) String level) {
        String filter=null; if(authentication!=null && authentication.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_UNIVERSITY_STUDENT"))) filter=users.findByEmailIgnoreCase(authentication.getName()).map(u->u.getUniversityLevel()).orElse(null);
        else if(authentication!=null && authentication.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"))) filter=level;
        LocalDate start=from==null?LocalDate.now().minusDays(7):from, end=to==null?LocalDate.now().plusDays(30):to;
        return new Response(filter==null?"FULL_LABELED":"LEVEL_FILTERED",filter,repository.sessions(filter),repository.exceptions(start,end,filter));
    }
}
