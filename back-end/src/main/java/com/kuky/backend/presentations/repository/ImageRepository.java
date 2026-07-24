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

    public void insert(UUID id, String contentType, int byteSize) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("contentType", contentType)
                .addValue("byteSize", byteSize);
        jdbc.update("""
                INSERT INTO images (id, content_type, byte_size)
                VALUES (:id, :contentType, :byteSize)
                """, params);
    }

    public Optional<Image> findById(UUID id) {
        return jdbc.query(
                "SELECT id, content_type, byte_size FROM images WHERE id = :id",
                Map.of("id", id),
                (rs, n) -> {
                    Image img = new Image();
                    img.setId(rs.getObject("id", UUID.class));
                    img.setContentType(rs.getString("content_type"));
                    img.setByteSize(rs.getInt("byte_size"));
                    return img;
                }
        ).stream().findFirst();
    }
}
