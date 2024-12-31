package com.example.demo.gql;

import com.example.demo.gql.directives.UppercaseDirectiveWiring;
import lombok.RequiredArgsConstructor;

//@DgsComponent
@RequiredArgsConstructor
public class CustomRuntimeWiring {
    final UppercaseDirectiveWiring uppercaseDirectiveWiring;

    // use
//    @DgsRuntimeWiring
//    public RuntimeWiring.Builder customRuntimeWiring(RuntimeWiring.Builder builder) {
//
//        // use @DgsDirective instead
//        // builder.directive("uppercase", uppercaseDirectiveWiring);
//
//        //
//        // This contains by default the standard library provided @Directive constraints
//        //
//        ValidationRules validationRules = ValidationRules.newValidationRules()
//                .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
//                .build();
//        //
//        // This will rewrite your data fetchers when rules apply to them so that validation
//        ValidationSchemaWiring schemaWiring = new ValidationSchemaWiring(validationRules);
//        //
//        // we add this schema wiring to the graphql runtime
//        return builder.directiveWiring(schemaWiring);
//    }
}
