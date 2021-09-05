package com.example.demo;

import com.example.demo.gql.types.Post;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostNotFoundException;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest()
//@SpringBootTest(classes = {DgsWebFluxAutoConfiguration.class, PostsDataFetcher.class})
@Slf4j
class QueryTests {

    @Autowired
    DgsReactiveQueryExecutor dgsQueryExecutor;

    @MockBean
    PostService postService;

    @MockBean
    AuthorService authorService;

    // too many dependencies not available in `DgsWebFluxAutoConfiguration`
    // @MockBean
    // graphql.schema.GraphQLSchema graphQLSchema;
    // @MockBean
    // DgsSchemaProvider schemaProvider;

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
        when(postService.getPostById(anyString())).thenReturn(Mono.just(
                Post.builder().id(UUID.randomUUID().toString())
                        .title("test title")
                        .content("test content")
                        .build()
        ));
        Mono<String> title = dgsQueryExecutor.executeAndExtractJsonPath(
                "query postById($postId:String!){ postById(postId:$postId) { title content }}",
                "data.postById.title",
                Map.of("postId", "test")
        );

        StepVerifier.create(title)
                .consumeNextWith(it -> assertThat(it).isEqualTo("test title"))
                .verifyComplete();

        verify(postService, times(1)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }

    @Test
    void postById_notFound() {
        when(postService.getPostById(anyString())).thenThrow(new PostNotFoundException("test"));
        var executeResult = dgsQueryExecutor.execute(
                "query postById($postId:String!){ postById(postId:$postId) { title content }}",
                Map.of("postId", "test")
        );

        StepVerifier.create(executeResult)
                .consumeNextWith(it -> {
                    log.debug("execute result: {}", it);
                    assertThat(it.getErrors()).isNotEmpty();
                    assertThat(it.getErrors().get(0).getMessage()).isEqualTo("Post: test was not found.");
                })
                .verifyComplete();

        verify(postService, times(1)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }

}
