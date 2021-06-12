package com.example.demo;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AuthorService {

    static Map<String, Author> STORE = new HashMap<>();

    public void init(List<Author> data) {
        STORE.clear();
        data.forEach(d -> STORE.put(d.id, d));
    }

    Mono<Author> getAuthorById(String id) {
        return Mono.justOrEmpty(STORE.get(id));
    }
}

