package com.example.demo;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
class DemoApplicationTests {

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    @Test
    public void testFileUpload() {
        var query = "mutation upload($file:Upload!){ upload(file:$file)	}";
        var result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
                query,
                "data.upload",
                Map.of("file", new MockMultipartFile("test", "test.txt", "text/plain", "test content".getBytes())),
                Boolean.class
        );

        assertThat(result).isTrue();
    }

}
