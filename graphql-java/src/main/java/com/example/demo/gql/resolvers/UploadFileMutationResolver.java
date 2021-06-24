package com.example.demo.gql.resolvers;

import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.service.PostService;
import graphql.kickstart.tools.GraphQLMutationResolver;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.Part;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class UploadFileMutationResolver implements GraphQLMutationResolver {
    @SneakyThrows
    public Boolean upload(Part file) {
        log.info("file name: {}", file.getName());
        log.info("content type: {}", file.getContentType());
        log.info("submitted file name: {}", file.getSubmittedFileName());
        file.write(file.getSubmittedFileName());

        return true;
    }
}
