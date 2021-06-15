package com.example.demo

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.security.authentication.*
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.session.web.http.HeaderHttpSessionIdResolver
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import java.util.*
import java.util.stream.IntStream
import kotlin.reflect.cast

@SpringBootApplication
@EnableJdbcAuditing
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args) {
        addInitializers(beans)
    }
}

val beans = beans {
    bean {
        ApplicationRunner {
            println("start data initialization...")
            val posts = ref<PostRepository>()
            val comments = ref<CommentRepository>()
            val authors = ref<AuthorRepository>()
            val passwordEncoder = ref<PasswordEncoder>()

            comments.deleteAll()
            posts.deleteAll()
            authors.deleteAll()

            val author = AuthorEntity(
                name = "user",
                password = passwordEncoder.encode("password"),
                email = "user@example.com"
            )
            val savedAuthor = authors.save(author)
            println("saved author: $savedAuthor")
//            for (i in 1..10) {
//                val postData = PostEntity(
//                    title = "Dgs post #$i",
//                    content = "Content of post #$i",
//                    authorId = savedAuthor.id
//                )
//                val savedPost = posts.save(postData);
//                IntStream.range(1, Random().nextInt(5) + 1)
//                    .mapToObj { CommentEntity(content = "comment # $it", postId = savedPost.id) }
//                    .forEach { comments.save(it) }
//            }
//
//            posts.findAll().forEach { println("post: $it") }
//            comments.findAll().forEach { println("comment: $it") }
//            authors.findAll().forEach { println("author: $it") }
            println("done data initialization...")
        }
    }

    profile("cors") {
        bean("corsFilter") {

            //val config = CorsConfiguration().apply {
            // allowedOrigins = listOf("http://allowed-origin.com")
            // maxAge = 8000L
            // addAllowedMethod("PUT")
            // addAllowedHeader("X-Allowed")
            //}

            val config = CorsConfiguration().applyPermitDefaultValues()

            val source = UrlBasedCorsConfigurationSource().apply {
                registerCorsConfiguration("/**", config)
            }

            CorsFilter(source)
        }
    }


    //security config.
    bean {
        HeaderHttpSessionIdResolver.xAuthToken()
    }

    bean {
        PasswordEncoderFactories.createDelegatingPasswordEncoder()
    }

    bean {
        AuditorAware<UUID> {
            Optional.ofNullable(SecurityContextHolder.getContext())
                .map { it.authentication }
                .filter { it !is AnonymousAuthenticationToken && it.isAuthenticated }
                .map { it as AuthenticationTokenWithId }
                .map { it.id }
        }
    }

    bean {
        val security = ref<HttpSecurity>()
        security {
            csrf { disable() }
            httpBasic { }
            securityMatcher("/**")
            // use method level security control instead of url based matchers.
//            authorizeRequests {
//                authorize("/auth/**", authenticated)
//                authorize(AntPathRequestMatcher("/posts/**", HttpMethod.GET.name), permitAll)
//                authorize(HttpMethod.DELETE, "/posts/**", hasRole("ADMIN"))
//                authorize("/posts/**", authenticated)
//                authorize(anyRequest, permitAll)
//            }
//            formLogin {
//                loginPage = "/log-in"
//            }
        }
        security.build()
    }

    bean {
        UserDetailsService {
            val users = ref<AuthorRepository>()
            val author = users.findByNameOrEmail(it, it)
            when {
                author != null -> UserWithId(author.id!!, author.name, author.password)
                else -> throw UsernameNotFoundException("Username: $it was not found.")
            }
        }
    }
    bean {
        AuthenticationManager {
            val username = it.principal.toString()
            val password = it.credentials.toString()

            val userDetailsService = ref<UserDetailsService>()
            val passwordEncoder = ref<PasswordEncoder>()

            val user = userDetailsService.loadUserByUsername(username) as UserWithId
            if (!passwordEncoder.matches(password, user.password)) {
                throw BadCredentialsException("username or password was not matched.")
            }
            if (!user.isEnabled()) {
                throw DisabledException("user is not enabled.")
            }
            AuthenticationTokenWithId(user.id, username, user.authorities)
        }
    }
}

data class UserWithId(val id: UUID, val name: String, val pwd: String) :
    User(name, pwd, AuthorityUtils.createAuthorityList("ROLE_USER"))

data class AuthenticationTokenWithId(val id: UUID, val username: String, val auth: Collection<GrantedAuthority>) :
    UsernamePasswordAuthenticationToken(username, "", auth)