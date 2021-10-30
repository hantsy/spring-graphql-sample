package com.example.demo.service;

import com.example.demo.gql.types.Author;
import com.example.demo.gql.types.Post;
import com.example.demo.model.AuthorEntity;
import com.example.demo.model.PostEntity;
import com.example.demo.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AuthorService {
    public static final Function<AuthorEntity, Author> MAPPER = entity -> Author.builder()
            .id(entity.id().toString())
            .name(entity.name())
            .email(entity.email())
            .build();

    private final AuthorRepository authors;

    public Mono<Author> getAuthorById(String id) {
        return this.authors.findById(UUID.fromString(id))
                .map(MAPPER)
                .switchIfEmpty(Mono.error(new AuthorNotFoundException(id)));
    }

    public Flux<Author> getAuthorByIdIn(List<String> keys) {
        List<UUID> uuids = keys.stream().map(UUID::fromString).toList();
        return this.authors.findByIdIn(uuids).map(MAPPER);
    }
}

