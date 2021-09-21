package com.example.demo;

import com.example.demo.gql.types.Post;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostsQueryTests {

    @LocalServerPort
    int port = 0;

    @Autowired
    TestRestTemplate testTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void allPosts() {
        var query = Map.of("query", """
                    query posts{
                    allPosts{
                        id
                        title
                        content
                        comments{ id content }
                        author{ id name email }
                    }
                }""".trim()
        );

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var httpEntity = new HttpEntity(query, headers);
        var response = testTemplate.exchange("http://localhost:" + port + "/graphql",
                HttpMethod.POST,
                httpEntity,
                new ParameterizedTypeReference<Map<String, Map<String, Object>>>() {
                }
        );
        assertNotNull(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var postsObject = response.getBody().get("data").get("allPosts");
        var posts = objectMapper.convertValue(postsObject, Post[].class);
        assertThat(posts).anyMatch(s -> s.getTitle().startsWith("DGS POST"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        public TestRestTemplate testRestTemplate() {
            return new TestRestTemplate(new RestTemplateBuilder().rootUri("http://localhost:8080"));
        }
    }

}
