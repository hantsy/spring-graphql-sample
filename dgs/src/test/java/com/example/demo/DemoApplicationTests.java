package com.example.demo;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
//@SpringBootTest(classes = {DgsAutoConfiguration.class, PostsDataFetcher.class})
class DemoApplicationTests {

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    @Test
    void allPosts() {
        List<String> titles = dgsQueryExecutor.executeAndExtractJsonPath(
                " { allPosts { title content }}",
                "data.allPosts[*].title");

        assertThat(titles).anyMatch(s -> s.startsWith("Dgs post"));
    }

    @Test
    void createComments() {
        //https://stackoverflow.com/questions/43082298/server-sent-event-client-example-in-spring
        List<String> titles = dgsQueryExecutor.executeAndExtractJsonPath(
                " { allPosts { title content }}",
                "data.allPosts[*].title");

        assertThat(titles).anyMatch(s -> s.startsWith("Dgs post"));
    }
}
