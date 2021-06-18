package com.example.demo.gql.directives;

import graphql.kickstart.tools.boot.SchemaDirective;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DirectiveConfig {

    @Bean
    SchemaDirective uppercaseDirective(){
        return new SchemaDirective("uppercase", new UppercaseDirective());
    }
}
