package com.example.demo

import com.example.demo.repository.AuthorRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.session.web.http.HeaderHttpSessionIdResolver
import java.util.*

@Configuration
class SecurityConfig {

    /**
     * The bean name must not be `springSecurityFilterChain`: Spring Boot's
     * `ServletWebSecurityAutoConfiguration` is `@ConditionalOnMissingBean(name = "springSecurityFilterChain")`,
     * so a bean with that name would suppress `@EnableWebSecurity` and with it the
     * `HttpSecurity` bean this method depends on.
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            httpBasic { }
            securityMatcher("/**")
            // use method level security control instead of url based matchers.
        }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun userDetailsService(authors: AuthorRepository): UserDetailsService = UserDetailsService { username ->
        val author = authors.findByNameOrEmail(username, username)
        if (author != null) {
            UserWithId(author.id!!, author.name, author.password)
        } else {
            throw UsernameNotFoundException("Username: $username was not found.")
        }
    }

    @Bean
    fun authenticationManager(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): AuthenticationManager = AuthenticationManager { authentication ->
        val username = authentication.principal.toString()
        val password = authentication.credentials.toString()

        val user = userDetailsService.loadUserByUsername(username) as UserWithId
        if (!passwordEncoder.matches(password, user.password)) {
            throw BadCredentialsException("username or password was not matched.")
        }
        if (!user.isEnabled) {
            throw DisabledException("user is not enabled.")
        }
        AuthenticationTokenWithId(user.id, username, user.authorities)
    }

    @Bean
    fun auditorAware(): AuditorAware<UUID> = AuditorAware {
        Optional.ofNullable(SecurityContextHolder.getContext())
            .map { it.authentication }
            .filter { it != null && it !is AnonymousAuthenticationToken && it.isAuthenticated }
            .map { it as AuthenticationTokenWithId }
            .map { it.id }
    }

    @Bean
    fun sessionIdResolver(): HeaderHttpSessionIdResolver = HeaderHttpSessionIdResolver.xAuthToken()
}

data class UserWithId(val id: UUID, val name: String, val pwd: String) :
    User(name, pwd, AuthorityUtils.createAuthorityList("ROLE_USER"))

data class AuthenticationTokenWithId(val id: UUID, val username: String, val auth: Collection<GrantedAuthority>) :
    UsernamePasswordAuthenticationToken(username, "", auth)
