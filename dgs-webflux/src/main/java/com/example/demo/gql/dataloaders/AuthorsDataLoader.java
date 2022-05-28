package com.example.demo.gql.dataloaders;

import com.example.demo.gql.types.Author;
import com.example.demo.service.AuthorService;
import com.netflix.graphql.dgs.DgsDataLoader;
import lombok.RequiredArgsConstructor;
import org.dataloader.BatchLoader;

import java.util.List;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor
@DgsDataLoader(name = "authorsLoader")
public class AuthorsDataLoader implements BatchLoader<Long, Author> {
    final AuthorService authorService;

    @Override
    public CompletionStage<List<Author>> load(List<Long> keys) {
        return this.authorService.getAuthorByIdIn(keys).collectList().toFuture();
    }
}
