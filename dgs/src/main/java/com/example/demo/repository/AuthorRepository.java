package com.example.demo.repository;

import com.example.demo.model.AuthorEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthorRepository {
    private final JdbcClient jdbcClient;

    public List<AuthorEntity> findAll() {
        return this.jdbcClient.sql("SELECT * FROM users")
                .query(AuthorRepository::mapAuthor)
                .list();
    }

    public AuthorEntity findById(Long id) {
        return this.jdbcClient.sql("SELECT * FROM users WHERE id = :id")
                .param("id", id)
                .query(AuthorRepository::mapAuthor)
                .optional()
                .orElse(null);
    }

    public List<AuthorEntity> findByIdIn(List<Long> id) {
        return this.jdbcClient.sql("SELECT * FROM users WHERE id IN (:id)")
                .param("id", id)
                .query(AuthorRepository::mapAuthor)
                .list();
    }

    public Long create(String name, String email) {
        return this.jdbcClient.sql("""
                INSERT INTO users (name, email)
                VALUES (:name, :email)
                returning id
                """)
                .param("name", name)
                .param("email", email)
                .query(Long.class)
                .optional()
                .orElseThrow(() -> new IllegalStateException("Failed to create author"));
    }

    public int deleteAll() {
        return this.jdbcClient.sql("DELETE FROM users").update();
    }

    private static AuthorEntity mapAuthor(ResultSet rs, int rowNum) throws SQLException {
        return new AuthorEntity(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email")
        );
    }
}
