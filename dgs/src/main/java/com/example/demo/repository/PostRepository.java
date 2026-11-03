package com.example.demo.repository;

import com.example.demo.model.PostEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostRepository {
    private final JdbcClient jdbcClient;

    public List<PostEntity> findAll() {
        String sql = "SELECT * FROM posts";
        return this.jdbcClient.sql(sql)
                .query(PostRepository::mapPost)
                .list();
    }

    public List<PostEntity> findByAuthorId(Long authorId) {
        String sql = """
                SELECT * FROM posts
                WHERE author_id = :author_id
                """;
        return this.jdbcClient.sql(sql)
                .param("author_id", authorId)
                .query(PostRepository::mapPost)
                .list();
    }

    public PostEntity findById(Long id) {
        String sql = """
                SELECT * FROM posts
                WHERE id = :id
                """;
        return this.jdbcClient.sql(sql)
                .param("id", id)
                .query(PostRepository::mapPost)
                .optional()
                .orElse(null);
    }

    public Long create(String title, String content, String status, Long authorId) {
        String sql = """
                INSERT INTO posts (title, content, status, author_id)
                VALUES (:title, :content, :status, :author_id)
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql(sql)
                .param("title", title)
                .param("content", content)
                .param("status", status)
                .param("author_id", authorId)
                .update(keyHolder, "id");
        return keyHolder.getKeyAs(Long.class);
    }

    public int deleteAll() {
        String sql = "DELETE FROM posts";
        return this.jdbcClient.sql(sql).update();
    }

    private static PostEntity mapPost(ResultSet rs, int rowNum) throws SQLException {
        return new PostEntity(
                rs.getObject("id", Long.class),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("status"),
                rs.getObject("author_id", Long.class)
        );
    }
}
