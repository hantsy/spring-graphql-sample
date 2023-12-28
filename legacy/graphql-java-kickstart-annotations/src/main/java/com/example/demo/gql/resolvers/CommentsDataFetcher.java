package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommentsDataFetcher implements DataFetcher<CompletableFuture<List<Comment>>> {

    @Override
    public CompletableFuture<List<Comment>> get(DataFetchingEnvironment dfe) throws Exception {
        DataLoader<String, List<Comment>> dataLoader = dfe.getDataLoader("commentsLoader");
        Post post = dfe.getSource();
        return dataLoader.load(post.getId());
    }
}
