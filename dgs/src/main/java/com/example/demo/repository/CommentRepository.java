package com.example.demo.repository;

import com.example.demo.model.CommentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CommentRepository {
    private final JdbcClient jdbcClient;

    public List<CommentEntity> findAll() {
        return this.jdbcClient.sql("SELECT * FROM comments")
                .query(CommentRepository::mapComment)
                .list();
    }

    public List<CommentEntity> findByPostId(Long postId) {
        return this.jdbcClient.sql("SELECT * FROM comments WHERE post_id = :post_id")
                .param("post_id", postId)
                .query(CommentRepository::mapComment)
                .list();
    }

    public List<CommentEntity> findByPostIdIn(List<Long> postId) {
        return this.jdbcClient.sql("SELECT * FROM comments WHERE post_id IN (:post_id)")
                .param("post_id", postId)
                .query(CommentRepository::mapComment)
                .list();
    }

    public Long create(String content, Long postId) {
        return this.jdbcClient.sql("""
                INSERT INTO comments (content, post_id)
                VALUES (:content, :post_id)
                returning id
                """)
                .param("content", content)
                .param("post_id", postId)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new IllegalStateException("Failed to create comment"));
    }

    public int deleteAll() {
        return this.jdbcClient.sql("DELETE FROM comments").update();
    }

    private static CommentEntity mapComment(ResultSet rs, int rowNum) throws SQLException {
        return new CommentEntity(
                rs.getLong("id"),
                rs.getString("content"),
                rs.getLong("post_id")
        );
    }
}
