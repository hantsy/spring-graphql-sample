package com.example.demo;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class PostService {
    static Map<String, Post> STORE = new HashMap<>();

    public void init(List<Post> data) {
        STORE.clear();
        data.forEach(d -> STORE.put(d.id, d));
    }

    Flux<Post> getAllPosts() {
        return Flux.fromIterable(STORE.values());
    }

    Mono<Post> getPostById(String id) {
        return Mono.justOrEmpty(STORE.get(id));
    }

    Flux<Post> getPostsByAuthorId(String id) {
        return Flux.fromIterable(STORE.values().stream().filter(p -> p.authorId.equals(id)).toList());
    }

    Mono<Post> createPost(CreatePostInput postInput) {
        var data = Post.builder().id(UUID.randomUUID().toString())
                .title(postInput.getTitle())
                .content(postInput.getContent())
                .build();
        STORE.put(data.id, data);
        return Mono.just(data);
    }

    Mono<Comment> addComment(CommentInput commentInput) {
        String postId = commentInput.getPostId();
        var p = STORE.get(postId);

        if (p != null) {
            var comment = Comment.builder()
                    .id(UUID.randomUUID().toString())
                    .postId(postId)
                    .content(commentInput.getContent())
                    .build();
            p.getComments().add(comment);
            return Mono.just(comment);
        } else {
            return Mono.error(new PostNotFoundException(postId));
        }
    }
}
