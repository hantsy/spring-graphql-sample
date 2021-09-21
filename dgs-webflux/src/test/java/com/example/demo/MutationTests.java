package com.example.demo;

import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest()
@Slf4j
class MutationTests {

    @Autowired
    DgsReactiveQueryExecutor dgsQueryExecutor;

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

        Mono<String> titleMono = dgsQueryExecutor.executeAndExtractJsonPath(
                "mutation createPost($input: CreatePostInput!){createPost(createPostInput:$input){id, title, content}}",
                "data.createPost.title",
                Map.of("input", Map.of(
                                "title", "test title",
                                "content", "test content"
                        )
                )
        );

        StepVerifier.create(titleMono)
                .consumeNextWith(it -> assertThat(it).isEqualTo("test title"))
                .verifyComplete();

        verify(postService, times(1)).createPost(any(CreatePostInput.class));
        verify(postService, times(1)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }

}
