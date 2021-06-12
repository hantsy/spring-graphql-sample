package com.example.demo.service;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.CommentInput;
import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.model.PostEntity;
import com.example.demo.model.PostStatus;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PostService {
    private static final Function<PostEntity, Post> POST_MAPPER = p -> Post.newBuilder()
            .id(p.id().toString())
            .title(p.title())
            .content(p.content())
            .authorId(p.authorId().toString())
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

    public UUID createPost(CreatePostInput postInput) {
        UUID id = this.posts.create(postInput.getTitle(), postInput.getContent(), PostStatus.DRAFT, null);
        return id;
    }


    public List<Comment> getCommentsByPostId(String id) {
        return this.comments.findByPostId(UUID.fromString(id))
                .stream()
                .map(c -> Comment.newBuilder()
                        .id(c.id().toString())
                        .content(c.content())
                        .postId(c.postId().toString())
                        .build()
                )
                .toList();
    }
}
