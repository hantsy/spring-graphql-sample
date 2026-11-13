# Subscriptions over WebSocket

A subscription is a long-lived operation: the server pushes a new result every
time an event happens. Plain HTTP request/response cannot carry that, so
subscription clients and servers agree on a WebSocket subprotocol. Two
protocols exist, and they are **not** compatible with each other — the client
and the server must speak the same one.

## subscriptions-transport-ws (deprecated)

[subscriptions-transport-ws](https://github.com/apollographql/subscriptions-transport-ws)
was the first WebSocket transport for GraphQL subscriptions, created by Apollo
in 2016. It was largely unmaintained after 2018, and its repository now states
that the project is no longer maintained and that users should migrate to
`graphql-ws`.

The protocol uses the WebSocket subprotocol `graphql-ws`. Its messages,
prefixed `GQL_` in the reference implementation, are:

| Message | Direction | Purpose |
| ------- | --------- | ------- |
| `connection_init` | client → server | Initialize the connection, optionally with a payload (for example, auth tokens). |
| `connection_ack` | server → client | Acknowledge the connection. |
| `connection_keep_alive` | server → client | Periodic heartbeat (`ka`) after the ack. |
| `start` | client → server | Start an operation with an id and a GraphQL payload. |
| `data` | server → client | One execution result for a started operation. |
| `error` | server → client | Operation failed before execution (for example, validation errors). |
| `complete` | server → client | The operation's stream is finished. |
| `stop` | client → server | Stop a running operation. |
| `connection_terminate` | client → server | Close the connection. |

Only subscriptions were intended to run over this protocol; queries and
mutations stayed on HTTP.

## graphql-ws

[graphql-ws](https://github.com/enisdenjo/graphql-ws) is the actively
maintained replacement. Its protocol, the
[GraphQL over WebSocket Protocol](https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md),
uses the subprotocol `graphql-transport-ws`. The message flow is similar, but
the protocol fixes the gaps of its predecessor:

| Message | Direction | Purpose |
| ------- | --------- | ------- |
| `ConnectionInit` | client → server | Initialize the connection. More than one init closes the socket (`4429`). |
| `ConnectionAck` | server → client | Acknowledge; operations are allowed only after the ack (otherwise `4401`). |
| `Subscribe` | client → server | Start **any** operation — query, mutation, or subscription — under a unique id. A duplicate id closes the socket (`4409`). |
| `Next` | server → client | One execution result from the operation's stream. |
| `Error` | server → client | Execution errors for the operation; the stream is finished. |
| `Complete` | either | The stream is finished; the client can also send it to unsubscribe. |
| `Ping` / `Pong` | either | Bidirectional heartbeat, sent at any time; a `Pong` may also be unsolicited. |

The differences that matter in practice:

- **Any operation over the socket.** Queries and mutations can share the
  subscription connection instead of opening separate HTTP requests.
- **Real keep-alive.** Both sides can ping; the old protocol only had a
  server-side keep-alive.
- **Defined failure semantics.** Close codes such as `4401 Unauthorized`,
  `4409 Subscriber already exists`, and `4429 Too many initialisation
  requests` replace ad-hoc behavior.

Because the protocols are distinct, migrating means upgrading the client and
the server together — a `graphql-ws` client cannot talk to a
`subscriptions-transport-ws` server.

## In this repository

The WebSocket examples all speak `graphql-transport-ws`:

- [dgs-subscription-ws](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ws)
  — DGS subscriptions. A `@DgsSubscription` method returns a reactive
  `Publisher`; the following excerpt is from its `DataFetchers.kt`:

  ```kotlin
  @DgsSubscription
  fun messageSent(): Publisher<TextMessage> = sink.asFlux()
  ```

- [spring-graphql-webmvc-ws](https://github.com/hantsy/spring-graphql-sample/tree/master/spring-graphql-webmvc-ws)
  and
  [spring-graphql-webflux-ws](https://github.com/hantsy/spring-graphql-sample/tree/master/spring-graphql-webflux-ws)
  — Spring GraphQL with every operation type over WebSocket. The endpoint is
  configured with a property, as in `spring-graphql-webmvc-http-ws`:

  ```properties
  spring.graphql.websocket.path=/subscriptions
  ```

- The `dgs-subscription-ui*` Angular clients consume these streams with Apollo
  Client and the `graphql-ws` library.

If you do not need a bidirectional channel, Server-Sent Events are a simpler
alternative — see [GraphQL over HTTP](./http.md).