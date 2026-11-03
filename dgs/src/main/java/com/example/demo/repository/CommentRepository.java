package com.example.demo.repository;

import com.example.demo.model.CommentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CommentRepository {
    private final JdbcClient jdbcClient;

    public List<CommentEntity> findAll() {
        String sql = "SELECT * FROM comments";
        return this.jdbcClient.sql(sql)
                .query(CommentRepository::mapComment)
                .list();
    }

    public List<CommentEntity> findByPostId(Long postId) {
        String sql = """
                SELECT * FROM comments
                WHERE post_id = :post_id
                """;
        return this.jdbcClient.sql(sql)
                .param("post_id", postId)
                .query(CommentRepository::mapComment)
                .list();
    }

    public List<CommentEntity> findByPostIdIn(List<Long> postId) {
        String sql = """
                SELECT * FROM comments
                WHERE post_id IN (:post_id)
                """;
        return this.jdbcClient.sql(sql)
                .param("post_id", postId)
                .query(CommentRepository::mapComment)
                .list();
    }

    public Long create(String content, Long postId) {
        String sql = """
                INSERT INTO comments (content, post_id)
                VALUES (:content, :post_id)
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql(sql)
                .param("content", content)
                .param("post_id", postId)
                .update(keyHolder, "id");
        return keyHolder.getKeyAs(Long.class);
    }

    public int deleteAll() {
        String sql = "DELETE FROM comments";
        return this.jdbcClient.sql(sql).update();
    }

    private static CommentEntity mapComment(ResultSet rs, int rowNum) throws SQLException {
        return new CommentEntity(
                rs.getLong("id"),
                rs.getString("content"),
                rs.getLong("post_id")
        );
    }
}
