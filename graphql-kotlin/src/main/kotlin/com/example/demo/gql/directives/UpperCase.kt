package com.example.demo.gql.directives

import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import graphql.introspection.Introspection

@GraphQLDirective(name = UpperCaseDirectiveWiring.name, locations = arrayOf(Introspection.DirectiveLocation.FIELD_DEFINITION))
annotation class UpperCase()
