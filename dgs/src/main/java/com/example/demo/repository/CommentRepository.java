package com.example.demo.repository;

import com.example.demo.model.CommentEntity;
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
            rs.getLong("id"),
            rs.getString("content"),
            rs.getLong("post_id")
    );

    public List<CommentEntity> findAll() {
        return this.jdbcTemplate.query(
                "SELECT * FROM comments",
                ROW_MAPPER
        );
    }

    public List<CommentEntity> findByPostId(Long postId) {
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

    public List<CommentEntity> findByPostIdIn(List<Long> postId) {
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

    public Long create(String content, Long postId) {
        String insert = """
                INSERT INTO  comments ( content, post_id) 
                VALUES ( :content, :post_id) 
                returning id
                """;
        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        int inserted = this.jdbcTemplate.update(
                insert,
                new MapSqlParameterSource(Map.of("content", content, "post_id", postId)),
                generatedKeyHolder
        );
        log.info("inserted rows: {}", inserted);
        return generatedKeyHolder.getKeyAs(Long.class);
    }

    public int deleteAll() {
        return this.jdbcTemplate.update(
                "DELETE FROM comments",
                Map.of()
        );
    }
}
