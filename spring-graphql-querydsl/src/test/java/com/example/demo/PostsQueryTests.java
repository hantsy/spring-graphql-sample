package com.example.demo;

import com.example.demo.model.Comment;
import com.example.demo.model.Post;
import com.example.demo.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.boot.test.tester.AutoConfigureWebGraphQlTester;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest()
@AutoConfigureWebGraphQlTester
@Slf4j
public class PostsQueryTests {

    @Autowired
    GraphQlTester graphQlTester;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PostRepository postRepository;

    @BeforeEach
    public void setup() {
        var comments = Set.of(
                Comment.builder().id(UUID.randomUUID()).content("Comment content").createdAt(LocalDateTime.now()).build(),
                Comment.builder().id(UUID.randomUUID()).content("Comment content 2").createdAt(LocalDateTime.now()).build()
        );

        var post = Post.builder()
                .id(UUID.randomUUID())
                .title("Post")
                .content("Content of post one")
                .comments(comments)
                .createdAt(LocalDateTime.now())
                .build();

        var post2 = Post.builder()
                .id(UUID.randomUUID())
                .title("Post 2")
                .content("Content of post two")
                .createdAt(LocalDateTime.now())
                .build();

        when(this.postRepository.findAll(any(Predicate.class))).thenReturn(List.of(post, post2));
        when(this.postRepository.findOne(any(Predicate.class))).thenReturn(Optional.of(post));
    }

    @Test
    void allPosts() {
        var allPosts = """
                query allPosts{
                   posts{
                     id
                     title
                     content
                     comments{ id content }
                   }
                }""";
        graphQlTester.query(allPosts)
                .execute()
                .path("posts[*].title")
                .entityList(String.class)
                .satisfies(titles -> assertThat(titles).anyMatch(s -> s.startsWith("POST")));
    }

    @Test
    void postById() {
        var postById = """
                query postById($id: ID!){
                   post(id:$id){
                     id
                     title
                     content
                     comments{ id content }
                   }
                 }""";
        graphQlTester.query(postById)
                .variable("id", UUID.randomUUID())
                .execute()
                .path("post.title")
                .entity(String.class).satisfies(s -> assertThat(s).startsWith("POST"));
    }

}
