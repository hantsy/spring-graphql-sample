package com.example.demo.gql.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Author {
  private String id;

  private String name;

  private String email;

  private LocalDateTime createdAt;

  @Builder.Default
  private List<Post> posts = new ArrayList<>();

}
