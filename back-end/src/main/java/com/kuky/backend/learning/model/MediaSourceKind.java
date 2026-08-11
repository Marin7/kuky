package com.kuky.backend.learning.model;

/**
 * How listening homework media is presented. Payload lives in {@code audio_url}
 * or {@code audio_file_id} on {@link HomeworkAssignment}.
 */
public enum MediaSourceKind {
    AUDIO_URL,
    UPLOADED_FILE,
    VIDEO_PAGE,
    YOUTUBE
}
