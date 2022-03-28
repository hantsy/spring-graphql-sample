package com.example.demo;

import com.example.demo.gql.datafetchers.PostsDataFetcherController;
import com.example.demo.gql.types.Post;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostNotFoundException;
import com.example.demo.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@GraphQlTest(controllers = {PostsDataFetcherController.class})
@Slf4j
class QueryTests {

    @Autowired
    GraphQlTester graphQlTester;

    @MockBean
    PostService postService;

    @MockBean
    AuthorService authorService;

    @Test
    void allPosts() {
        when(postService.getAllPosts()).thenReturn(Flux.just(
                Post.builder().id(UUID.randomUUID().toString())
                        .title("test title")
                        .content("test content")
                        .build(),
                Post.builder().id(UUID.randomUUID().toString())
                        .title("test title2")
                        .content("test content2")
                        .build()
        ));
        var query = " { allPosts { title content }}";
        graphQlTester.document(query)
                .execute()
                .path("data.allPosts[*].title")
                .entityList(String.class).contains("test title", "test title2");

        verify(postService, times(1)).getAllPosts();
        verifyNoMoreInteractions(postService);
    }

    @Test
    void postById() {
        when(postService.getPostById(anyString())).thenReturn(Mono.just(
                Post.builder().id(UUID.randomUUID().toString())
                        .title("test title")
                        .content("test content")
                        .build()
        ));
        var query = "query postById($postId:String!){ postById(postId:$postId) { title content }}";

        graphQlTester.document(query)
                .variable("postId", "test")
                .execute()
                .path("data.postById.title")
                .entity(String.class).isEqualTo("test title");

        verify(postService, times(1)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }

    @Test
    void postById_notFound() {
        when(postService.getPostById(anyString())).thenThrow(new PostNotFoundException("test"));
        var query = "query postById($postId:String!){ postById(postId:$postId) { title content }}";
        graphQlTester.document(query)
                .variable("postId", "test")
                .execute()
                .errors()
                .satisfy(it -> {
                    assertThat(it).isNotEmpty();
                    assertThat(it.get(0).getMessage()).isEqualTo("Post: test was not found.");
                });

        verify(postService, times(1)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }

}
