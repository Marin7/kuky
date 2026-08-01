package com.kuky.backend.university.controller;

import com.kuky.backend.university.repository.UniversityRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/university")
public class UniversityContentController {
    private final UniversityRepository repository;
    public UniversityContentController(UniversityRepository repository){this.repository=repository;}
    @GetMapping("/exams") public List<UniversityRepository.Exam> exams(){return repository.exams(false);}
    @GetMapping("/news") public List<UniversityRepository.News> news(){return repository.news(false);}
}
