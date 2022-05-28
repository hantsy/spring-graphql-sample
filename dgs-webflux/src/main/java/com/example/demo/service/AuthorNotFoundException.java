package com.example.demo.service;

public class AuthorNotFoundException extends RuntimeException {
    public AuthorNotFoundException(Long id) {
        super("Author: " + id + " was not found.");
    }
}
