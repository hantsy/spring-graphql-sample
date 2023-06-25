package com.example.demo;

import com.example.demo.gql.PostController;
import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.gql.types.Post;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@GraphQlTest(PostController.class)
@Slf4j
public class MutationTests {

    @Autowired
    GraphQlTester graphQlTester;

    @MockBean
    PostService postService;

    @MockBean
    AuthorService authorService;

    @Test
    void testCreatePost() {
        var data = Post.builder().id(UUID.randomUUID().toString())
                .title("test title")
                .content("test content")
                .build();
        when(postService.createPost(any(CreatePostInput.class))).thenReturn(data);
        var creatPost = """
                mutation createPost($createPostInput: CreatePostInput!){
                   createPost(createPostInput:$createPostInput){
                   id
                   title
                   content
                   }
                }""".trim();

        String TITLE = "test title";//valid
        graphQlTester.document(creatPost)
                .variable("createPostInput",
                        Map.of(
                                "title", TITLE,
                                "content", "content of my post"
                        ))
                .execute()
                .path("createPost.title").entity(String.class).isEqualTo(TITLE);

        verify(postService, times(1)).createPost(any(CreatePostInput.class));
        verifyNoMoreInteractions(postService);
    }


    @Test
        // @Disabled
        // see: https://github.com/spring-projects/spring-graphql/issues/110
    void testCreatePost_FailedValidation() {
        var data = Post.builder().id(UUID.randomUUID().toString())
                .title("test title")
                .content("test content")
                .build();
        when(postService.createPost(any(CreatePostInput.class))).thenReturn(data);
        var creatPost = """
                mutation createPost($createPostInput: CreatePostInput!){
                   createPost(createPostInput:$createPostInput){
                   id
                   title
                   content
                   }
                }""".trim();

        String TITLE = "test";//not valid
        graphQlTester.document(creatPost)
                .variable(
                        "createPostInput",
                        Map.of(
                                "title", TITLE,
                                "content", "content of my post"
                        )
                )
                .execute()
                .errors().expect(it -> it.getErrorType() == ErrorType.INTERNAL_ERROR);

// see: https://stackoverflow.com/questions/74704427/assert-unhandled-exception-in-spring-graphql
//        assertThatThrownBy(() -> graphQlTester.document(creatPost)
//                .variable("createPostInput",
//                        Map.of(
//                                "title", TITLE,
//                                "content", "content of my post"
//                        )
//                )
//                .executeAndVerify()
//        ).hasRootCauseInstanceOf(ConstraintViolationException.class);

        verify(postService, times(0)).createPost(any(CreatePostInput.class));
        verifyNoMoreInteractions(postService);
    }

}
