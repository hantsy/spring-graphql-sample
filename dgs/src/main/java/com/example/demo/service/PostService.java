package com.example.demo.service;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.gql.types.PostStatus;
import com.example.demo.model.CommentEntity;
import com.example.demo.model.PostEntity;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PostService {
    private static final Function<PostEntity, Post> POST_MAPPER = p -> Post.builder()
            .id(p.id())
            .title(p.title())
            .content(p.content())
            .status(PostStatus.valueOf(p.status()))
            .authorId(p.authorId())
            .build();
    public static final Function<CommentEntity, Comment> COMMENT_MAPPER = c -> Comment.builder()
            .id(c.id())
            .content(c.content())
            .postId(c.postId())
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

    public Post getPostById(Long id) {
        var postEntity = this.posts.findById(id);
        return Optional.ofNullable(postEntity)
                .map(POST_MAPPER)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    List<Post> getPostsByAuthorId(Long id) {
        return this.posts.findByAuthorId(id)
                .stream()
                .map(POST_MAPPER)
                .toList();
    }

    public Long createPost(CreatePostInput postInput) {
        var authorId = this.authors.findAll().get(0).id();
        Long id = this.posts.create(postInput.getTitle(), postInput.getContent(), "DRAFT", authorId);
        return id;
    }

    public List<Comment> getCommentsByPostId(Long id) {
        return this.comments.findByPostId(id)
                .stream()
                .map(COMMENT_MAPPER)
                .toList();
    }

    public List<Comment> getCommentsByPostIdIn(Set<Long> ids) {
        return this.comments.findByPostIdIn(new ArrayList<>(ids))
                .stream()
                .map(COMMENT_MAPPER)
                .toList();
    }
}
