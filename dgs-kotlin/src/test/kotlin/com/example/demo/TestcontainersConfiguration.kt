package com.example.demo

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun postgreSQLContainer(): PostgreSQLContainer = PostgreSQLContainer("postgres:16").withInitScript("init.sql")

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> = GenericContainer(DockerImageName.parse("redis").withTag("latest"))
        .withExposedPorts(6379)

    @Bean
    @ServiceConnection
    fun mongoDBContainer(): MongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo").withTag("latest"))
}
