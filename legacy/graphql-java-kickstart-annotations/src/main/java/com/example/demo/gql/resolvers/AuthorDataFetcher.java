package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.Author;
import com.example.demo.gql.types.Post;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;

import java.util.concurrent.CompletableFuture;

public class AuthorDataFetcher implements DataFetcher<CompletableFuture<Author>> {

    @Override
    public CompletableFuture<Author> get(DataFetchingEnvironment dfe) throws Exception {
        DataLoader<String, Author> dataLoader = dfe.getDataLoader("authorsLoader");
        Post post = dfe.getSource();
        return dataLoader.load(post.getAuthorId());
    }
}
