package com.example.demo.service

import java.util.UUID

class AuthorNotFoundException(id: UUID) : RuntimeException("Author: $id was not found.")