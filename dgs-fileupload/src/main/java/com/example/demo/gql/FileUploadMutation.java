package com.example.demo.gql;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.InputArgument;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@DgsComponent
@RequiredArgsConstructor
@Slf4j
public class FileUploadMutation {


    @SneakyThrows
    @DgsMutation
    public Boolean upload(@InputArgument("file") MultipartFile file) {
        log.info("file name: {}", file.getName());
        log.info("file original file name: {}", file.getOriginalFilename());
        log.info("file content type: {}", file.getContentType());
        var fileContent = new String(file.getBytes());
        log.info("file content: {}", fileContent);
        return true;
    }
}
