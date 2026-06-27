package com.kuky.backend.scheduling.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** One absolute available window on a concrete date (a row of {@code week_availability}). */
public record DayWindow(LocalDate date, LocalTime startTime, LocalTime endTime) {}
