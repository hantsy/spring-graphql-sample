package com.example.demo;

import com.netflix.graphql.dgs.*;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@DgsComponent
@RequiredArgsConstructor
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
    public Mono<Post> createPost(@InputArgument("createPostInput") CreatePostInput input) {
        return this.postService.createPost(input);
    }
}
