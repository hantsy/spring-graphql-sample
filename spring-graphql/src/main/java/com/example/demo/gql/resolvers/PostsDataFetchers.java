package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class PostsDataFetchers {
    final PostService postService;
    final ObjectMapper objectMapper;

    public DataFetcher<Post> postById() {
        return dfe -> {
            String postId = dfe.getArgument("postId");
            return postService.getPostById(postId);
        };
    }

    public DataFetcher<List<Post>> allPosts() {
        return dfe -> postService.getAllPosts();
    }

    public DataFetcher<UUID> createPost() {
        return dfe -> {
            var input = dfe.getArgument("createPostInput");
            var createPostInput = objectMapper.convertValue(input, CreatePostInput.class);
            return postService.createPost(createPostInput);
        };
    }

    public AuthorDataFetcher authorOfPost() {
        return new AuthorDataFetcher();
    }

    public CommentsDataFetcher commentsOfPost() {
        return new CommentsDataFetcher();
    }

}
