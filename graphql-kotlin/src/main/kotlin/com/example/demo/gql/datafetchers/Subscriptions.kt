package com.example.demo.gql.datafetchers

import com.example.demo.PostService
import com.example.demo.gql.types.Comment
import com.expediagroup.graphql.server.operations.Subscription
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class Subscriptions(val postService: PostService) : Subscription {

    fun commentAdded(): Publisher<Comment> {
        return postService.commentAdded()
    }
}