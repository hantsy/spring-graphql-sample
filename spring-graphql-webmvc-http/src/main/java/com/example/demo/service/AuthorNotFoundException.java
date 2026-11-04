package com.example.demo.service;

public class AuthorNotFoundException extends RuntimeException {
    public AuthorNotFoundException(String id) {
        super("Author: " + id + " was not found.");
    }
}
