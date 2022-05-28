package com.example.demo;

import com.example.demo.gql.types.Post;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureHttpGraphQlTester
public class QueryTests {

    @Autowired
    HttpGraphQlTester graphQlTester;

    @MockBean
    PostService postService;

    @MockBean
    AuthorService authorService;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
    }

    @Test
    void allPosts() {
        when(this.postService.getAllPosts())
                .thenReturn(
                        List.of(
                                Post.builder().id("1").title("Dgs Post 1").build(),
                                Post.builder().id("2").title("Dgs Post 2").build()
                        )
                );

        //the `author` and `comments` requires a `DataLoader` which currently only works in a web environment.
        var allPosts = """
                query posts{
                   allPosts{
                     id
                     title
                     content
                   }
                 }""";
        graphQlTester.document(allPosts)
                .execute()
                .path("allPosts[*].title")
                .entityList(String.class)
                .satisfies(titles -> assertThat(titles).anyMatch(s -> s.startsWith("DGS POST")));

        verify(this.postService, times(1)).getAllPosts();
    }
}
