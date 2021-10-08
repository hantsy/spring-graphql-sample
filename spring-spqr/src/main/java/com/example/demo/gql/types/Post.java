package com.example.demo.gql.types;

import io.leangen.graphql.annotations.GraphQLId;
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
    @GraphQLId
    String id;
    String title;
    String content;
    @Builder.Default
    PostStatus status = PostStatus.DRAFT;
    @Builder.Default
    List<Comment> comments = new ArrayList<>();
    String authorId;
    Author author;

    public void addComment(Comment comment) {
        comment.setPostId(this.id);
        this.comments.add(comment);
        log.info("post comments: {}", this.comments);
    }
}
