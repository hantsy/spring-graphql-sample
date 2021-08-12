package com.example.demo.repository;

import com.example.demo.model.CommentEntity;
import com.example.demo.model.PostEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentRepository {
    final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<CommentEntity> ROW_MAPPER = (rs, rowNum) -> new CommentEntity(
            rs.getObject("id", UUID.class),
            rs.getString("content"),
            rs.getObject("created_at", LocalDateTime.class),
            rs.getObject("post_id", UUID.class)
    );

    public List<CommentEntity> findAll() {
        return this.jdbcTemplate.query(
                "SELECT * FROM comments",
                ROW_MAPPER
        );
    }

    public CommentEntity findById(UUID id) {
        return this.jdbcTemplate.queryForObject(
                "SELECT * FROM comments WHERE id = :id",
                Map.of("id", id),
                ROW_MAPPER
        );
    }

    public List<CommentEntity> findByPostId(UUID postId) {
        String sql = """
                SELECT * FROM comments 
                WHERE post_id  = :post_id
                """;
        return this.jdbcTemplate.query(
                sql,
                Map.of("post_id", postId),
                ROW_MAPPER
        );
    }

    public List<CommentEntity> findByPostIdIn(List<UUID> postId) {
        String sql = """
                SELECT * FROM comments 
                WHERE post_id  in (:post_id)
                """;
        return this.jdbcTemplate.query(
                sql,
                Map.of("post_id", postId),
                ROW_MAPPER
        );
    }

    public UUID create(String content, UUID postId) {
        String insert = """
                INSERT INTO  comments ( content, post_id, created_at) 
                VALUES ( :content, :post_id, :created_at) 
                returning id
                """;
        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        int inserted = this.jdbcTemplate.update(
                insert,
                new MapSqlParameterSource(Map.of("content", content, "post_id", postId, "created_at", LocalDateTime.now())),
                generatedKeyHolder
        );
        log.info("inserted rows: {}", inserted);
        return generatedKeyHolder.getKeyAs(UUID.class);
    }

    public int deleteAll() {
        return this.jdbcTemplate.update(
                "DELETE FROM comments",
                Map.of()
        );
    }
}
