package com.example.demo.repository;

import com.example.demo.model.PostEntity;
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
public class PostRepository {
    private final JdbcClient jdbcClient;

    public List<PostEntity> findAll() {
        return this.jdbcClient.sql("SELECT * FROM posts")
                .query(PostRepository::mapPost)
                .list();
    }

    public List<PostEntity> findByAuthorId(UUID authorId) {
        return this.jdbcClient.sql("""
                SELECT * FROM posts WHERE author_id = :author_id
                """)
                .param("author_id", authorId)
                .query(PostRepository::mapPost)
                .list();
    }

    public PostEntity findById(UUID id) {
        return this.jdbcClient.sql("SELECT * FROM posts WHERE id = :id")
                .param("id", id)
                .query(PostRepository::mapPost)
                .optional()
                .orElse(null);
    }

    public UUID create(String title, String content, String status, UUID authorId) {
        return this.jdbcClient.sql("""
                INSERT INTO posts (title, content, status, author_id, created_at)
                VALUES (:title, :content, :status, :author_id, :created_at)
                returning id
                """)
                .param("title", title)
                .param("content", content)
                .param("status", status)
                .param("author_id", authorId)
                .param("created_at", LocalDateTime.now())
                .query(UUID.class)
                .optional()
                .orElseThrow(() -> new IllegalStateException("Failed to create post"));
    }

    public int deleteAll() {
        return this.jdbcClient.sql("DELETE FROM posts").update();
    }

    private static PostEntity mapPost(ResultSet rs, int rowNum) throws SQLException {
        return new PostEntity(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("status"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("author_id", UUID.class)
        );
    }
}
