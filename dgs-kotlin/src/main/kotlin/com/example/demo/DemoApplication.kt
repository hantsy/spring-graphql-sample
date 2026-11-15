package com.example.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@SpringBootApplication
@EnableJdbcAuditing
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}
