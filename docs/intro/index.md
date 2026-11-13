# Introduction to GraphQL

GraphQL is a query language for APIs and a runtime for executing those queries
against your data. Facebook's mobile apps have been powered by GraphQL since
2012. The specification was open-sourced in 2015, and it is now supported by the
[GraphQL Foundation](https://graphql.org/foundation/), hosted by the non-profit
Linux Foundation since 2018.

A GraphQL server exposes a single endpoint and a strongly typed schema. The
client sends a document that describes exactly the data it needs, and the
server responds with JSON in the same shape. Compared with a REST API, this
moves the choice of response shape from the server to the client: no
over-fetching fixed payloads, and no chaining multiple endpoint calls to
assemble one screen.

The specification defines three kinds of operations:

- **Query** — read data.
- **Mutation** — write data, executed serially in the order the document lists
  them.
- **Subscription** — a long-lived operation that pushes a stream of results to
  the client as events happen.

The specification deliberately says nothing about the transport. HTTP and
WebSocket are the common choices, and separate specifications cover them.

## In this section

Read the pages in order for a complete picture of the protocol:

1. [Specification and schema](./spec.md) — the specification editions and the
   schema definition language (SDL).
2. [Requests and responses](./request-response.md) — the document, variables,
   and the `data`/`errors` response envelope.
3. [File upload](./file-upload.md) — the multipart request specification and
   the `Upload` scalar.
4. [GraphQL over HTTP](./http.md) — the HTTP transport, media types, status
   codes, and Server-Sent Events.
5. [Subscriptions over WebSocket](./websocket.md) — the deprecated
   `subscriptions-transport-ws` protocol and its replacement, `graphql-ws`.