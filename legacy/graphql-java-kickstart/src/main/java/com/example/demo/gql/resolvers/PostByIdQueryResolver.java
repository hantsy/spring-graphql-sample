package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import graphql.kickstart.tools.GraphQLQueryResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PostByIdQueryResolver implements GraphQLQueryResolver {
    final PostService postService;

    public Post postById(String postId) {
        return this.postService.getPostById(postId);
    }
}
