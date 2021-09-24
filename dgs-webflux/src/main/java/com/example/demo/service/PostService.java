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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
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

    public Mono<UUID> createPost(CreatePostInput postInput) {
        var author = this.authors.findAll().last();//get an existing id
        return author.flatMap(a -> this.posts.create(postInput.getTitle(), postInput.getContent(), "DRAFT", a.id()));
    }

    public Flux<Comment> getCommentsByPostIdIn(Set<String> ids) {
        var uuids = ids.stream().map(UUID::fromString).toList();
        return this.comments.findByPostIdIn(uuids)
                .map(COMMENT_MAPPER);
    }

    public Mono<UUID> addComment(CommentInput commentInput) {
        String postId = commentInput.getPostId();
        return this.posts.findById(UUID.fromString(postId))
                .flatMap(p -> this.comments.create(commentInput.getContent(), UUID.fromString(postId)))
                .switchIfEmpty(Mono.error(new PostNotFoundException(postId)));
    }

    public Mono<Comment> getCommentById(String id) {
        return this.comments.findById(UUID.fromString(id)).map(COMMENT_MAPPER);
    }
}
