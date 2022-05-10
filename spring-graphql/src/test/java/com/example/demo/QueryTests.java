package com.example.demo;

import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class QueryTests {

    @Autowired
    HttpGraphQlTester graphQlTester;

    @MockBean
    PostService postService;

    @Autowired
    ObjectMapper objectMapper;

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
                     author{ id name email }
                     comments{ id content }
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
