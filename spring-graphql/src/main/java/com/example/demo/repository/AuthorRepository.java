package com.example.demo.repository;

import com.example.demo.model.AuthorEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthorRepository {
    private final JdbcClient jdbcClient;

    public List<AuthorEntity> findAll() {
        String sql = "SELECT * FROM users";
        return this.jdbcClient.sql(sql)
                .query(AuthorRepository::mapAuthor)
                .list();
    }

    public AuthorEntity findById(UUID id) {
        String sql = """
                SELECT * FROM users
                WHERE id = :id
                """;
        return this.jdbcClient.sql(sql)
                .param("id", id)
                .query(AuthorRepository::mapAuthor)
                .optional()
                .orElse(null);
    }

    public List<AuthorEntity> findByIdIn(List<UUID> id) {
        String sql = """
                SELECT * FROM users
                WHERE id IN (:id)
                """;
        return this.jdbcClient.sql(sql)
                .param("id", id)
                .query(AuthorRepository::mapAuthor)
                .list();
    }

    public UUID create(String name, String email) {
        String sql = """
                INSERT INTO users (name, email, created_at)
                VALUES (:name, :email, :created_at)
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql(sql)
                .param("name", name)
                .param("email", email)
                .param("created_at", LocalDateTime.now())
                .update(keyHolder, "id");
        return keyHolder.getKeyAs(UUID.class);
    }

    public int deleteAll() {
        String sql = "DELETE FROM users";
        return this.jdbcClient.sql(sql).update();
    }

    private static AuthorEntity mapAuthor(ResultSet rs, int rowNum) throws SQLException {
        return new AuthorEntity(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("email"),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}
