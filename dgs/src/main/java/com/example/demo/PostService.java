package com.example.demo;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PostService {
    static Map<String, Post> STORE = new HashMap<>();

    public void init(List<Post> data) {
        STORE.clear();
        data.forEach(d -> STORE.put(d.id, d));
    }

    List<Post> getAllPosts() {
        return new ArrayList<>(STORE.values());
    }

    Optional<Post> getPostById(String id) {
        return Optional.ofNullable(STORE.get(id));
    }

    List<Post> getPostsByAuthorId(String id) {
        return STORE.values().stream().filter(p -> p.authorId.equals(id)).toList();
    }

    Post createPost(CreatePostInput postInput) {
        var data = Post.builder().id(UUID.randomUUID().toString())
                .title(postInput.getTitle())
                .content(postInput.getContent())
                .build();
        STORE.put(data.id, data);
        return data;
    }

    Comment addComment(CommentInput commentInput) {
        String postId = commentInput.getPostId();
        var p = STORE.get(postId);
        if (p == null) {
            throw new PostNotFoundException(postId);
        }

        var comment = Comment.builder()
                .id(UUID.randomUUID().toString())
                .postId(postId)
                .content(commentInput.getContent())
                .build();
        p.addComment(comment);
        return comment;
    }
}
