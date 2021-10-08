package com.example.demo.service;

import com.example.demo.gql.types.*;
import com.example.demo.model.CommentEntity;
import com.example.demo.model.PostEntity;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
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
            .authorId(p.authorId().toString())
            .build();
    public static final Function<CommentEntity, Comment> COMMENT_MAPPER = c -> Comment.builder()
            .id(c.id().toString())
            .content(c.content())
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
        var authorId = this.authors.findAll().get(0).id();
        UUID id = this.posts.create(postInput.getTitle(), postInput.getContent(), "DRAFT", authorId);
        PostEntity entity = this.posts.findById(id);
        return POST_MAPPER.apply(entity);
    }

    public List<Comment> getCommentsByPostId(String id) {
        return this.comments.findByPostId(UUID.fromString(id))
                .stream()
                .map(COMMENT_MAPPER)
                .toList();
    }

    public List<Comment> getCommentsByPostIdIn(Set<String> ids) {
        var uuids  = ids.stream().map(UUID::fromString).toList();
        return this.comments.findByPostIdIn(uuids)
                .stream()
                .map(COMMENT_MAPPER)
                .toList();
    }

    public Comment addComment(String id, CommentInput input) {
        var postEntity = this.posts.findById(UUID.fromString(id));
        return Optional.ofNullable(postEntity)
                .map(entity ->{
                     UUID commentId = this.comments.create(input.getContent(), entity.id());
                     return this.comments.findById(commentId);
                })
                .map(commentEntity -> {
                    Comment comment = COMMENT_MAPPER.apply(commentEntity);
                    sinks.emitNext(comment, Sinks.EmitFailureHandler.FAIL_FAST);
                    return comment;
                })
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    private final Sinks.Many<Comment> sinks = Sinks.many().replay().latest();

    public Publisher<Comment> commentAdded() {
        return sinks.asFlux();
    }
}
