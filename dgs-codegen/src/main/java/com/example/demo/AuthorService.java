package com.example.demo;

import com.example.demo.gql.types.Author;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuthorService {

    static List<Author> STORE = new ArrayList<>();

    public void init(List<Author> data) {
        STORE.clear();
        STORE.addAll(data);
    }

    Optional<Author> getAuthorById(String id) {
        return STORE.stream().filter(p -> p.getId().equals(id)).findFirst();
    }
}

