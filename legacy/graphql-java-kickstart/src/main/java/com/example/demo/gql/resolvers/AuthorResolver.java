package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.Author;
import com.example.demo.gql.types.Post;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class AuthorResolver implements GraphQLResolver<Post> {

    public CompletableFuture<Author> author(Post post, DataFetchingEnvironment dfe) {
        DataLoader<String, Author> dataLoader = dfe.getDataLoader("authorsLoader");
      //  Post post = dfe.getSource();
        return dataLoader.load(post.getAuthorId());
    }

}
