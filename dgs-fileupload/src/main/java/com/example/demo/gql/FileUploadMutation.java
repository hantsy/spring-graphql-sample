package com.example.demo.gql;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.InputArgument;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@DgsComponent
@RequiredArgsConstructor
@Slf4j
public class FileUploadMutation {

    @DgsMutation
    public Boolean upload(@InputArgument("file") MultipartFile file) {
        printFileInfo(file);
        return true;
    }

    @DgsMutation
    public Boolean uploadWithDesc(@InputArgument("desc") String desc, @InputArgument("file") MultipartFile file) {
        log.info("description: {}", desc);
        printFileInfo(file);
        return true;
    }


    @DgsMutation
    public Boolean uploads(@InputArgument("files") List<MultipartFile> files) {
        files.forEach(file -> printFileInfo(file));
        return true;
    }

    @DgsMutation
    public Boolean fileUpload(@InputArgument("file") FileUploadInput file) {
        log.info("description: {}", file.getDescription());
        printFileInfo(file.getFile());
        return true;
    }

    @DgsMutation
    public Boolean fileUploads(@InputArgument("files") List<FileUploadInput> files) {
        files.forEach(file -> {
            log.info("description: {}", file.getDescription());
            printFileInfo(file.getFile());
        });
        return true;
    }

    @SneakyThrows
    private void printFileInfo(MultipartFile file) {
        log.info("file name: {}", file.getName());
        log.info("file original file name: {}", file.getOriginalFilename());
        log.info("file content type: {}", file.getContentType());
        String fileContent = StreamUtils.copyToString(file.getInputStream(), StandardCharsets.UTF_8);
        log.info("file content: {}", fileContent);
    }
}
