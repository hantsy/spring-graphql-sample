package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import graphql.kickstart.tools.GraphQLQueryResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class AllPostQueryResolver implements GraphQLQueryResolver {
    final PostService postService;

    public List<Post> allPosts() {
        return this.postService.getAllPosts();
    }

}
