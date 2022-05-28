package com.example.demo.service;

import com.example.demo.gql.types.Author;
import com.example.demo.model.AuthorEntity;
import com.example.demo.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AuthorService {
    private static final Function<AuthorEntity, Author> MAPPER = a -> Author.builder()
            .id(a.id())
            .name(a.name())
            .email(a.email())
            .build();
    final AuthorRepository authors;

    public Author getAuthorById(Long id) {
        var authorEntity = this.authors.findById(id);
        return Optional.ofNullable(authorEntity)
                .map(MAPPER)
                .orElseThrow(() -> new AuthorNotFoundException(id));
    }

    public List<Author> getAuthorByIdIn(Collection<Long> ids) {
        var authorEntities = this.authors.findByIdIn(new ArrayList<>(ids));
        return authorEntities.stream().map(MAPPER).toList();
    }
}

