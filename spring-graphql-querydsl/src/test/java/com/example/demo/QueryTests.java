package com.example.demo;

import com.example.demo.model.Comment;
import com.example.demo.model.Post;
import com.example.demo.model.QPost;
import com.example.demo.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureWebGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest()
@AutoConfigureWebGraphQlTester
@Slf4j
public class QueryTests {

    @Autowired
    GraphQlTester graphQlTester;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PostRepository postRepository;

    @BeforeEach
    public void setup() {

    }

    @Test
    void allPosts() {
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

        when(this.postRepository.findBy(any(Predicate.class), any())).thenReturn(List.of(post, post2));
        var allPosts = """
                query allPosts{
                   posts{
                     id
                     title
                     content
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

        var post = Post.builder()
                .id(UUID.randomUUID())
                .title("Post")
                .content("Content of post one")
                .createdAt(LocalDateTime.now())
                .build();
        when(this.postRepository.findBy(any(Predicate.class), any())).thenReturn(Optional.of(post));
        var postById = """
                query postById($id: ID!){
                   post(id:$id){
                     id
                     title
                     content
                   }
                 }""".trim();
        var id = UUID.randomUUID();
        graphQlTester.query(postById)
                .variable("id", id)
                .execute()
                .path("post.title")
                .entity(String.class).satisfies(s -> assertThat(s).startsWith("POST"));

        ArgumentCaptor<Predicate> predicateCaptor = ArgumentCaptor.forClass(Predicate.class);
        verify(this.postRepository).findBy(predicateCaptor.capture(), any());

        Predicate predicate = predicateCaptor.getValue();
        assertThat(predicate).isEqualTo(QPost.post.id.eq(id));
    }

}
