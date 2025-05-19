import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.config.ApiVersion.Companion.KOTLIN_2_0
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
	id("org.springframework.boot") version "3.4.4"
	id("io.spring.dependency-management") version "1.1.7"

	kotlin("jvm") version "2.1.21"
	kotlin("plugin.spring") version "2.1.20"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

extra["coroutinesVersion"]="1.8.1"
extra["mockkVersion"]="1.13.11"
extra["springmockkVersion"]="4.0.2"
extra["kotestVersion"]="5.9.0"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-graphql")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-rsocket")

	//r2dbc
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.postgresql:r2dbc-postgresql")

	//kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	//kotlin coroutines
	developmentOnly("org.jetbrains.kotlinx:kotlinx-coroutines-debug:${property("coroutinesVersion")}")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${property("coroutinesVersion")}")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${property("coroutinesVersion")}")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${property("coroutinesVersion")}")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${property("coroutinesVersion")}")

	//test
	testImplementation("org.springframework.graphql:spring-graphql-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(module = "mockito-core")
	}
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:${property("coroutinesVersion")}")
	testImplementation("io.mockk:mockk-jvm:${property("mockkVersion")}")
	testImplementation("com.ninja-squad:springmockk:${property("springmockkVersion")}"){
		exclude(module = "mockk")
	}

	testImplementation("io.kotest:kotest-runner-junit5-jvm:${property("kotestVersion")}")
	testImplementation("io.kotest:kotest-assertions-core-jvm:${property("kotestVersion")}")
	testImplementation("io.kotest:kotest-framework-concurrency:${property("kotestVersion")}")
}

kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        jvmTarget.set(JvmTarget.fromTarget("21"))
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

tasks.withType<Test> {
	useJUnitPlatform()
}
