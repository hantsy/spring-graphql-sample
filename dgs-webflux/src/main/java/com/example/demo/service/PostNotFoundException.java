package com.example.demo.service;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(String id) {
        super("Post: " + id + " was not found.");
    }
}
