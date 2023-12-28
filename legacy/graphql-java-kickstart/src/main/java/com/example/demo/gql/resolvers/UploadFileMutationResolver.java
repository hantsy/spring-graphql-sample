package com.example.demo.gql.resolvers;

import graphql.kickstart.tools.GraphQLMutationResolver;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.servlet.http.Part;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Slf4j
@Component
public class UploadFileMutationResolver implements GraphQLMutationResolver {
    @SneakyThrows
    public Boolean upload(Part file) {
        log.info("file name: {}", file.getName());
        log.info("content type: {}", file.getContentType());
        log.info("submitted file name: {}", file.getSubmittedFileName());
        log.info("file content size: {}", file.getSize());
        String content = StreamUtils.copyToString(file.getInputStream(), StandardCharsets.UTF_8);
        log.info("file content : {}", content);
        return true;
    }
}
