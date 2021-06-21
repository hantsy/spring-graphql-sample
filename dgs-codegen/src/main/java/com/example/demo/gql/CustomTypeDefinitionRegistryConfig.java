package com.example.demo.gql;

import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeExtensionDefinition;
import graphql.language.TypeName;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class CustomTypeDefinitionRegistryConfig {

    @Bean
    public TypeDefinitionRegistry extraTypeDefinitionRegistry() {
        ObjectTypeExtensionDefinition objectTypeExtensionDefinition = ObjectTypeExtensionDefinition
                .newObjectTypeExtensionDefinition()
                .name("Query").fieldDefinition(
                        FieldDefinition.newFieldDefinition()
                                .name("currentTimestamp")
                                .type(new TypeName("String"))
                                .build()
                )
                .build();

        TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
        typeDefinitionRegistry.add(objectTypeExtensionDefinition);
        return typeDefinitionRegistry;
    }
}
