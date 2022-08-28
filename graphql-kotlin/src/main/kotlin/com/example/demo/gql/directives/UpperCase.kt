package com.example.demo.gql.directives

import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import graphql.introspection.Introspection.DirectiveLocation.FIELD_DEFINITION

@GraphQLDirective(name = UpperCaseDirectiveWiring.name, locations = [FIELD_DEFINITION])
annotation class UpperCase
