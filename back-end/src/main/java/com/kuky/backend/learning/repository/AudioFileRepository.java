package com.kuky.backend.learning.repository;

import com.kuky.backend.learning.model.AudioFile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AudioFileRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AudioFileRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID insert(String originalName, String contentType, byte[] data) {
        UUID id = UUID.randomUUID();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("originalName", originalName)
                .addValue("contentType", contentType)
                .addValue("byteSize", data.length)
                .addValue("data", data);
        jdbc.update("""
                INSERT INTO audio_files (id, original_name, content_type, byte_size, data)
                VALUES (:id, :originalName, :contentType, :byteSize, :data)
                """, params);
        return id;
    }

    public Optional<AudioFile> findById(UUID id) {
        return jdbc.query("SELECT * FROM audio_files WHERE id = :id", Map.of("id", id), (rs, n) -> {
            AudioFile f = new AudioFile();
            f.setId(rs.getObject("id", UUID.class));
            f.setOriginalName(rs.getString("original_name"));
            f.setContentType(rs.getString("content_type"));
            f.setByteSize(rs.getInt("byte_size"));
            f.setData(rs.getBytes("data"));
            return f;
        }).stream().findFirst();
    }

    /** Lightweight existence + name lookup that avoids loading the bytes. */
    public Optional<String> findOriginalName(UUID id) {
        return jdbc.query("SELECT original_name FROM audio_files WHERE id = :id",
                Map.of("id", id), (rs, n) -> rs.getString("original_name")).stream().findFirst();
    }
}
