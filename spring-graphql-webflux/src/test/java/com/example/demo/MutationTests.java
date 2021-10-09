package com.example.demo;

import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.boot.test.tester.AutoConfigureGraphQlTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@SpringBootTest()
@AutoConfigureGraphQlTester
@Slf4j
class MutationTests {

    @Autowired
    GraphQlTester graphQlTester;

    @MockBean
    PostService postService;

    @MockBean
    AuthorService authorService;

    @Test
    void createPosts() {
        when(postService.createPost(any(CreatePostInput.class))).thenReturn(Mono.just(UUID.randomUUID()));
        when(postService.getPostById(anyString())).thenReturn(Mono.just(
                Post.builder().id(UUID.randomUUID().toString())
                        .title("test title")
                        .content("test content")
                        .build()
        ));

        var query = "mutation createPost($input: CreatePostInput!){createPost(createPostInput:$input){id, title, content}}";
        graphQlTester.query(query)
                .variable("input", Map.of(
                        "title", "test title",
                        "content", "test content"
                ))
                .execute()
                .path("data.createPost.title")
                .entity(String.class)
                .isEqualTo("test title");

        verify(postService, times(1)).createPost(any(CreatePostInput.class));
        verify(postService, times(1)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }

}
