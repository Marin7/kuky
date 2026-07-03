package com.kuky.backend.scheduling.controller;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.dto.ScheduleResponse;
import com.kuky.backend.scheduling.dto.SlotResponse;
import com.kuky.backend.scheduling.exception.BookingNotAllowedException;
import com.kuky.backend.scheduling.service.AvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ScheduleController {

    private final AvailabilityService availabilityService;
    private final SchedulingProperties props;

    public ScheduleController(AvailabilityService availabilityService, SchedulingProperties props) {
        this.availabilityService = availabilityService;
        this.props = props;
    }

    @GetMapping("/schedule")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @RequestParam(required = false, defaultValue = "60") int durationMinutes) {
        if (durationMinutes != props.getScheduling().getClassDurationMinutes()
                && durationMinutes != props.getScheduling().getExtendedClassDurationMinutes()) {
            throw new BookingNotAllowedException(BookingNotAllowedException.Reason.INVALID_DURATION);
        }

        List<SlotResponse> slots = availabilityService.generateSchedule(durationMinutes).stream()
                .map(s -> new SlotResponse(s.getStart(), s.getEnd(), s.getStatus().name()))
                .toList();

        ScheduleResponse response = new ScheduleResponse(
                props.getScheduling().getTeacherTimezone(),
                availabilityService.getHorizonStart(),
                availabilityService.getHorizonEnd(),
                slots
        );
        return ResponseEntity.ok(response);
    }
}
