package com.example.demo.gql.types;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor()
public class CreatePostInput {

    @NotBlank
    @Size(min = 5, max = 100)
    private String title;

    @NotBlank
    private String content;
}
