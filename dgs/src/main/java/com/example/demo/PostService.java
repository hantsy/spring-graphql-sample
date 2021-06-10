package com.example.demo;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PostService {
    static List<Post> STORE = new ArrayList<>();

    public void init(List<Post> data) {
        STORE.clear();
        STORE.addAll(data);
    }

    List<Post> getAllPosts() {
        return STORE;
    }

    Optional<Post> getPostById(String id) {
        return STORE.stream().filter(p -> p.id.equals(id)).findFirst();
    }

    List<Post> getPostsByAuthorId(String id) {
        return STORE.stream().filter(p -> p.authorId.equals(id)).toList();
    }

    Post createPost(CreatePostInput postInput) {
        var data = Post.builder().id(UUID.randomUUID().toString())
                .title(postInput.getTitle())
                .content(postInput.getContent())
                .build();
        STORE.add(data);
        return data;
    }

    Comment addComment(CommentInput commentInput) {
        String postId = commentInput.getPostId();
        return getPostById(postId)
                .map(
                        p -> {
                            var comment = Comment.builder()
                                    .id(UUID.randomUUID().toString())
                                    .postId(postId)
                                    .content(commentInput.getContent())
                                    .build();
                            p.getComments().add(comment);
                            return comment;
                        }
                )
                .orElseThrow(() -> new PostNotFoundException(postId));
    }
}
