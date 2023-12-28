package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class CommentsResolver implements GraphQLResolver<Post> {

    public CompletableFuture<List<Comment>> comments(Post post, DataFetchingEnvironment dfe) {
        DataLoader<String, List<Comment>> dataLoader = dfe.getDataLoader("commentsLoader");
        //Post post = dfe.getSource();
        return dataLoader.load(post.getId());
    }
}
