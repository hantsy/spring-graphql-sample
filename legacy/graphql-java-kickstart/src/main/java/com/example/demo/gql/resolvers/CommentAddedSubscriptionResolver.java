package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.Comment;
import com.example.demo.service.PostService;
import graphql.kickstart.tools.GraphQLSubscriptionResolver;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentAddedSubscriptionResolver implements GraphQLSubscriptionResolver {
    final PostService postService;
    Publisher<Comment> commentAdded(){
        return postService.commentAdded();
    }
}
