package com.example.demo.repository;

import com.example.demo.model.CommentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommentRepository {
    private final JdbcClient jdbcClient;

    public List<CommentEntity> findAll() {
        return this.jdbcClient.sql("SELECT * FROM comments")
                .query(CommentRepository::mapComment)
                .list();
    }

    public CommentEntity findById(UUID id) {
        return this.jdbcClient.sql("SELECT * FROM comments WHERE id = :id")
                .param("id", id)
                .query(CommentRepository::mapComment)
                .optional()
                .orElse(null);
    }

    public List<CommentEntity> findByPostId(UUID postId) {
        return this.jdbcClient.sql("SELECT * FROM comments WHERE post_id = :post_id")
                .param("post_id", postId)
                .query(CommentRepository::mapComment)
                .list();
    }

    public List<CommentEntity> findByPostIdIn(List<UUID> postId) {
        return this.jdbcClient.sql("SELECT * FROM comments WHERE post_id IN (:post_id)")
                .param("post_id", postId)
                .query(CommentRepository::mapComment)
                .list();
    }

    public UUID create(String content, UUID postId) {
        return this.jdbcClient.sql("""
                INSERT INTO comments (content, post_id, created_at)
                VALUES (:content, :post_id, :created_at)
                returning id
                """)
                .param("content", content)
                .param("post_id", postId)
                .param("created_at", LocalDateTime.now())
                .query(UUID.class)
                .optional()
                .orElseThrow(() -> new IllegalStateException("Failed to create comment"));
    }

    public int deleteAll() {
        return this.jdbcClient.sql("DELETE FROM comments").update();
    }

    private static CommentEntity mapComment(ResultSet rs, int rowNum) throws SQLException {
        return new CommentEntity(
                rs.getObject("id", UUID.class),
                rs.getString("content"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("post_id", UUID.class)
        );
    }
}
