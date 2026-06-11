package com.kuky.backend.scheduling.model;

import java.time.LocalTime;
import java.util.UUID;

/** One recurring weekly availability window for a given weekday (ISO 1=Mon … 7=Sun). */
public class AvailabilityRule {

    private UUID id;
    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    public AvailabilityRule() {}

    public AvailabilityRule(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
