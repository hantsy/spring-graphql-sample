package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.boot.test.tester.AutoConfigureGraphQlTester;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureGraphQlTester
public class DemoApplicationTests {

    @Autowired
    GraphQlTester graphQlTester;

    @Test
    void allPosts() {
        var allPosts = """
                query posts{
                   allPosts{
                     id
                     title
                     content
                   }
                 }
                  """;
        graphQlTester.query(allPosts)
                .execute()
                .path("allPosts[*].title")
                .entityList(String.class)
                .satisfies(titles -> assertThat(titles).anyMatch(s -> s.startsWith("DGS POST")));
    }

}
