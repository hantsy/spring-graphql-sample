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

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    public static final Function<PostEntity, Post> MAPPER = entity -> Post.builder()
            .id(entity.id())
            .title(entity.title())
            .content(entity.content())
            .authorId(entity.authorId())
            .build();

    public static final Function<CommentEntity, Comment> COMMENT_MAPPER = entity -> Comment.builder()
            .id(entity.id())
            .content(entity.content())
            .postId(entity.postId())
            .build();

    private final PostRepository posts;
    private final AuthorRepository authors;
    private final CommentRepository comments;

    public Flux<Post> getAllPosts() {
        return this.posts.findAll()
                .map(MAPPER);
    }

    public Mono<Post> getPostById(Long id) {
        return this.posts.findById(id)
                .map(MAPPER)
                .switchIfEmpty(Mono.error(new PostNotFoundException(id)));
    }

    public Flux<Post> getPostsByAuthorId(Long id) {
        return this.posts.findByAuthorId(id)
                .map(MAPPER);
    }

    public Mono<Long> createPost(CreatePostInput postInput) {
        var author = this.authors.findAll().last();//get an existing id
        return author.flatMap(a -> this.posts.create(postInput.getTitle(), postInput.getContent(), "DRAFT", a.id()));
    }

    public Flux<Comment> getCommentsByPostIdIn(Set<Long> ids) {
        return this.comments.findByPostIdIn(new ArrayList<>(ids))
                .map(COMMENT_MAPPER);
    }

    public Mono<Comment> addComment(CommentInput commentInput) {
        var postId = Long.valueOf(commentInput.getPostId());
        return this.posts.findById(postId)
                .flatMap(p -> this.comments.create(commentInput.getContent(), postId))
                .flatMap(id -> comments.findById(id))
                .map(COMMENT_MAPPER)
                .doOnNext(c -> sink.emitNext(c, Sinks.EmitFailureHandler.FAIL_FAST))
                .switchIfEmpty(Mono.error(new PostNotFoundException(postId)));
    }

    private final Sinks.Many<Comment> sink = Sinks.many().replay().latest();

    public Flux<Comment> commentAdded() {
        return sink.asFlux();
    }
    public Mono<Comment> getCommentById(Long id) {
        return this.comments.findById(id).map(COMMENT_MAPPER);
    }
}
