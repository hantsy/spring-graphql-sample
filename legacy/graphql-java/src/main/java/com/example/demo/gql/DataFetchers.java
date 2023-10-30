package com.example.demo.gql;

import com.example.demo.gql.types.Author;
import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor
@Slf4j
@Component
public class DataFetchers {
    private final PostService posts;
    private final ObjectMapper jacksonMapper;

    public DataFetcher<List<Post>> getAllPosts() {
        return (DataFetchingEnvironment dfe) -> this.posts.getAllPosts();
    }

    public DataFetcher<Post> getPostById() {
        return (DataFetchingEnvironment dfe) -> {
            String postId = dfe.getArgument("postId");
            return posts.getPostById(postId);
        };
    }

    public DataFetcher<UUID> createPost() {
        return (DataFetchingEnvironment dfe) -> {
            var postInputArg = dfe.getArgument("createPostInput");
            var input = jacksonMapper.convertValue(postInputArg, CreatePostInput.class);
            return this.posts.createPost(input);
        };
    }

    public DataFetcher<CompletionStage<List<Comment>>> commentsOfPost() {
        return (DataFetchingEnvironment dfe) -> {
            DataLoader<String, List<Comment>> dataLoader = dfe.getDataLoader("commentsLoader");
            Post post = dfe.getSource();
            //log.info("source: {}", post);
            return dataLoader.load(post.getId());
        };
    }

    public DataFetcher<CompletionStage<Author>> authorOfPost() {
        return (DataFetchingEnvironment dfe) -> {
            DataLoader<String, Author> dataLoader = dfe.getDataLoader("authorsLoader");
            Post post = dfe.getSource();
            //log.info("source: {}", post);
            return dataLoader.load(post.getAuthorId());
        };
    }
}
