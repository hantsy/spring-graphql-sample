package com.example.demo.repository;

import com.example.demo.model.AuthorEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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

    public AuthorEntity findById(Long id) {
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

    public List<AuthorEntity> findByIdIn(List<Long> id) {
        String sql = """
                SELECT * FROM users
                WHERE id IN (:id)
                """;
        return this.jdbcClient.sql(sql)
                .param("id", id)
                .query(AuthorRepository::mapAuthor)
                .list();
    }

    public Long create(String name, String email) {
        String sql = """
                INSERT INTO users (name, email)
                VALUES (:name, :email)
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcClient.sql(sql)
                .param("name", name)
                .param("email", email)
                .update(keyHolder, "id");
        return keyHolder.getKeyAs(Long.class);
    }

    public int deleteAll() {
        String sql = "DELETE FROM users";
        return this.jdbcClient.sql(sql).update();
    }

    private static AuthorEntity mapAuthor(ResultSet rs, int rowNum) throws SQLException {
        return new AuthorEntity(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email")
        );
    }
}
