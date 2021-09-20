package com.example.demo;

import com.graphql.spring.boot.test.GraphQLResponse;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class DemoApplicationTests {

    @Autowired
    GraphQLTestTemplate testTemplate;

    @Test
    void allPosts() {
        var allPosts = """
                {"query": "query posts{
                   allPosts{
                     id
                     title
                     content
                     comments{
                       id
                       content
                     }
                     author{
                       id
                       name
                       email
                     }
                   }
                 }"
                 }
                  """;
        GraphQLResponse response = testTemplate.post(allPosts);
        assertNotNull(response);
        assertThat(response.isOk()).isTrue();
        List<String> titles = response.getList("$.data.allPosts[*].title", String.class);
        assertThat(titles).anyMatch(s -> s.startsWith("DGS POST"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        public TestRestTemplate testRestTemplate() {
            return new TestRestTemplate(new RestTemplateBuilder().rootUri("http://localhost:8080"));
        }
    }

}
