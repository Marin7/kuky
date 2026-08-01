package com.kuky.backend.university.controller;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.learning.dto.*;
import com.kuky.backend.learning.service.ExerciseGradingService;
import com.kuky.backend.learning.service.HomeworkSubmissionService;
import com.kuky.backend.learning.repository.ContentRepository;
import com.kuky.backend.presentations.model.PresentationFile;
import com.kuky.backend.presentations.repository.PresentationRepository;
import com.kuky.backend.presentations.service.PresentationService;
import com.kuky.backend.university.repository.UniversityAvailabilityRepository;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/university/learning")
public class UniversityLearningController {
    private final UserRepository users; private final UniversityAvailabilityRepository availability; private final ContentRepository content;
    private final PresentationRepository presentations; private final PresentationService presentationService; private final HomeworkSubmissionService submissions; private final ExerciseGradingService exercises;
    public UniversityLearningController(UserRepository users,UniversityAvailabilityRepository availability,ContentRepository content,PresentationRepository presentations,PresentationService presentationService,HomeworkSubmissionService submissions,ExerciseGradingService exercises){this.users=users;this.availability=availability;this.content=content;this.presentations=presentations;this.presentationService=presentationService;this.submissions=submissions;this.exercises=exercises;}
    public record Overview(String level,List<SharedPresentationSummary> presentations,List<HomeworkItemResponse> homework){}
    @GetMapping public Overview overview(@AuthenticationPrincipal String email,@RequestParam(required=false)String level){
        User user=users.findByEmailIgnoreCase(email).orElseThrow();String actual="ADMIN".equals(user.getRole())?level:user.getUniversityLevel();if(actual==null)throw new IllegalArgumentException("Indica un nivel universitario.");
        List<SharedPresentationSummary> ps=presentations.listSummaries().stream().filter(p->availability.presentationAvailable(p.id(),actual)).map(p->new SharedPresentationSummary(p.id(),p.title(),p.hasFile(),null)).toList();
        List<HomeworkItemResponse> hw=availability.ids("homeworks",actual).stream().map(content::findPublishedAssignmentById).flatMap(Optional::stream).map(a->new HomeworkItemResponse(a.getId(),a.getTitle(),a.getInstructions(),a.getDueOn(),a.getHomeworkType()==null?null:a.getHomeworkType().name(),a.getLevel()==null?null:a.getLevel().name(),a.getFormat().name(),"PENDING",null,null,null,null,false,a.getAudioUrl(),a.getAudioFileId(),null)).toList();
        return new Overview(actual,ps,hw);
    }
    @GetMapping("/presentations/{id}/file") public ResponseEntity<byte[]> file(@AuthenticationPrincipal String email,@PathVariable UUID id){checkPresentation(email,id);PresentationFile f=presentationService.getFileData(id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(f.originalName(),StandardCharsets.UTF_8).build().toString()).body(f.data());}
    @PutMapping("/homework/{id}") public HomeworkItemResponse submit(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody(required=false) SubmitHomeworkRequest x){checkHomework(email,id);return submissions.submit(email,id,x==null?null:x.response());}
    @GetMapping("/homework/{id}") public ExerciseResponse exercise(@AuthenticationPrincipal String email,@PathVariable UUID id){checkHomework(email,id);return exercises.getExercise(email,id);}
    @PutMapping("/homework/{id}/answers") public ExerciseResultResponse answers(@AuthenticationPrincipal String email,@PathVariable UUID id,@RequestBody(required=false) SubmitExerciseRequest x){checkHomework(email,id);return exercises.submit(email,id,x);}
    private void checkHomework(String email,UUID id){User u=users.findByEmailIgnoreCase(email).orElseThrow();if(!"ADMIN".equals(u.getRole())&&!availability.homeworkAvailable(id,u.getUniversityLevel()))throw new com.kuky.backend.learning.exception.AssignmentNotFoundException("Tarea no encontrada.");}
    private void checkPresentation(String email,UUID id){User u=users.findByEmailIgnoreCase(email).orElseThrow();if(!"ADMIN".equals(u.getRole())&&!availability.presentationAvailable(id,u.getUniversityLevel()))throw new com.kuky.backend.presentations.exception.PresentationNotFoundException("Presentación no encontrada.");}
}
