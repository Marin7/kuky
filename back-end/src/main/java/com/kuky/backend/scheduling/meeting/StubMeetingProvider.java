package com.kuky.backend.scheduling.meeting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public class StubMeetingProvider implements MeetingProvider {

    private static final Logger log = LoggerFactory.getLogger(StubMeetingProvider.class);

    @Override
    public MeetingDetails create(Instant start, int durationMinutes, String topic) {
        String id = "stub-" + UUID.randomUUID();
        String joinUrl = "https://zoom.invalid/local/" + id;
        log.warn("StubMeetingProvider — Zoom meeting URL for '{}' starting at {}: {}", topic, start, joinUrl);
        return new MeetingDetails(id, joinUrl);
    }

    @Override
    public void cancel(String meetingId) {
        log.warn("StubMeetingProvider — would cancel Zoom meeting: {}", meetingId);
    }
}
