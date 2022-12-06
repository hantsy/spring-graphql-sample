package com.example.demo.repository;

import com.example.demo.model.PostEntity;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostRepository {
    private final DatabaseClient client;

    public static final BiFunction<Row, RowMetadata, PostEntity> MAPPING_FUNCTION = (row, rowMetaData) -> new PostEntity(
            row.get("id", UUID.class),
            row.get("title", String.class),
            row.get("content", String.class),
            row.get("status", String.class),
            row.get("created_at", LocalDateTime.class),
            row.get("author_id", UUID.class)
    );

    public Flux<PostEntity> findAll() {
        String sql = "SELECT * FROM posts";
        return this.client.sql(sql)
                .filter((statement, executeFunction) -> statement.fetchSize(10).execute())
                .map(MAPPING_FUNCTION)
                .all();
    }

    public Flux<PostEntity> findByAuthorId(UUID authorId) {
        String sql = "SELECT * FROM posts WHERE author_id  = :author_id";
        return this.client.sql(sql)
                .bind("author_id", authorId)
                .map(MAPPING_FUNCTION)
                .all();
    }

    public Mono<PostEntity> findById(UUID id) {
        String sql = "SELECT * FROM posts WHERE id = :id";
        return this.client.sql(sql)
                .bind("id", id)
                .map(MAPPING_FUNCTION)
                .one();
    }

    public Mono<UUID> create(String title, String content, String status, UUID authorId) {
        String insert = """
                INSERT INTO  posts (title, content, status, author_id, created_at) 
                VALUES (:title, :content, :status, :author_id, :created_at) 
                """;
        return this.client.sql(insert)
                .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                .bind("title", title)
                .bind("content", content)
                .bind("status", status)
                .bind("author_id", authorId)
                .bind("created_at", LocalDateTime.now())
                .fetch()
                .first()
                .map(r -> (UUID) r.get("id"));
    }

    public Mono<Long> deleteAll() {
        String sql = "DELETE FROM posts";
        return this.client.sql(sql).fetch().rowsUpdated();
    }
}
