package com.kuky.backend.university.controller;

import com.kuky.backend.university.repository.UniversityRepository;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/university")
public class UniversityAdminController {
    private final UniversityRepository r;
    public UniversityAdminController(UniversityRepository r){this.r=r;}
    public record SessionRequest(String level,int dayOfWeek,LocalTime startTime,LocalTime endTime,String title){}
    public record ExceptionRequest(String level,LocalDate exceptionDate,String kind,UUID sessionId,LocalTime startTime,LocalTime endTime,String title){}
    public record ExamRequest(String title,Instant examAt,String description,boolean published){}
    public record NewsRequest(String title,String body,boolean published,Instant publishedAt){}
    @GetMapping("/schedule/sessions") public List<UniversityRepository.Session> sessions(){return r.sessions(null);}
    @PostMapping("/schedule/sessions") public UUID addSession(@RequestBody SessionRequest x){validateLevel(x.level());validateSession(x);return r.saveSession(null,x.level(),x.dayOfWeek(),x.startTime(),x.endTime(),x.title());}
    @PutMapping("/schedule/sessions/{id}") public UUID updateSession(@PathVariable UUID id,@RequestBody SessionRequest x){validateLevel(x.level());validateSession(x);return r.saveSession(id,x.level(),x.dayOfWeek(),x.startTime(),x.endTime(),x.title());}
    @DeleteMapping("/schedule/sessions/{id}") public void deleteSession(@PathVariable UUID id){r.delete("university_schedule_sessions",id);}
    @GetMapping("/schedule/exceptions") public List<UniversityRepository.ExceptionRow> exceptions(@RequestParam(required=false)LocalDate from,@RequestParam(required=false)LocalDate to){return r.exceptions(from==null?LocalDate.now().minusDays(30):from,to==null?LocalDate.now().plusDays(90):to,null);}
    @PostMapping("/schedule/exceptions") public UUID addException(@RequestBody ExceptionRequest x){validateLevel(x.level());if(!"CANCEL".equals(x.kind())&&!"EXTRA".equals(x.kind()))throw new IllegalArgumentException("La excepción debe ser CANCEL o EXTRA.");if("CANCEL".equals(x.kind())&&x.sessionId()==null)throw new IllegalArgumentException("CANCEL requiere sessionId.");if("EXTRA".equals(x.kind())&&(x.startTime()==null||x.endTime()==null||!x.endTime().isAfter(x.startTime())))throw new IllegalArgumentException("EXTRA requiere horas válidas.");return r.saveException(x.level(),x.exceptionDate(),x.kind(),x.sessionId(),x.startTime(),x.endTime(),x.title());}
    @DeleteMapping("/schedule/exceptions/{id}") public void deleteException(@PathVariable UUID id){r.delete("university_schedule_exceptions",id);}
    @GetMapping("/exams") public List<UniversityRepository.Exam> exams(){return r.exams(true);}
    @PostMapping("/exams") public UUID addExam(@RequestBody ExamRequest x){return r.saveExam(null,x.title(),x.examAt(),x.description(),x.published());}
    @PutMapping("/exams/{id}") public UUID updateExam(@PathVariable UUID id,@RequestBody ExamRequest x){return r.saveExam(id,x.title(),x.examAt(),x.description(),x.published());}
    @DeleteMapping("/exams/{id}") public void deleteExam(@PathVariable UUID id){r.delete("university_exam_dates",id);}
    @GetMapping("/news") public List<UniversityRepository.News> news(){return r.news(true);}
    @PostMapping("/news") public UUID addNews(@RequestBody NewsRequest x){return r.saveNews(null,x.title(),x.body(),x.published(),x.publishedAt()==null&&x.published()?Instant.now():x.publishedAt());}
    @PutMapping("/news/{id}") public UUID updateNews(@PathVariable UUID id,@RequestBody NewsRequest x){return r.saveNews(id,x.title(),x.body(),x.published(),x.publishedAt()==null&&x.published()?Instant.now():x.publishedAt());}
    @DeleteMapping("/news/{id}") public void deleteNews(@PathVariable UUID id){r.delete("university_news_items",id);}
    static void validateLevel(String level){if(!"BEGINNER".equals(level)&&!"INTERMEDIATE".equals(level))throw new IllegalArgumentException("El nivel debe ser BEGINNER o INTERMEDIATE.");}
    static void validateSession(SessionRequest x){if(x.dayOfWeek()<1||x.dayOfWeek()>7||x.startTime()==null||x.endTime()==null||!x.endTime().isAfter(x.startTime()))throw new IllegalArgumentException("La sesión no es válida.");}
}
