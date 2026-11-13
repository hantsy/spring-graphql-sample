# GraphQL over HTTP

The GraphQL specification deliberately does not define a transport. For years
the closest thing to official guidance was the
[Serving over HTTP](https://graphql.org/learn/serving-over-http/) article, and
leading clients and servers converged on its conventions. The
[GraphQL over HTTP specification](https://github.com/graphql/graphql-over-http)
formalizes those conventions for interoperability; it is a work in progress
(Stage 2: Draft), but its core rules already describe what most servers do.

## POST

A server MUST support GraphQL requests via POST with a UTF-8 JSON body and the
`Content-Type: application/json` header. The body carries the `query`,
`variables`, and `operationName` members described in
[Requests and responses](./request-response.md). Every example in this
repository accepts POST on `/graphql`.

## GET

A server MAY support GET for queries. The request parameters move into the URL
query string: `query`, `variables` (JSON-encoded), and `operationName`. GET
requests MUST NOT execute mutations — the recommended response is
`405 Method Not Allowed` with an `Allow` header. GET is useful for caching
read-only queries, but it is optional; do not rely on it being enabled.

## Media types and status codes

The specification registers two media types:

| Media type | Purpose |
| ---------- | ------- |
| `application/json` | GraphQL JSON requests. |
| `application/graphql-response+json` | GraphQL JSON responses. |

A client that understands the new response media type sends it in the
`Accept` header, typically as
`application/graphql-response+json, application/json;q=0.9` so older servers
can fall back to plain JSON.

The media type decides how errors map to HTTP status codes:

- With `application/graphql-response+json`, a request-level error (invalid
  document, validation failure, malformed JSON) produces a `4xx` or `5xx`
  status code. A well-formed operation that executes — even with field
  errors — produces a `2xx` status code.
- With legacy `application/json` responses, most servers return `200 OK` for
  any well-formed response and signal problems only in the `errors` member.

This is why a GraphQL client must always inspect the response body: a `200`
status does not mean every field succeeded.

## Subscriptions over HTTP: Server-Sent Events

Subscriptions need a channel that stays open. Over plain HTTP, the examples in
this repository use Server-Sent Events (SSE): the client POSTs the
subscription document with `Accept: text/event-stream`, and the server keeps
the response open and writes one `data:` event per result. The
[graphql-sse](https://github.com/enisdenjo/graphql-sse) library implements this
pattern on the client side.

The `-http` modules — for example
[spring-graphql-webmvc-http](https://github.com/hantsy/spring-graphql-sample/tree/master/spring-graphql-webmvc-http)
and
[dgs-subscription-sse](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-sse)
— serve queries, mutations, and SSE subscriptions on the same `/graphql`
endpoint. For a bidirectional channel that also carries queries and
mutations, see [Subscriptions over WebSocket](./websocket.md).

## GraphiQL

[GraphiQL](https://github.com/graphql/graphiql) is an in-browser IDE for
exploring a GraphQL endpoint: it reads the schema through introspection and
provides completion, documentation, and a query runner. Spring GraphQL serves
it when `spring.graphql.graphiql.enabled=true`; in this repository the two
`-http` modules enable it, and the other `spring-graphql-web*` modules disable
it.