package com.example.demo.gql.dataloaders;

import com.example.demo.gql.types.Author;
import com.example.demo.service.AuthorService;
import lombok.RequiredArgsConstructor;
import org.dataloader.BatchLoader;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor
public class AuthorsDataLoader implements BatchLoader<String, Author> {
    final AuthorService authorService;

    @Override
    public CompletionStage<List<Author>> load(List<String> keys) {
        return CompletableFuture.supplyAsync(() ->
                this.authorService.getAuthorByIdIn(keys)
        );
    }
}
