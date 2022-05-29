package com.example.demo;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class DemoApplicationTests {

    @LocalServerPort
    int port;

    private TestRestTemplate testTemplate;

    @BeforeEach
    void beforeEach() {
        this.testTemplate = new TestRestTemplate(new RestTemplateBuilder().rootUri("http://localhost:" + port));
    }

    @Test
    void allPosts() {
        var allPosts = Map.of("query", """
                query posts{
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
                 }
                  """);
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        var entity = new HttpEntity(allPosts, headers);
        var response = testTemplate.exchange("/graphql", HttpMethod.POST, entity, String.class, Map.of());
        assertNotNull(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var json = response.getBody();
        log.info("response body is: {}", json);
        List<String> titles = JsonPath.read(json, "$.data.allPosts[*].title");
        assertThat(titles).anyMatch(s -> s.startsWith("DGS POST"));
    }


}
