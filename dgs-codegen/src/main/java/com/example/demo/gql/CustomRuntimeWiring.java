package com.example.demo.gql;

import com.example.demo.gql.directives.UppercaseDirectiveWiring;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsRuntimeWiring;
import graphql.schema.idl.RuntimeWiring;
import lombok.RequiredArgsConstructor;

@DgsComponent
@RequiredArgsConstructor
public class CustomRuntimeWiring {
    final UppercaseDirectiveWiring uppercaseDirectiveWiring;

    @DgsRuntimeWiring
    public RuntimeWiring.Builder customRuntimeWiring(RuntimeWiring.Builder builder) {
        return builder.directive("uppercase", uppercaseDirectiveWiring);
    }
}
