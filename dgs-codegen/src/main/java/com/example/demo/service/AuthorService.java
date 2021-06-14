package com.example.demo.service;

import com.example.demo.gql.types.Author;
import com.example.demo.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorService {
    final AuthorRepository authors;

    public Author getAuthorById(String id) {
        var authorEntity = this.authors.findById(UUID.fromString(id));
        return Optional.ofNullable(authorEntity)
                .map(a -> Author.newBuilder()
                        .id(a.id().toString())
                        .name(a.name())
                        .email(a.email())
                        .createdAt(a.createdAt())
                        .build()
                )
                .orElseThrow(() -> new AuthorNotFoundException(id));
    }
}

