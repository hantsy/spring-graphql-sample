package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.CommentInput;
import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
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

    public DataFetcher<UUID> addComment() {
        return (DataFetchingEnvironment dfe) -> {
            var commentInputArg = dfe.getArgument("commentInput");
            var input = objectMapper.convertValue(commentInputArg, CommentInput.class);
            UUID id = this.postService.addComment(input);

            Comment commentById = this.postService.getCommentById(id.toString());
            sink.emitNext(commentById, Sinks.EmitFailureHandler.FAIL_FAST);

            return id;
        };
    }

    private Sinks.Many<Comment> sink = Sinks.many().replay().latest();

    public DataFetcher<Publisher<Comment>> commentAdded() {
        return (DataFetchingEnvironment dfe) -> {
            log.info("connect to `commentAdded`");
            return sink.asFlux();
        };
    }
}
