package com.example.demo.gql.types;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class Author {
    String id;
    String name;
    String email;
}
