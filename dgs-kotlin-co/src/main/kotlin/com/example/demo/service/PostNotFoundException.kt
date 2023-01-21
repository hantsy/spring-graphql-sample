package com.example.demo.service

class PostNotFoundException(id: String) : RuntimeException("Post: $id was not found.")