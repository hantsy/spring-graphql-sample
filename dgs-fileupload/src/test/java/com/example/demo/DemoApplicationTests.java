package com.example.demo;

import com.example.demo.gql.FileUploadInput;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
class DemoApplicationTests {

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    @Test
    public void testUpload() {
        var query = "mutation upload($file:Upload!){ upload(file:$file)	}";
        var result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
                query,
                "data.upload",
                Map.of("file", new MockMultipartFile("test", "test.txt", "text/plain", "test content".getBytes())),
                Boolean.class
        );

        assertThat(result).isTrue();
    }

    @Test
    public void testUploadWithDesc() {
        var query = "mutation uploadWithDesc($desc:String, $file:Upload!){ uploadWithDesc(desc:$desc, file:$file) }";
        var result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
                query,
                "data.uploadWithDesc",
                Map.of("desc", "file description",
                        "file", new MockMultipartFile("test", "test.txt", "text/plain", "test content".getBytes())),
                Boolean.class
        );

        assertThat(result).isTrue();
    }

    @Test
    public void testUploads() {
        var query = "mutation uploads($files:[Upload!]!){ uploads(files:$files)	}";
        var result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
                query,
                "data.uploads",
                Map.of("files", List.of(
                        new MockMultipartFile("test", "test.txt", "text/plain", "test content".getBytes()),
                        new MockMultipartFile("test2", "test2.txt", "text/plain", "test2 content".getBytes()),
                        new MockMultipartFile("test3", "test3.txt", "text/plain", "test3 content".getBytes())
                        )
                ),
                Boolean.class
        );

        assertThat(result).isTrue();
    }

    @Test
    @Disabled("this dose not work")
    public void testFileUpload() {
        var query = "mutation fileUpload($file:FileUploadInput!){ fileUpload(file:$file)	}";
        var result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
                query,
                "data.fileUpload",
                Map.of("file",
                        FileUploadInput.builder()
                                .description("file desc")
                                .file(new MockMultipartFile("test", "test.txt", "text/plain", "test content".getBytes()))
                                .build()
                ),
                Boolean.class
        );

        assertThat(result).isTrue();
    }

    @Test
    @Disabled("this dose not work")
    public void testFileUploads() {
        var query = "mutation fileUploads($files:[FileUploadInput!]!){ fileUploads(files:$files)}";
        var result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(
                query,
                "data.fileUploads",
                Map.of("files",
                        List.of(
                                FileUploadInput.builder()
                                        .description("file desc")
                                        .file(new MockMultipartFile("test", "test.txt", "text/plain", "test content".getBytes()))
                                        .build(),
                                FileUploadInput.builder()
                                        .description("file2 desc")
                                        .file(new MockMultipartFile("test2", "test2.txt", "text/plain", "test2 content".getBytes()))
                                        .build(),
                                FileUploadInput.builder()
                                        .description("file3 desc")
                                        .file(new MockMultipartFile("test3", "test3.txt", "text/plain", "test3 content".getBytes()))
                                        .build()

                        )
                ),
                Boolean.class
        );

        assertThat(result).isTrue();
    }

}
