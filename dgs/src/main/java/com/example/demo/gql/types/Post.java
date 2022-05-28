package com.example.demo.gql.types;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@ToString
@Slf4j
public class Post {
    Long id;
    String title;
    String content;
    @Builder.Default
    PostStatus status = PostStatus.DRAFT;
    @Builder.Default
    List<Comment> comments = new ArrayList<>();
    Long authorId;
    Author author;

    public void addComment(Comment comment) {
        comment.setPostId(this.id);
        this.comments.add(comment);
        log.info("post comments: {}", this.comments);
    }
}
