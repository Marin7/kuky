package com.kuky.backend.scheduling.meeting;

import java.time.Instant;

public interface MeetingProvider {

    record MeetingDetails(String meetingId, String joinUrl) {}

    MeetingDetails create(Instant start, int durationMinutes, String topic);

    void cancel(String meetingId);
}
