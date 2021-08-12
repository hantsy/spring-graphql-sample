package com.example.demo.gql;

import com.example.demo.gql.types.*;
import com.example.demo.service.PostService;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.reactivestreams.Publisher;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@GraphQlController
@RequiredArgsConstructor
@Validated
public class PostsDataFetchingController {//spring boot stater created an `AnnotatedDataFetchersConfigurer` to register data fetchers from `@GraphQlController` clazz
    private final PostService postService;

    @QueryMapping
    public List<Post> allPosts() {
        return this.postService.getAllPosts();
    }

    @QueryMapping
    public Post postById(@Argument("postId") String id) {
        return this.postService.getPostById(id);
    }

    @MutationMapping
    public UUID createPost(@Argument("createPostInput") @Validated CreatePostInput input) {
        return postService.createPost(input);
    }

    @MutationMapping
    public UUID addComment(@Argument CommentInput commentInput) {
        return this.postService.addComment(commentInput);
    }

    @SubscriptionMapping
    public Publisher<Comment> commentAdded() {
        return this.postService.commentAdded();
    }

    // resolving fields
    @SchemaMapping(field = "comments")
    public CompletionStage<List<Comment>> commentsOfPost(Post post, DataFetchingEnvironment dfe) {
        DataLoader<String, List<Comment>> dataLoader = dfe.getDataLoader("commentsLoader");
        return dataLoader.load(post.getId());
    }

    @SchemaMapping(field = "author")
    public CompletionStage<Author> authorOfPost(Post post, DataFetchingEnvironment dfe) {
        DataLoader<String, Author> dataLoader = dfe.getDataLoader("authorsLoader");
        return dataLoader.load(post.getAuthorId());
    }
}
