package com.example.demo.gql.datafetchers;

import com.example.demo.gql.types.*;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.reactivestreams.Publisher;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.validation.Valid;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Controller
@RequiredArgsConstructor
@Slf4j
@Validated
public class PostsDataFetcher {
    private final PostService postService;
    private final AuthorService authorService;

    @QueryMapping
    public Flux<Post> allPosts() {
        return this.postService.getAllPosts();
    }

    @QueryMapping
    public Mono<Post> postById(@Argument String postId) {
        return this.postService.getPostById(postId);
    }

    @SchemaMapping
    public CompletableFuture<List<Comment>> comments(Post post, DataFetchingEnvironment dfe) {
        DataLoader<String, List<Comment>> dataLoader = dfe.getDataLoader("commentsLoader");
        return dataLoader.load(post.getId());
    }

    @SchemaMapping
    public CompletableFuture<Author> author(Post post, DataFetchingEnvironment dfe) {
        DataLoader<String, Author> dataLoader = dfe.getDataLoader("authorsLoader");
        return dataLoader.load(post.getAuthorId());
    }

    @MutationMapping
    public Mono<Post> createPost(@Argument("createPostInput") @Valid CreatePostInput input) {
        return this.postService.createPost(input).flatMap(uuid -> this.postService.getPostById(uuid.toString()));
    }

    @MutationMapping
    public Mono<Comment> addComment(@Argument("commentInput") @Valid CommentInput input) {
        Mono<Comment> comment = this.postService.addComment(input)
                .flatMap(id -> this.postService.getCommentById(id.toString())
                        .doOnNext(c -> {
                            log.debug("emitting comment: {}", c);
                            sink.emitNext(c, Sinks.EmitFailureHandler.FAIL_FAST);
                        })
                );

        return comment;
    }

    private final Sinks.Many<Comment> sink = Sinks.many().replay().latest();

    @SubscriptionMapping
    Publisher<Comment> commentAdded() {
        return sink.asFlux();
    }
}
