package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.boot.test.tester.AutoConfigureGraphQlTester;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
//@AutoConfigureMockMvc  // to make subscription work.
@AutoConfigureGraphQlTester
@Slf4j
public class DemoApplicationTests {

    @Autowired
    GraphQlTester graphQlTester;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void allPosts() {
        var allPosts = """
                query allPosts{
                   posts{
                     id
                     title
                     content
                     comments{ id content }
                   }
                }""";
        graphQlTester.query(allPosts)
                .execute()
                .path("posts[*].title")
                .entityList(String.class)
                .satisfies(titles -> assertThat(titles).anyMatch(s -> s.startsWith("DGS POST")));
    }

    @Test
    void postById() {
        var postById = """
                query postById($id: ID!){
                   post(id:$id){
                     id
                     title
                     content
                     comments{ id content }
                   }
                 }""";
        graphQlTester.query(postById)
                .variable("id", UUID.randomUUID())
                .execute()
                .path("post.title").valueDoesNotExist();
    }


}
