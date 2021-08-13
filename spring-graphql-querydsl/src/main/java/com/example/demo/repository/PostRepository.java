package com.example.demo.repository;

import com.example.demo.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.graphql.data.GraphQlRepository;

import java.util.UUID;

@GraphQlRepository()
public interface PostRepository extends JpaRepository<Post, UUID> ,
        QuerydslPredicateExecutor<CommentRepository> {

}