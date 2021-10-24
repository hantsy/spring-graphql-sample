package com.example.demo.service;

import com.example.demo.gql.types.*;
import com.example.demo.model.CommentEntity;
import com.example.demo.model.PostEntity;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PostService {
    private static final Function<PostEntity, Post> POST_MAPPER = p -> Post.builder()
            .id(p.id().toString())
            .title(p.title())
            .content(p.content())
            .status(PostStatus.valueOf(p.status()))
            .createdAt(p.createdAt())
            .authorId(p.authorId().toString())
            .build();
    public static final Function<CommentEntity, Comment> COMMENT_MAPPER = c -> Comment.builder()
            .id(c.id().toString())
            .content(c.content())
            .createdAt(c.createdAt())
            .postId(c.postId().toString())
            .build();
    final PostRepository posts;
    final CommentRepository comments;
    final AuthorRepository authors;


    public List<Post> getAllPosts() {
        return this.posts.findAll()
                .stream()
                .map(POST_MAPPER)
                .toList();
    }

    public Post getPostById(String id) {
        var postEntity = this.posts.findById(UUID.fromString(id));
        return Optional.ofNullable(postEntity)
                .map(POST_MAPPER)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    List<Post> getPostsByAuthorId(String id) {
        return this.posts.findByAuthorId(UUID.fromString(id))
                .stream()
                .map(POST_MAPPER)
                .toList();
    }

    public Post createPost(CreatePostInput postInput) {
        Validate.notNull(postInput, "CreatePostInput can not be null");
        Validate.notEmpty(postInput.getTitle(), "CreatePostInput.title can not be empty");
        // Use a hard code user id here.
        // In a real world application, the author is the current user which can be fetched from Spring security context.
        var authorId = this.authors.findAll().get(0).id();
        UUID id = this.posts.create(postInput.getTitle(), postInput.getContent(), "DRAFT", authorId);
        var entity = this.posts.findById(id);
        return Optional.ofNullable(entity)
                .map(POST_MAPPER)
                .orElseThrow(() -> new PostNotFoundException(id.toString()));
    }

    public List<Comment> getCommentsByPostId(String id) {
        return this.comments.findByPostId(UUID.fromString(id))
                .stream()
                .map(COMMENT_MAPPER)
                .toList();
    }

    public List<Comment> getCommentsByPostIdIn(Set<String> ids) {
        var uuids = ids.stream().map(UUID::fromString).toList();
        return this.comments.findByPostIdIn(uuids)
                .stream()
                .map(COMMENT_MAPPER)
                .toList();
    }

    public Comment addComment(CommentInput input) {
        UUID id = this.comments.create(input.getContent(), UUID.fromString(input.getPostId()));
        Comment commentById = this.getCommentById(id.toString());
        sink.emitNext(commentById, Sinks.EmitFailureHandler.FAIL_FAST);
        return commentById;
    }

    // sink of `commentAdded` event
    private Sinks.Many<Comment> sink = Sinks.many().replay().latest();

    // subscription for `commentAdded`
    public Publisher<Comment> commentAdded() {
        return sink.asFlux();
    }

    public Comment getCommentById(String id) {
        var commentById = this.comments.findById(UUID.fromString(id));
        return COMMENT_MAPPER.apply(commentById);
    }

}
