package com.example.demo.gql

import com.example.demo.Scalars
import com.example.demo.gql.directives.UpperCaseDirectiveWiring
import graphql.schema.idl.RuntimeWiring
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import org.springframework.stereotype.Component

@Component
class PostsRuntimeWiring : RuntimeWiringConfigurer {
    override fun configure(builder: RuntimeWiring.Builder) {
        builder
            .scalar(Scalars.uuidType())
            .scalar(Scalars.localDateTimeType())
            .directive("uppercase", UpperCaseDirectiveWiring())
            .build()
    }
}