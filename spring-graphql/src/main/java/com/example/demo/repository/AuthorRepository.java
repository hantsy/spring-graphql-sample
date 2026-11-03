package com.example.demo.repository;

import com.example.demo.model.AuthorEntity;
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
public class AuthorRepository {
    private final JdbcClient jdbcClient;

    public List<AuthorEntity> findAll() {
        return this.jdbcClient.sql("SELECT * FROM users")
                .query(AuthorRepository::mapAuthor)
                .list();
    }

    public AuthorEntity findById(UUID id) {
        return this.jdbcClient.sql("SELECT * FROM users WHERE id = :id")
                .param("id", id)
                .query(AuthorRepository::mapAuthor)
                .optional()
                .orElse(null);
    }

    public List<AuthorEntity> findByIdIn(List<UUID> id) {
        return this.jdbcClient.sql("SELECT * FROM users WHERE id IN (:id)")
                .param("id", id)
                .query(AuthorRepository::mapAuthor)
                .list();
    }

    public UUID create(String name, String email) {
        return this.jdbcClient.sql("""
                INSERT INTO users (name, email, created_at)
                VALUES (:name, :email, :created_at)
                returning id
                """)
                .param("name", name)
                .param("email", email)
                .param("created_at", LocalDateTime.now())
                .query(UUID.class)
                .optional()
                .orElseThrow(() -> new IllegalStateException("Failed to create author"));
    }

    public int deleteAll() {
        return this.jdbcClient.sql("DELETE FROM users").update();
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
