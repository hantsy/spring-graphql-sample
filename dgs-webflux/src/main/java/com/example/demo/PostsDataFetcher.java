package com.example.demo;

import com.netflix.graphql.dgs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.validation.Valid;

@DgsComponent
@RequiredArgsConstructor
@Slf4j
@Validated
public class PostsDataFetcher {
    private final PostService postService;
    private final AuthorService authorService;

    @DgsQuery
    public Flux<Post> allPosts() {
        return this.postService.getAllPosts();
    }

    @DgsQuery
    public Mono<Post> postById(@InputArgument String postId) {
        return this.postService.getPostById(postId)
                .switchIfEmpty(Mono.error(new PostNotFoundException(postId)));
    }

    @DgsData(parentType = "Post", field = "author")
    public Mono<Author> author(DgsDataFetchingEnvironment dfe) {
        Post post = dfe.getSource();
        String authorId = post.getAuthorId();
        return this.authorService.getAuthorById(authorId)
                .switchIfEmpty(Mono.error(new AuthorNotFoundException(authorId)));
    }

    @DgsMutation
    public Mono<Post> createPost(@InputArgument("createPostInput") @Valid CreatePostInput input) {
        return this.postService.createPost(input);
    }

    @DgsMutation
    public Mono<Comment> addComment(@InputArgument("commentInput") @Valid CommentInput input) {
        Mono<Comment> comment = this.postService.addComment(input)
                .doOnNext(
                        c -> sink.emitNext(c, Sinks.EmitFailureHandler.FAIL_FAST)
                );

        return comment;
    }

    private final Sinks.Many<Comment> sink = Sinks.many().replay().limit(100);

    @DgsSubscription
    Publisher<Comment> commentAdded() {
        return sink.asFlux();
    }
}
