package com.example.demo.service;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.CommentInput;
import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.model.CommentEntity;
import com.example.demo.model.PostEntity;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    public static final Function<PostEntity, Post> MAPPER = entity -> Post.builder()
            .id(entity.id().toString())
            .title(entity.title())
            .content(entity.content())
            .authorId(entity.authorId().toString())
            .build();

    public static final Function<CommentEntity, Comment> COMMENT_MAPPER = entity -> Comment.builder()
            .id(entity.id().toString())
            .content(entity.content())
            .postId(entity.postId().toString())
            .build();

    private final PostRepository posts;
    private final AuthorRepository authors;
    private final CommentRepository comments;

    public Flux<Post> getAllPosts() {
        return this.posts.findAll()
                .map(MAPPER);
    }

    public Mono<Post> getPostById(String id) {
        return this.posts.findById(UUID.fromString(id))
                .map(MAPPER)
                .switchIfEmpty(Mono.error(new PostNotFoundException(id)));
    }

    public Flux<Post> getPostsByAuthorId(String id) {
        return this.posts.findByAuthorId(UUID.fromString(id))
                .map(MAPPER);
    }

    public Mono<Post> createPost(CreatePostInput postInput) {
        var author = this.authors.findAll().last();//get an existing id
        return author.flatMap(a -> this.posts.create(postInput.getTitle(), postInput.getContent(), "DRAFT", a.id()))
                .flatMap(id -> this.posts.findById(id).map(MAPPER));
    }

    public Flux<Comment> getCommentsByPostIdIn(List<String> ids) {
        var uuids = ids.stream().map(UUID::fromString).toList();
        return this.comments.findByPostIdIn(uuids)
                .map(COMMENT_MAPPER);
    }

    public Mono<Comment> addComment(CommentInput commentInput) {
        String postId = commentInput.getPostId();
        return this.posts.findById(UUID.fromString(postId))
                .flatMap(p -> this.comments.create(commentInput.getContent(), UUID.fromString(postId)))
                .flatMap(id -> this.comments.findById(id).map(COMMENT_MAPPER))
                .doOnNext(c -> {
                    log.debug("emitting comment: {}", c);
                    sink.emitNext(c, Sinks.EmitFailureHandler.FAIL_FAST);
                })
                .switchIfEmpty(Mono.error(new PostNotFoundException(postId)));
    }

    public Mono<Comment> getCommentById(String id) {
        return this.comments.findById(UUID.fromString(id)).map(COMMENT_MAPPER);
    }

    private final Sinks.Many<Comment> sink = Sinks.many().replay().latest();

    public Publisher<Comment> commentAdded() {
        return sink.asFlux();
    }
}


