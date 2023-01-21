package com.example.demo.gql.datafetchers

import com.example.demo.service.PostService
import com.example.demo.gql.types.Comment
import com.expediagroup.graphql.server.operations.Subscription
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component


@Component
class Subscriptions(val postService: PostService) : Subscription {

    fun commentAdded(): Flow<Comment> {
        return postService.commentAdded()
    }
}