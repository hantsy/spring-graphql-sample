package com.example.demo

import com.netflix.graphql.dgs.DgsQueryExecutor
import graphql.ExecutionResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.security.test.context.support.WithMockUser
import reactor.test.StepVerifier
import java.util.*


@SpringBootTest(
    classes = [DemoApplication::class],
    properties = ["context.initializer.classes=com.example.demo.TestConfigInitializer"]
)
class DemoApplicationTests {

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Test
    fun `get all posts`() {
        val query = """
			query posts{
                allPosts{
                    id
                    title
                    content
                    author { id name email }
                    comments { id content }
                }
			}""".trimIndent()

        val titles = dgsQueryExecutor.executeAndExtractJsonPath<List<String>>(query, "data.allPosts[*].title")
        assertThat(titles.size).isEqualTo(0);
    }

    @Test
    fun `get an non-existing post should return errors NOT_FOUND`() {
        val query = "query notExisted(\$id: String!){postById(postId:\$id){ id title }}"

        val result = dgsQueryExecutor.execute(query, mapOf("id" to UUID.randomUUID().toString()))
        assertThat(result.errors).isNotNull
        assertThat(result.errors[0].extensions["errorType"]).isEqualTo("NOT_FOUND")
    }

    //see: https://github.com/Netflix/dgs-framework/issues/453
    //and: https://github.com/Netflix/dgs-framework/issues/437#issuecomment-871100938
    @Test
    fun `create a post without auth should return errors PERMISSION_DENIED`() {
        val query = "mutation createPost(\$input: CreatePostInput!){createPost(createPostInput:\$input){ id title }}"

        val result = dgsQueryExecutor.execute(
            query,
            mapOf("input" to mapOf("title" to "my title", "content" to "my content of my title"))
        )
        println("errors: " + result.errors);
        assertThat(result.errors).isNotNull
        // explained in  https://github.com/Netflix/dgs-framework/issues/437#issuecomment-871100938
        //assertThat(result.errors[0].extensions["errorType"]).isEqualTo("PERMISSION_DENIED")
    }


    @Test
    @Disabled
    fun `sign in and create a post and comment`() {
        // logged in
        val loggedInQuery = "query signIn(\$input: Credentials!){signIn(credentials:\$input){ name roles  token }}"
        val token = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            loggedInQuery,
            "data.signIn.token",
            mapOf("input" to mapOf("username" to "user", "password" to "password"))
        )
        println("user auth token: $token")
        assertThat(token).isNotNull

        // create a new post
        val createPostQuery = "mutation createPost(\$input: CreatePostInput!){createPost(createPostInput:\$input){ id title }}"
        val createPostResult = dgsQueryExecutor.executeAndGetDocumentContext(
            createPostQuery,
            mapOf("input" to mapOf("title" to "my title", "content" to "my content of my title")),
            HttpHeaders().apply { add("X-Auth-Token", token) }
        )

        val postId = createPostResult.read("data.createPost.id", String::class.java)
        println("created post id: $postId")
        assertThat(postId).isNotNull

        // get post by id.
        val postByIdQuery = "query postById(\$id: String!){postById(postId:\$id){ id title }}"
        val postByIdResult = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            postByIdQuery,
            "data.postById.title",
            mapOf("id" to UUID.randomUUID().toString())
        )

        println("get post by id: $postId, result: $postByIdResult")
        assertThat(postByIdResult).isEqualTo("my title")

        //subscribe to commentAdded.
        val executionResult = dgsQueryExecutor.execute("subscription onCommentAdded{ commentAdded{ id content}}")
        val publisher = executionResult.getData<Publisher<ExecutionResult>>()

        val verifier = StepVerifier.create(publisher)
            .consumeNextWith {
                assertThat((it.getData<Map<String, Map<String, Any>>>()["commentAdded"]
                    ?.get("content") as String)).contains("comment1")
            }
            .consumeNextWith {
                assertThat((it.getData<Map<String, Map<String, Any>>>()["commentAdded"]
                    ?.get("content") as String)).contains("comment2")
            }
            .thenCancel()
            .verifyLater()


        val comment1 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation addComment(\$input: CommentInput!) { addComment(commentInput:\$input) { content}}",
            "data.commentAdded.content",
            mapOf("postId" to postId, "content" to "comment1 message")
        )
        assertThat(comment1).contains("comment1");

        val comment2 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation addComment(\$input: CommentInput!) { addComment(commentInput:\$input) { content}}",
            "data.commentAdded.content",
            mapOf("postId" to postId, "content" to "comment2 message")
        )
        assertThat(comment2).contains("comment2");

        //verify it now.
        verifier.verify();
    }

}
