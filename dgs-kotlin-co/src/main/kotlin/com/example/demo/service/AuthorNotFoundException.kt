package com.example.demo.service

class AuthorNotFoundException(id: String) : RuntimeException("Author: $id was not found.")

