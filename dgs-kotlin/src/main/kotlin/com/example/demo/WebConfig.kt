package com.example.demo

import com.example.demo.repository.ProfileRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.http.MediaType
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.router
import java.util.*

@Configuration
class WebConfig {

    @Bean
    fun profileCoverImageRouter(
        gridFsTemplate: GridFsTemplate,
        profiles: ProfileRepository
    ): RouterFunction<ServerResponse> = router {
        GET("/users/:id/profile/coverImage") { request ->
            val userId = request.pathVariable("id")

            val profile = profiles.findByUserId(UUID.fromString(userId))
            if (profile != null) {
                val gridFile = gridFsTemplate.findOne(query(where("_id").isEqualTo(profile.coverImgId)))
                val gridResource = gridFsTemplate.getResource(gridFile)

                ServerResponse.ok()
                    .contentType(MediaType.parseMediaType(gridResource.contentType))
                    .contentLength(gridResource.contentLength())
                    .body(gridResource.content)

            }
            ServerResponse.notFound().build()
        }
    }

    /**
     * The CORS filter is only registered when the `cors` profile is active.
     */
    @Bean
    @Profile("cors")
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration().applyPermitDefaultValues()

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }

        return CorsFilter(source)
    }
}
