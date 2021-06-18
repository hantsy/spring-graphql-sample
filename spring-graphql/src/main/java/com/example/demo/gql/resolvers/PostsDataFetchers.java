package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class PostsDataFetchers {
    final PostService postService;

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
            CreatePostInput input = dfe.getArgument("createPostInput");
            return postService.createPost(input);
        };
    }

    public AuthorDataFetcher authorOfPost(){
        return new AuthorDataFetcher();
    }

    public CommentsDataFetcher commentsOfPost() {
        return new CommentsDataFetcher();
    }

}
