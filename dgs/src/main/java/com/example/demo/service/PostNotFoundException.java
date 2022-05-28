package com.example.demo.service;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(Long id) {
        super("Post: #" + id + " was not found.");
    }
}
