package com.example.demo.repository;

import com.example.demo.model.AuthorEntity;
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
public class AuthorRepository {
        private final DatabaseClient client;

        public static final BiFunction<Row, RowMetadata, AuthorEntity> MAPPING_FUNCTION = (row,
                        rowMetaData) -> new AuthorEntity(
                                        row.get("id", Long.class),
                                        row.get("name", String.class),
                                        row.get("email", String.class));

        public Flux<AuthorEntity> findAll() {
                String sql = "SELECT * FROM users";
                return this.client.sql(sql)
                                .filter((statement, executeFunction) -> statement.fetchSize(10).execute())
                                .map(MAPPING_FUNCTION)
                                .all();
        }

        public Mono<AuthorEntity> findById(Long id) {
                String sql = """
                                SELECT * FROM users
                                WHERE id  = :id
                                """;
                return this.client.sql(sql)
                                .bind("id", id)
                                .map(MAPPING_FUNCTION)
                                .one();

        }

        public Flux<AuthorEntity> findByIdIn(List<Long> id) {
                String sql = """
                                SELECT * FROM users
                                WHERE id  in (:id)
                                """;
                return this.client.sql(sql)
                                .bind("id", id)
                                .map(MAPPING_FUNCTION)
                                .all();
        }

        public Mono<Long> create(String name, String email) {
                String insert = """
                                INSERT INTO  users  ( name, email)
                                VALUES ( :name, :email)
                                """;

                return this.client.sql(insert)
                                .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                                .bind("name", name)
                                .bind("email", email)
                                .fetch()
                                .first()
                                .map(r -> (Long) r.get("id"));

        }

        public Mono<Long> deleteAll() {
                String sql = "DELETE FROM users";
                return this.client.sql(sql).fetch().rowsUpdated();
        }
}
