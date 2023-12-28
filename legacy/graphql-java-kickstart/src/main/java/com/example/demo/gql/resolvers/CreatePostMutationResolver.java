package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.service.PostService;
import graphql.kickstart.tools.GraphQLMutationResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class CreatePostMutationResolver implements GraphQLMutationResolver {
    final PostService postService;
    public UUID createPost(CreatePostInput input) {
        return this.postService.createPost(input);
    }
}
