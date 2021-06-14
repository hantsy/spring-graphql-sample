package com.example.demo.repository;

import com.example.demo.model.AuthorEntity;
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
public class AuthorRepository {
    final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<AuthorEntity> ROW_MAPPER = (rs, rowNum) -> new AuthorEntity(
            rs.getObject("id", UUID.class),
            rs.getString("name"),
            rs.getString("email"),
            rs.getObject("created_at", LocalDateTime.class)
    );

    public List<AuthorEntity> findAll() {
        return this.jdbcTemplate.query(
                "SELECT * FROM users",
                ROW_MAPPER
        );
    }

    public AuthorEntity findById(UUID id) {
        String sql = """
                SELECT * FROM users 
                WHERE id  = :id
                """;
        return this.jdbcTemplate.queryForObject(
                sql,
                Map.of("id", id),
                ROW_MAPPER
        );
    }

    public List<AuthorEntity> findByIdIn(List<UUID> id) {
        String sql = """
                SELECT * FROM users 
                WHERE id  in (:id)
                """;
        return this.jdbcTemplate.query(
                sql,
                Map.of("id", id),
                ROW_MAPPER
        );
    }

    public UUID create(String name, String email) {
        String insert = """
                INSERT INTO  users  ( name, email, created_at) 
                VALUES ( :name, :email, :created_at) 
                returning id
                """;
        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        int inserted = this.jdbcTemplate.update(
                insert,
                new MapSqlParameterSource(Map.of("name", name, "email", email, "created_at", LocalDateTime.now())),
                generatedKeyHolder
        );
        log.info("inserted rows: {}", inserted);
        return generatedKeyHolder.getKeyAs(UUID.class);
    }

    public int deleteAll() {
        return this.jdbcTemplate.update(
                "DELETE FROM users",
                Map.of()
        );
    }
}
