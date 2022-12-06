package com.example.demo.repository;

import com.example.demo.model.AuthorEntity;
import com.example.demo.model.CommentEntity;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentRepository {
    private final DatabaseClient client;

    public static final BiFunction<Row, RowMetadata, CommentEntity> MAPPING_FUNCTION = (row, rowMetaData) -> new CommentEntity(
            row.get("id", UUID.class),
            row.get("content", String.class),
            row.get("created_at", LocalDateTime.class),
            row.get("post_id", UUID.class)
    );


    public Flux<CommentEntity> findAll() {
        String sql = "SELECT * FROM comments";
        return this.client.sql(sql)
                .filter((statement, executeFunction) -> statement.fetchSize(10).execute())
                .map(MAPPING_FUNCTION)
                .all();
    }

    public Mono<CommentEntity> findById(UUID id) {
        String sql = """
                SELECT * FROM comments 
                WHERE id  = :id
                """;
        return this.client.sql(sql)
                .bind("id", id)
                .map(MAPPING_FUNCTION)
                .one();

    }

    public Flux<CommentEntity> findByPostId(UUID postId) {
        String sql = """
                SELECT * FROM comments 
                WHERE post_id  = :post_id
                """;
        return this.client.sql(sql)
                .filter((statement, executeFunction) -> statement.fetchSize(10).execute())
                .bind("post_id", postId)
                .map(MAPPING_FUNCTION)
                .all();
    }

    public Flux<CommentEntity> findByPostIdIn(List<UUID> postId) {
        String sql = """
                SELECT * FROM comments 
                WHERE post_id  in (:post_id)
                """;
        return this.client.sql(sql)
                .bind("post_id", postId)
                .map(MAPPING_FUNCTION)
                .all();
    }

    public Mono<UUID> create(String content, UUID postId) {
        String insert = """
                INSERT INTO  comments (content, post_id, created_at) 
                VALUES (:content, :post_id, :created_at) 
                """;
        return this.client.sql(insert)
                .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                .bind("content", content)
                .bind("post_id", postId)
                .bind("created_at", LocalDateTime.now())
                .fetch()
                .first()
                .map(r -> (UUID) r.get("id"));
    }

    public Mono<Long> deleteAll() {
        String sql = "DELETE FROM comments";
        return this.client.sql(sql).fetch().rowsUpdated();
    }

}
