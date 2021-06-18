package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.service.PostService;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.kickstart.graphql.annotations.GraphQLMutationResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@GraphQLMutationResolver
public class Mutations implements ApplicationContextAware {
    static PostService postService;

    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Creates a new post.")
    public static UUID createPost(@GraphQLName("createPostInput") CreatePostInput input) {
        return postService.createPost(input);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        postService = applicationContext.getBean(PostService.class);
    }
}
