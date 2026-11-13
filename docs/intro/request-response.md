# Requests and responses

This page shows the shape of a GraphQL request and response. The examples run
against the schema in [Specification and schema](./spec.md), which the
[dgs](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs) and
[spring-graphql](https://github.com/hantsy/spring-graphql-sample/tree/master/spring-graphql)
examples expose at `http://localhost:8080/graphql`.

## The request

A GraphQL request carries three members:

- `query` — the document: one or more operations, each a `query`, `mutation`,
  or `subscription`.
- `variables` — a JSON object of values for the variables the document
  declares. Optional.
- `operationName` — which operation to run when the document contains more
  than one. Optional.

Over HTTP, the client POSTs this as a JSON body. The following request reads
every post with its title and author name:

```bash
curl http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ allPosts { id title author { name } } }"}'
```

### Variables

Never build a document by concatenating user input into the query string.
Declare variables instead, and let the server coerce and validate them:

```bash
curl http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query PostById($postId: ID!) { postById(postId: $postId) { title content } }",
    "variables": { "postId": "1" },
    "operationName": "PostById"
  }'
```

The document declares `$postId: ID!`; the `variables` object supplies the
value. The server validates the document against the schema before any data
fetcher runs.

### Mutations

A mutation uses the same envelope. The following request creates a post with
the `CreatePostInput` input type:

```bash
curl http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation CreatePost($input: CreatePostInput!) { createPost(createPostInput: $input) { id title } }",
    "variables": { "input": { "title": "Hello GraphQL", "content": "First post" } }
  }'
```

## The response

A GraphQL response is a JSON object with up to three members:

- `data` — the result, in the same shape as the operation's selection set.
- `errors` — a list of errors raised while validating or executing the
  operation. Present only when something went wrong.
- `extensions` — an optional map for server-specific additions, such as
  tracing data.

A successful response to the `allPosts` query looks like this:

```json
{
  "data": {
    "allPosts": [
      { "id": "1", "title": "Hello GraphQL", "author": { "name": "Hantsy" } }
    ]
  }
}
```

### Partial data and field errors

GraphQL distinguishes request errors from field errors. A request error — a
syntax error, a validation failure, an unknown operation — rejects the whole
operation, and the response has `errors` and no `data`. A field error — a data
fetcher that throws — nulls out only that field, and the rest of the response
still returns:

```json
{
  "data": {
    "postById": {
      "title": "Hello GraphQL",
      "author": null
    }
  },
  "errors": [
    {
      "message": "Failed to load author",
      "locations": [{ "line": 1, "column": 40 }],
      "path": ["postById", "author"]
    }
  ]
}
```

Each error carries a `message`, the `locations` in the document that caused
it, and the `path` to the field that failed. If a non-null field (`Author!`)
errors, the null propagates up to the nearest nullable parent — the schema's
nullability rules decide how much of the response survives.

This envelope is the same over every transport. See
[GraphQL over HTTP](./http.md) for how HTTP status codes interact with it, and
[Subscriptions over WebSocket](./websocket.md) for how a stream of these
envelopes reaches the client.