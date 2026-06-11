package com.kuky.backend.scheduling.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** A date-specific override: BLOCK removes time, OPEN adds time, on {@code date}. */
public class AvailabilityException {

    public enum Kind { BLOCK, OPEN }

    private UUID id;
    private LocalDate date;
    private Kind kind;
    private LocalTime startTime;
    private LocalTime endTime;

    public AvailabilityException() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
