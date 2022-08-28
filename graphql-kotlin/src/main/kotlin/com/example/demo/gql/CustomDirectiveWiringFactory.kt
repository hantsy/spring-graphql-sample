package com.example.demo.gql

import com.example.demo.gql.directives.UpperCaseDirectiveWiring
import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import org.springframework.stereotype.Component

@Component
class CustomDirectiveWiringFactory : KotlinDirectiveWiringFactory(
    mapOf("uppercase" to UpperCaseDirectiveWiring())
)