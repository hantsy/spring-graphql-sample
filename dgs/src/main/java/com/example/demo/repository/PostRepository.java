package com.example.demo.repository;

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
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostRepository {
    final NamedParameterJdbcTemplate jdbcTemplate;
    private static final RowMapper<PostEntity> ROW_MAPPER = (rs, rowNum) -> new PostEntity(
            rs.getObject("id", Long.class),
            rs.getString("title"),
            rs.getString("content"),
            rs.getString("status"),
            rs.getObject("author_id", Long.class)
    );

    public List<PostEntity> findAll() {
        return this.jdbcTemplate.query(
                "SELECT * FROM posts",
                ROW_MAPPER
        );
    }

    public List<PostEntity> findByAuthorId(Long authorId) {
        return this.jdbcTemplate.query(
                "SELECT * FROM posts WHERE author_id  = :author_id",
                Map.of("author_id", authorId),
                ROW_MAPPER
        );
    }

    public PostEntity findById(Long id) {
        return this.jdbcTemplate.queryForObject(
                "SELECT * FROM posts WHERE id = :id",
                Map.of("id", id),
                ROW_MAPPER
        );
    }

    public Long create(String title, String content, String status, Long authorId) {
        String insert = """
                INSERT INTO  posts (title, content, status, author_id) 
                VALUES (:title, :content, :status, :author_id) 
                returning id
                """;
        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        int inserted = this.jdbcTemplate.update(
                insert,
                new MapSqlParameterSource(Map.of("title", title, "content", content, "status", status, "author_id", authorId )),
                generatedKeyHolder
        );
        log.info("inserted rows: {}", inserted);
        return generatedKeyHolder.getKeyAs(Long.class);
    }

    public int deleteAll() {
        return this.jdbcTemplate.update(
                "DELETE FROM posts",
                Map.of()
        );
    }
}
