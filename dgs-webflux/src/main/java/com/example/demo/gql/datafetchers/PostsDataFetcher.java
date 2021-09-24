package com.example.demo.gql.datafetchers;

import com.example.demo.gql.dataloaders.CommentsDataLoader;
import com.example.demo.gql.types.*;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.reactivestreams.Publisher;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.validation.Valid;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        return this.postService.getPostById(postId);
    }

    @DgsData(parentType = "Post", field = "comments")
    public CompletableFuture<List<Comment>> comments(DgsDataFetchingEnvironment dfe) {
        DataLoader<String, List<Comment>> dataLoader = dfe.getDataLoader(CommentsDataLoader.class);
        Post post = dfe.getSource();
        return dataLoader.load(post.getId());
    }

    @DgsData(parentType = "Post", field = "author")
    public CompletableFuture<Author> author(DgsDataFetchingEnvironment dfe) {
        DataLoader<String, Author> dataLoader = dfe.getDataLoader("authorsLoader");
        Post post = dfe.getSource();
        return dataLoader.load(post.getAuthorId());
    }

    @DgsMutation
    public Mono<Post> createPost(@InputArgument("createPostInput") @Valid CreatePostInput input) {
        return this.postService.createPost(input).flatMap(uuid -> this.postService.getPostById(uuid.toString()));
    }

    @DgsMutation
    public Mono<Comment> addComment(@InputArgument("commentInput") @Valid CommentInput input) {
        Mono<Comment> comment = this.postService.addComment(input)
                .flatMap(id -> this.postService.getCommentById(id.toString()))
                .doOnNext(c -> sink.emitNext(c, Sinks.EmitFailureHandler.FAIL_FAST));

        return comment;
    }

    private final Sinks.Many<Comment> sink = Sinks.many().replay().latest();

    @DgsSubscription
    Publisher<Comment> commentAdded() {
        return sink.asFlux();
    }
}
