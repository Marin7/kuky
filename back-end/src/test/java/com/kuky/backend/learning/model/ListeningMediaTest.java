package com.kuky.backend.learning.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ListeningMediaTest {

    @Test
    void extractYouTubeIdFromCommonForms() {
        assertThat(ListeningMedia.extractYouTubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
                .isEqualTo("dQw4w9WgXcQ");
        assertThat(ListeningMedia.extractYouTubeId("https://youtu.be/dQw4w9WgXcQ"))
                .isEqualTo("dQw4w9WgXcQ");
        assertThat(ListeningMedia.extractYouTubeId("https://example.com/video")).isNull();
    }

    @Test
    void incompleteAudioWithoutKind() {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setHomeworkType(HomeworkType.AUDIO);
        assertThat(ListeningMedia.isComplete(a)).isFalse();
    }

    @Test
    void completeYoutube() {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setHomeworkType(HomeworkType.AUDIO);
        a.setMediaSourceKind(MediaSourceKind.YOUTUBE);
        a.setAudioUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertThat(ListeningMedia.isComplete(a)).isTrue();
    }

    @Test
    void completeUploadedFile() {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setHomeworkType(HomeworkType.AUDIO);
        a.setMediaSourceKind(MediaSourceKind.UPLOADED_FILE);
        a.setAudioFileId(UUID.randomUUID());
        assertThat(ListeningMedia.isComplete(a)).isTrue();
    }

    @Test
    void nonAudioAlwaysComplete() {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setHomeworkType(HomeworkType.WRITE);
        assertThat(ListeningMedia.isComplete(a)).isTrue();
    }
}
