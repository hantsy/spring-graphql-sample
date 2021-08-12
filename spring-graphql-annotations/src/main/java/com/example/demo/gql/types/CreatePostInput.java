package com.example.demo.gql.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor()
public class CreatePostInput {

    private String title;

    private String content;
}
