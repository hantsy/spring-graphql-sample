# Specification and schema

This page explains what the GraphQL specification covers and how to read and
write the schema definition language (SDL). It is for developers who are new to
GraphQL; it does not cover any Java API.

## The specification

The [GraphQL specification](https://spec.graphql.org/) defines the query
language, the type system, validation rules, execution semantics, and
introspection. It deliberately does not define a transport — see
[GraphQL over HTTP](./http.md) and
[Subscriptions over WebSocket](./websocket.md) for the transport specifications.

The specification is published in dated editions:

| Edition | Notes |
| ------- | ----- |
| [June 2018](https://spec.graphql.org/June2018/) | The first stable edition after the 2015 open-source release. |
| [October 2021](https://spec.graphql.org/October2021/) | Adds custom scalar semantics clarifications and repeatable directives. |
| [September 2025](https://spec.graphql.org/September2025/) | The latest released edition. |

A [draft](https://spec.graphql.org/draft/) of the next edition is also
published. Every server implementation — including
[GraphQL Java](../java/index.md) — implements the language and execution rules
from these editions.

## The schema definition language

A GraphQL schema is a contract written in SDL. It declares the types a server
exposes and the operations clients can run. The following schema is the one
used by the [dgs](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs)
example (comments and formatting trimmed):

```graphql
type Post {
    id: ID!
    title: String!
    content: String
    comments: [Comment]
    authorId: String
    author: Author
}

type Author {
    id: ID!
    name: String!
    email: String!
    posts: [Post]
}

type Comment {
    id: ID!
    content: String!
    postId: String!
}

input CreatePostInput {
    title: String!
    content: String!
}

type Query {
    allPosts: [Post!]!
    postById(postId: ID!): Post
}

type Mutation {
    createPost(createPostInput: CreatePostInput!): Post!
}
```

The building blocks are:

- **Object types** (`type Post`) — named collections of fields. Each field has
  a type and can take arguments, such as `postById(postId: ID!)`.
- **Scalar types** — the leaf values. The built-in scalars are `Int`, `Float`,
  `String`, `Boolean`, and `ID`. Servers can add custom scalars such as `Date`
  or `Upload`.
- **Input types** (`input CreatePostInput`) — structured arguments for
  mutations. Only input types, scalars, and enums can be used as input.
- **Enums** — a fixed set of named values, for example
  `enum PostStatus { DRAFT PUBLISHED }`.
- **Interfaces and unions** — abstract types that let a field return one of
  several object types. A `TypeResolver` picks the concrete type at runtime.
- **Non-null (`!`) and lists (`[]`)** — modifiers. `Post!` never returns null,
  `[Post!]!` is a non-null list of non-null posts, and `[Post]` can be null and
  can contain nulls.
- **Directives** — annotations such as `@deprecated(reason: "...")` that change
  how the schema or a document is executed. Servers can define custom
  directives.

## Root operation types

Three root types anchor every schema:

- `Query` — the entry point for reads.
- `Mutation` — the entry point for writes. Top-level mutation fields execute
  serially, in document order; query fields can execute in parallel.
- `Subscription` — the entry point for event streams. A subscription field
  returns a stream of results rather than a single value.

The names are conventions; the schema can rename them with a `schema`
declaration, but most servers — and every example in this repository — use the
default names.

## Introspection

A GraphQL schema is queryable. Clients can ask the server for its own types
through the meta-fields `__schema` and `__type`, which is how tools such as
GraphiQL and Apollo Client provide completion and documentation. The following
query lists every type name the server exposes:

```graphql
{
  __schema {
    types {
      name
    }
  }
}
```

Introspection is part of the specification, so it works against any compliant
server, including all examples in this repository.