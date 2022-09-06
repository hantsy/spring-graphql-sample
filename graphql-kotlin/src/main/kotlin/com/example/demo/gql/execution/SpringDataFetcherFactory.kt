package com.example.demo.gql.execution

import com.expediagroup.graphql.generator.extensions.deepName
import graphql.schema.DataFetcher
import graphql.schema.DataFetcherFactory
import graphql.schema.DataFetcherFactoryEnvironment
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.stereotype.Component

@Component
class SpringDataFetcherFactory : DataFetcherFactory<Any?>, BeanFactoryAware {
    companion object {
        private val log = LoggerFactory.getLogger(SpringDataFetcherFactory::class.java)
    }

    private lateinit var beanFactory: BeanFactory

    override fun setBeanFactory(beanFactory: BeanFactory) {
        this.beanFactory = beanFactory
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(environment: DataFetcherFactoryEnvironment): DataFetcher<Any?> {
        // Strip out possible `Input` and `!` suffixes added to by the SchemaGenerator
        var targetedTypeName = environment.fieldDefinition?.type?.deepName?.removeSuffix("!")?.removeSuffix("Input")
        log.debug("field def name:" + environment.fieldDefinition?.name)
        if (targetedTypeName != null && targetedTypeName.startsWith("[") && targetedTypeName.endsWith("!]")) {
            targetedTypeName = targetedTypeName.substring(1, targetedTypeName.length-2) +"List"
        }
        log.debug("target type name: $targetedTypeName")

        return beanFactory.getBean("${targetedTypeName}DataFetcher") as DataFetcher<Any?>
    }
}