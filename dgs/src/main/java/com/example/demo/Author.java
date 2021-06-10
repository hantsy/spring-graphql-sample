package com.example.demo;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@ToString
public class Author {
    String id;
    String name;
    String email;
    List<Post> posts = new ArrayList<>();
}
