package com.example.demo.gql.datafetchers

import com.example.demo.gql.types.Credentials
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.internal.DgsWebMvcRequestData
import jakarta.servlet.http.HttpSession
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository

@DgsComponent
class AuthDataFetcher(
    val authenticationManager: AuthenticationManager
) {

    @DgsMutation
    fun signIn(@InputArgument credentials: Credentials, dfe: DgsDataFetchingEnvironment): Map<String, Any> {
        var auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                credentials.username,
                credentials.password
            )
        )
        SecurityContextHolder.getContext().authentication = auth

        // get the session id from redis.
        val req = dfe.getDgsContext().requestData as DgsWebMvcRequestData
        val session = req.webRequest?.sessionMutex as HttpSession
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
        )
        return mapOf(
            "name" to auth.principal.toString(),
            "roles" to auth.authorities.map { it.authority },
            "token" to session.id
        )
    }

    @DgsMutation
    @PreAuthorize("isAuthenticated()")
    fun logout(dfe: DgsDataFetchingEnvironment): Boolean {
        val req = dfe.getDgsContext().requestData as DgsWebMvcRequestData
        val session = req.webRequest?.sessionMutex as HttpSession?
        session?.invalidate()
        return true
    }
}