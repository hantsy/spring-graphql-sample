package com.example.demo;

import com.example.demo.gql.datafetchers.PostsDataFetcher;
import com.example.demo.gql.types.Post;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostNotFoundException;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor;
import com.netflix.graphql.dgs.webflux.autoconfiguration.DgsWebFluxAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {QueryTests.QueryTestsConfig.class})
@Slf4j
class QueryTests {

    @Autowired
    DgsReactiveQueryExecutor dgsQueryExecutor;

    @MockBean
    PostService postService;

    @MockBean
    AuthorService authorService;

    @Configuration
    @Import(PostsDataFetcher.class)
    @ImportAutoConfiguration(classes = {
            DgsWebFluxAutoConfiguration.class,
            WebFluxAutoConfiguration.class,
            DgsAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    static class QueryTestsConfig {

    }

    @Test
    void allPosts() {
        when(postService.getAllPosts()).thenReturn(Flux.just(
                Post.builder().id(1L)
                        .title("test title")
                        .content("test content")
                        .build(),
                Post.builder().id(2L)
                        .title("test title2")
                        .content("test content2")
                        .build()
        ));
        Mono<List<String>> titles = dgsQueryExecutor.executeAndExtractJsonPath(
                " { allPosts { title content }}",
                "data.allPosts[*].title");

        StepVerifier.create(titles)
                .consumeNextWith(it -> assertThat(it).containsAll(List.of("test title", "test title2")))
                .verifyComplete();

        verify(postService, times(1)).getAllPosts();
        verifyNoMoreInteractions(postService);
    }

    @Test
    void postById() {
        when(postService.getPostById(anyLong())).thenReturn(Mono.just(
                Post.builder().id(1L)
                        .title("test title")
                        .content("test content")
                        .build()
        ));
        Mono<String> title = dgsQueryExecutor.executeAndExtractJsonPath(
                "query postById($postId:ID!){ postById(postId:$postId) { title content }}",
                "data.postById.title",
                Map.of("postId", 1L)
        );

        StepVerifier.create(title)
                .consumeNextWith(it -> assertThat(it).isEqualTo("test title"))
                .verifyComplete();

        verify(postService, times(1)).getPostById(anyLong());
        verifyNoMoreInteractions(postService);
    }

    @Test
    void postById_notFound() {
        when(postService.getPostById(anyLong())).thenThrow(new PostNotFoundException(1L));
        var executeResult = dgsQueryExecutor.execute(
                "query postById($postId: ID!){ postById(postId:$postId) { title content }}",
                Map.of("postId", 1L)
        );

        StepVerifier.create(executeResult)
                .consumeNextWith(it -> {
                    log.debug("execute result: {}", it);
                    assertThat(it.getErrors()).isNotEmpty();
                    assertThat(it.getErrors().get(0).getMessage()).contains("Post: 1 was not found.");
                })
                .verifyComplete();

        verify(postService, times(1)).getPostById(anyLong());
        verifyNoMoreInteractions(postService);
    }

}
