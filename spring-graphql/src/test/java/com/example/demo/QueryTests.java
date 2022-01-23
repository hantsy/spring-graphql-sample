package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureWebGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.WebGraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebGraphQlTester
@Slf4j
public class QueryTests {

    @Autowired
    WebGraphQlTester graphQlTester;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void allPosts() {

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
        graphQlTester.query(allPosts)
                .execute()
                .path("allPosts[*].title")
                .entityList(String.class)
                .satisfies(titles -> assertThat(titles).anyMatch(s -> s.startsWith("DGS POST")));
    }
}
