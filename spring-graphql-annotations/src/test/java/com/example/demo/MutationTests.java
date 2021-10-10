package com.example.demo;

import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.boot.test.tester.AutoConfigureGraphQlTester;
import org.springframework.graphql.test.tester.GraphQlTester;

import javax.validation.ConstraintViolationException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
//@AutoConfigureMockMvc  // to make subscription work.
@AutoConfigureGraphQlTester
@Slf4j
public class MutationTests {

    @Autowired
    GraphQlTester graphQlTester;

    @MockBean
    PostService postService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testCreatePost() {
        when(postService.createPost(any(CreatePostInput.class))).thenReturn(UUID.randomUUID());
        when(postService.getPostById(anyString())).thenReturn(
                Post.builder().id(UUID.randomUUID().toString())
                        .title("test title")
                        .content("test content")
                        .build()
        );

        var creatPost = """
                mutation createPost($createPostInput: CreatePostInput!){
                   createPost(createPostInput:$createPostInput){
                   id 
                   title
                   content
                   }
                }""".trim();

        String TITLE = "test title";//valid
        graphQlTester.query(creatPost)
                .variable("createPostInput",
                        Map.of(
                                "title", TITLE,
                                "content", "content of my post"
                        ))
                .execute()
                .path("createPost.title").entity(String.class).isEqualTo(TITLE);

        verify(postService, times(1)).createPost(any(CreatePostInput.class));
        verify(postService, times(1)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }


    @Test
    @Disabled
    // see: https://github.com/spring-projects/spring-graphql/issues/110
    void testCreatePost_FailedValidation() {
        when(postService.createPost(any(CreatePostInput.class))).thenReturn(UUID.randomUUID());
        when(postService.getPostById(anyString())).thenReturn(
                Post.builder().id(UUID.randomUUID().toString())
                        .title("test title")
                        .content("test content")
                        .build()
        );
        var creatPost = """
                mutation createPost($createPostInput: CreatePostInput!){
                   createPost(createPostInput:$createPostInput){
                   id
                   title
                   content
                   }
                }""".trim();

        String TITLE = "test";//not valid
        assertThatThrownBy(() -> graphQlTester.query(creatPost)
                .variable("createPostInput",
                        Map.of(
                                "title", TITLE,
                                "content", "content of my post"
                        )
                )
                .executeAndVerify()
        ).hasCauseInstanceOf(ConstraintViolationException.class);

        verify(postService, times(0)).createPost(any(CreatePostInput.class));
        verify(postService, times(0)).getPostById(anyString());
        verifyNoMoreInteractions(postService);
    }

}
