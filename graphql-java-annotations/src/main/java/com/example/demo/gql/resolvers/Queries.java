package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.kickstart.graphql.annotations.GraphQLQueryResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@GraphQLQueryResolver
public class Queries implements ApplicationContextAware {
    static PostService postService;

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Return post by Id.")
    public static Post postById(String postId) {
        return postService.getPostById(postId);
    }

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Return posts.")
    public static List<Post> allPosts() {
        return postService.getAllPosts();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        postService = applicationContext.getBean(PostService.class);
    }
}
