package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphql.spring.boot.test.GraphQLResponse;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import com.jayway.jsonpath.JsonPath;
import graphql.kickstart.autoconfigure.web.servlet.GraphQLWebsocketAutoConfiguration;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnableAutoConfiguration(exclude = GraphQLWebsocketAutoConfiguration.class)
class DemoApplicationTests {

  @Autowired
  GraphQLTestTemplate testTemplate;

  @Autowired
  TestRestTemplate restTemplate;

  @Autowired
  ObjectMapper objectMapper;

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

  // see: https://github.com/jaydenseric/graphql-multipart-request-spec/issues/6
  @SneakyThrows
  @Test
  public void uploadFile() {
    var query = """
        mutation upload($file:Upload!){
            upload(file:$file)
        }
        """;
    var variables = new HashMap<>();
    variables.put("file", null);

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    var body = new LinkedMultiValueMap();
    body.add("map", Map.of("file0", List.of("variables.file")));
    body.add("file0", new FileSystemResource(new ClassPathResource("/test.txt").getFile()));
    body.add("operations", Map.of("query", query, "variables", variables));
    var entity = new HttpEntity(body, headers);
    var responseJson = restTemplate.postForEntity("/graphql", entity, String.class);

    assertThat(responseJson.getStatusCode()).isEqualTo(HttpStatus.OK);
    Boolean read = JsonPath.read(responseJson.getBody(), "$.data.upload");
    assertThat(read).isTrue();
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    public TestRestTemplate testRestTemplate() {
      return new TestRestTemplate(new RestTemplateBuilder()
          .defaultMessageConverters().additionalMessageConverters(new ByteArrayHttpMessageConverter())
          .rootUri("http://localhost:8080"));
    }
  }

}
