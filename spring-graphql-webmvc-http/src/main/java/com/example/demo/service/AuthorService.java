package com.example.demo.service;

import com.example.demo.gql.types.Author;
import com.example.demo.model.AuthorEntity;
import com.example.demo.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AuthorService {
    private static final Function<AuthorEntity, Author> MAPPER = a -> Author.builder()
            .id(a.id().toString())
            .name(a.name())
            .email(a.email())
            .createdAt(a.createdAt())
            .build();
    final AuthorRepository authors;

    public Author getAuthorById(String id) {
        var authorEntity = this.authors.findById(UUID.fromString(id));
        return Optional.ofNullable(authorEntity)
                .map(MAPPER)
                .orElseThrow(() -> new AuthorNotFoundException(id));
    }

    public List<Author> getAuthorByIdIn(Collection<String> ids) {
        var uuids = ids.stream().map(UUID::fromString).toList();
        var authorEntities = this.authors.findByIdIn(uuids);
        return authorEntities.stream().map(MAPPER).toList();
    }
}

