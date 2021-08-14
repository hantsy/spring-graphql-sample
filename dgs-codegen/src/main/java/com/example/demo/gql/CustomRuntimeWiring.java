package com.example.demo.gql;

import com.example.demo.gql.directives.UppercaseDirectiveWiring;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsRuntimeWiring;
import graphql.schema.idl.RuntimeWiring;

@DgsComponent
public class CustomRuntimeWiring {

    @DgsRuntimeWiring
    public RuntimeWiring.Builder customRuntimeWiring(RuntimeWiring.Builder builder, UppercaseDirectiveWiring uppercaseDirectiveWiring) {
        return builder.directive("uppercase", uppercaseDirectiveWiring);
    }
}
