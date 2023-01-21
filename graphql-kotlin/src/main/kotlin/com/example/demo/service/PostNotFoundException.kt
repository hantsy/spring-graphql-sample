package com.example.demo.service

import java.util.*

class PostNotFoundException(id: UUID) : RuntimeException("Post: $id was not found.")

