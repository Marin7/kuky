package com.kuky.backend.presentations.repository;

import com.kuky.backend.presentations.model.Image;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ImageRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ImageRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID insert(String contentType, byte[] data) {
        UUID id = UUID.randomUUID();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("contentType", contentType)
                .addValue("byteSize", data.length)
                .addValue("data", data);
        jdbc.update("""
                INSERT INTO images (id, content_type, byte_size, data)
                VALUES (:id, :contentType, :byteSize, :data)
                """, params);
        return id;
    }

    public Optional<Image> findById(UUID id) {
        return jdbc.query("SELECT * FROM images WHERE id = :id", Map.of("id", id), (rs, n) -> {
            Image img = new Image();
            img.setId(rs.getObject("id", UUID.class));
            img.setContentType(rs.getString("content_type"));
            img.setByteSize(rs.getInt("byte_size"));
            img.setData(rs.getBytes("data"));
            return img;
        }).stream().findFirst();
    }
}
