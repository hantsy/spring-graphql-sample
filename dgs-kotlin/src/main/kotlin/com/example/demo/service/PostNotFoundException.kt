package com.example.demo.service

import java.util.UUID

class PostNotFoundException(id: UUID) : RuntimeException("Post: $id was not found.")

