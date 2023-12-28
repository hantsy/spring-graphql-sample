package com.example.demo.gql.types;

import graphql.annotations.annotationTypes.*;
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

  @GraphQLID
  @GraphQLField
  @GraphQLNonNull
  private String id;

  @GraphQLField
  @GraphQLNonNull
  private String name;

  @GraphQLField
  @GraphQLNonNull
  private String email;

  @GraphQLField
  private LocalDateTime createdAt;

  @Builder.Default
  @GraphQLField
  private List<Post> posts = new ArrayList<>();

}
