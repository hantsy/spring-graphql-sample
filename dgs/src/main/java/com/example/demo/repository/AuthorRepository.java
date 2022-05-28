package com.example.demo.repository;

import com.example.demo.model.AuthorEntity;
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
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email")
    );

    public List<AuthorEntity> findAll() {
        return this.jdbcTemplate.query(
                "SELECT * FROM users",
                ROW_MAPPER
        );
    }

    public AuthorEntity findById(Long id) {
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

    public List<AuthorEntity> findByIdIn(List<Long> id) {
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

    public Long create(String name, String email) {
        String insert = """
                INSERT INTO  users  ( name, email) 
                VALUES ( :name, :email) 
                returning id
                """;
        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        int inserted = this.jdbcTemplate.update(
                insert,
                new MapSqlParameterSource(Map.of("name", name, "email", email)),
                generatedKeyHolder
        );
        log.info("inserted rows: {}", inserted);
        return generatedKeyHolder.getKeyAs(Long.class);
    }

    public int deleteAll() {
        return this.jdbcTemplate.update(
                "DELETE FROM users",
                Map.of()
        );
    }
}
