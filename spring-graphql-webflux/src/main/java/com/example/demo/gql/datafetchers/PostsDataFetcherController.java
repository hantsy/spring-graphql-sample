package com.example.demo.gql.datafetchers;

import com.example.demo.gql.types.*;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@Validated
public class PostsDataFetcherController {
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

    @BatchMapping
    public Mono<Map<Post, List<Comment>>> comments(List<Post> posts) {
        var keys = posts.stream().map(Post::getId).toList();
        return this.postService.getCommentsByPostIdIn(keys)
                .collectMultimap(Comment::getPostId)
                .map(m -> {
                    var result = new HashMap<Post, List<Comment>>();
                    m.keySet().forEach(k -> {
                        var postKey = posts.stream().filter(post -> post.getId().equals(k)).toList().get(0);
                        result.put(postKey, new ArrayList<>(m.get(k)));
                    });
                    return result;
                });
    }

    @BatchMapping
    public Flux<Author> author(List<Post> posts) {
        var keys = posts.stream().map(Post::getAuthorId).toList();
        var authorByIds = this.authorService.getAuthorByIdIn(keys);
        return Flux.fromIterable(posts).flatMap(p -> authorByIds.filter(a -> a.getId().equals(p.getAuthorId())));
    }

    @MutationMapping
    public Mono<Post> createPost(@Argument("createPostInput") @Valid CreatePostInput input) {
        return this.postService.createPost(input);
    }

    @MutationMapping
    public Mono<Comment> addComment(@Argument("commentInput") @Valid CommentInput input) {
        return this.postService.addComment(input);
    }

    @SubscriptionMapping
    Publisher<Comment> commentAdded() {
        return this.postService.commentAdded();
    }
}
