package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.CommentInput;
import com.example.demo.service.PostService;
import graphql.kickstart.tools.GraphQLMutationResolver;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.servlet.http.Part;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class AddCommentMutationResolver implements GraphQLMutationResolver {
    final PostService postService;

    public UUID addComment(CommentInput commentInput) {
        return this.postService.addComment(commentInput);
    }
}
