# Create a Chat Application with Netflix DGS

Real-time chat is the smallest application that needs all three GraphQL
operation types: a **query** to load the history, a **mutation** to send a
message, and a **subscription** to push new messages to every connected
client. In this tutorial you build exactly that with the Netflix DGS
framework on the server and Angular on the client.

The application gets built twice. The first version runs its subscription
over a **WebSocket**; the second swaps in **Server-Sent Events (SSE)**. The
UI is written twice as well — first with the classic Angular component API,
then again with **signals**.

## Why WebSocket first

The two transports are not equally hard, and the difference shows up mostly
on the client.

Apollo Client ships a ready-made link for WebSocket. `GraphQLWsLink` from
`@apollo/client/link/subscriptions` is a handful of lines, and the
`graphql-ws` package underneath it takes care of the handshake, the
heartbeats, and the reconnection.

SSE has no equivalent. The server side is arguably the simpler of the two —
an SSE subscription is just a long-lived HTTP response, so the server needs
no extra dependency at all — but on the client you have to wrap the
`graphql-sse` package in a hand-written `ApolloLink` that translates
Apollo operations into SSE requests.

Building the WebSocket version first gets you a working chat before you
write a line of transport plumbing. The SSE switch afterwards is then a
small, focused exercise: swap one link for another, and watch the browser
hold an event-stream response open instead of a socket.

Every step below maps onto a module in the
[spring-graphql-sample repository](https://github.com/hantsy/spring-graphql-sample):

| Module | Role |
| ------ | ---- |
| [`dgs-subscription-ws`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ws) | DGS server, subscriptions over WebSocket |
| [`dgs-subscription-ui`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui) | Angular client, components, WebSocket transport |
| [`dgs-subscription-ui-signals`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui-signals) | Angular client, signals, WebSocket transport |
| [`dgs-subscription-sse`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-sse) | DGS server, subscriptions over SSE |
| [`dgs-subscription-ui-sse`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui-sse) | Angular client, components, SSE transport |
| [`dgs-subscription-ui-sse-signals`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui-sse-signals) | Angular client, signals, SSE transport |

Import lines and package declarations are left out where they would only
pad the example.

## Prerequisites

You need a few tools on your machine:

| Tool | Version | Why |
| ---- | ------- | --- |
| JDK | 25 | The samples run on Spring Boot 4.1.2 and Kotlin 2.4.x |
| Node.js | 24 | The Angular clients use Angular 22 and the Vitest test runner |
| Git | any | Only if you plan to clone the sample repository |

Maven does not have to be installed separately — Spring Initializr drops a
wrapper script (`mvnw`, or `mvnw.cmd` on Windows) into the generated
project.

New to DGS? Read
[Building GraphQL APIs with Netflix DGS](./start.md) first. It covers the
programming model, the schema-first workflow, data fetchers, DataLoaders,
and error handling. This tutorial picks up from there and stays focused on
subscriptions and the client.

## Step 1: Define the use cases

Start with the users rather than the schema. Chat is small enough to reason
about end to end, and the use cases tell you which operations you actually
need.

### User stories

**Story 1 — catch up on the conversation.** As a visitor, I want to see the
recent messages as soon as I open the chat, so I can follow along without
asking anyone to repeat themselves.

**Story 2 — send a message.** As a visitor, I want to type a message and
send it, so I can take part in the conversation.

**Story 3 — see messages arrive.** As a visitor, I want messages from other
people to show up without a page refresh, so the chat feels alive.

**Story 4 — know when the live feed breaks.** As a visitor, I want to be
told when the live connection drops, so I do not sit in front of a list
that quietly stopped updating.

**Story 5 — one contract.** As an operator, I want reads, writes, and live
updates served from a single endpoint against a single schema, so the
client needs no second API and no second contract to maintain.

### From stories to operations

Each story lands on one operation type:

| Story | Operation | Field |
| ----- | --------- | ----- |
| 1 — catch up | Query | `messages` |
| 2 — send | Mutation | `send(message: TextMessageInput!)` |
| 3 — live updates | Subscription | `messageSent` |
| 4 — connection state | — | transport level, outside the schema |
| 5 — one contract | All three | the same `/graphql` endpoint |

Three things fall out of this table.

Stories 1 and 2 are ordinary GraphQL operations. They go over plain HTTP
and need no special server setup.

Story 3 is the only one that needs a **long-lived connection**, and that is
where the transport choice — WebSocket or SSE — actually bites.

Story 4 cannot be expressed in the schema at all. A subscription that dies
silently is indistinguishable from a chat room where nobody is talking, so
the client has to watch the transport directly. Every client in this
tutorial renders a connection banner for exactly that reason.

Since stories 1, 2, and 5 map onto the three root types, the chat schema
needs exactly one query, one mutation, and one subscription. Everything
else is detail.

## Step 2: Create the project

The chat server is the DGS application from
[Building GraphQL APIs with Netflix DGS](./start.md), with two differences:
it is written in Kotlin, and it keeps the message history in memory instead
of a database — which keeps the focus on the subscription stream.

Start from [Spring Initializr](https://start.spring.io/): pick Kotlin and
Spring Boot 4.1.2, use `com.example` as the group and `demo` as the
artifact, and add Spring Web plus the DGS starter. The POM that comes out
needs the DGS BOM, the starter, and the code generation plugin:

```xml
<properties>
    <java.version>25</java.version>
    <kotlin.version>2.4.21</kotlin.version>
    <dgs-platform.version>12.1.1</dgs-platform.version>
    <dgs-codegen-plugin.version>3.10.2</dgs-codegen-plugin.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.netflix.graphql.dgs</groupId>
            <artifactId>graphql-dgs-platform-dependencies</artifactId>
            <version>${dgs-platform.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.netflix.graphql.dgs</groupId>
        <artifactId>dgs-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.jetbrains.kotlin</groupId>
        <artifactId>kotlin-stdlib</artifactId>
    </dependency>
    <dependency>
        <groupId>org.jetbrains.kotlin</groupId>
        <artifactId>kotlin-reflect</artifactId>
    </dependency>
</dependencies>
```

The dependencies each do a specific job:

- `dgs-starter` wires up DGS and its GraphQL execution support.
- `spring-boot-starter-webmvc` serves the HTTP endpoint.
- `spring-boot-starter-websocket` turns on the WebSocket transport. The
  GraphQL WebSocket endpoint is off by default in Spring GraphQL, and for a
  servlet application this starter is what enables it.
- `reactor-core` provides the reactive types the subscription returns.

Only the WebSocket starter is transport-specific. When you move to SSE in
Step 7 the server does not need it at all, because an SSE subscription is
served by the HTTP endpoint.

Configure the Kotlin compiler and the code generation plugin next. The
generator reads the SDL and emits Kotlin data classes plus a typed Java
client API, so the fetchers never have to hand-write types:

```xml
<build>
    <sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>
    <plugins>
        <plugin>
            <groupId>io.github.deweyjose</groupId>
            <artifactId>graphqlcodegen-maven-plugin</artifactId>
            <version>${dgs-codegen-plugin.version}</version>
            <executions>
                <execution>
                    <id>dgs-codegen</id>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                    <configuration>
                        <language>kotlin</language>
                        <schemaPaths>
                            <param>${project.basedir}/src/main/resources/schema</param>
                        </schemaPaths>
                        <packageName>com.example.demo.gql</packageName>
                        <generateClientApi>true</generateClientApi>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-plugin</artifactId>
            <configuration>
                <compilerPlugins>
                    <plugin>spring</plugin>
                </compilerPlugins>
            </configuration>
        </plugin>
    </plugins>
</build>
```

`mvnw generate-sources` (or `mvnw.cmd generate-sources` on Windows) runs
the generator. The data classes land in `com.example.demo.gql.types` and
the typed client API in `com.example.demo.gql.client`. The examples below
use the former; the latter is what you would reach for when writing a Java
or Kotlin client against this schema.

## Step 3: Define the chat schema

DGS is schema-first. The SDL file is the contract, and the Kotlin code
afterwards only fills in the behaviour. Put `schema.graphql` under
`src/main/resources/schema`:

```graphql
type TextMessage {
    id: ID!
    body: String!
    sentAt: LocalDateTime
}

type Query {
    messages: [TextMessage!]!
}

type Mutation {
    send(message: TextMessageInput!): TextMessage
}

input TextMessageInput {
    body: String!
}

type Subscription {
    messageSent: TextMessage!
}

scalar LocalDateTime
```

Everything in there traces back to the stories above. `TextMessage` is the
message type: `id` and `body` are required, while `sentAt` is a
`LocalDateTime` — a **custom scalar** the server supplies for ISO-8601
timestamps. `Query.messages` serves the catch-up story, `Mutation.send`
serves sending, and `Subscription.messageSent` pushes new messages to
every member of the room.

Two details deserve a second look. `send` returns the stored message, but
the client does not need that response to draw the new message — the
subscription delivers it. The response matters for correctness and for
error reporting, not for the happy path. And `messageSent` returns one
message per event rather than a list: a subscription delivers a single
result at a time, and the stream of events is what forms the list.

Nothing in the schema mentions the transport. The same file serves the
WebSocket version and the SSE version, and the same `@DgsSubscription`
method backs both. That is why the SSE switch later comes down to a
client-side change and a couple of properties.

## Step 4: Implement the DGS data fetchers

Data fetchers are where the schema meets the code. A `@DgsComponent` class
holds the methods, and DGS annotations say which schema field each method
answers. Subscription fetchers differ from the rest in one respect: they
return a reactive `Publisher` instead of a value, and the transport
forwards every item that publisher emits.

In package `com.example.demo`:

```kotlin
@DgsComponent
class MessagesDataFetcher {

    @DgsQuery
    fun messages() = STORE.values

    @DgsMutation
    fun send(@InputArgument("message") message: TextMessageInput): TextMessage {
        val msg = TextMessage(
            id = UUID.randomUUID().toString(),
            body = message.body,
            sentAt = LocalDateTime.now()
        )
        STORE[msg.id] = msg
        sink.emitNext(msg, Sinks.EmitFailureHandler.FAIL_FAST)
        return msg
    }

    @DgsSubscription
    fun messageSent(): Publisher<TextMessage> = sink.asFlux()

    private val sink: Many<TextMessage> = Sinks.many().replay().limit(100)

    companion object {
        val STORE = mutableMapOf<String, TextMessage>()
    }
}
```

The annotations line up with the schema without any extra configuration.
`@DgsQuery` binds `messages()` to `Query.messages`, `@DgsMutation`
binds `send(...)` to `Mutation.send`, and `@DgsSubscription` binds
`messageSent()` to `Subscription.messageSent`. On the mutation,
`@InputArgument("message")` wires the GraphQL argument to the parameter —
the name is spelled out because the parameter is `message` while the
generated input type is `TextMessageInput`.

### The event stream

The centre of a chat server is a **sink**: a Reactor `Sinks.Many`, which is
a broadcasting channel that imperative code pushes into and reactive code
reads from.

```kotlin
private val sink: Many<TextMessage> = Sinks.many().replay().limit(100)
```

`Sinks.many()` creates a multi-subscriber sink — every open browser tab
gets its own subscription to the same stream. `.replay()` buffers what has
been emitted, so a client that joins late still receives the recent
messages instead of waiting for the next one to arrive. `.limit(100)` caps
that buffer at a hundred messages: a WebSocket connection is long-lived, so
a client that dropped briefly can catch up in one burst rather than miss
the conversation. Step 7 narrows the buffer once the stream turns out to be
short-lived.

```kotlin
sink.emitNext(msg, Sinks.EmitFailureHandler.FAIL_FAST)
```

The mutation emits into the sink, and every subscriber receives the message
— nobody had to ask for it. `FAIL_FAST` means that if the sink is already
closed or terminated the emission fails immediately, instead of queueing a
message that will never be delivered.

The subscription fetcher just hands the sink out as a `Flux`:

```kotlin
@DgsSubscription
fun messageSent(): Publisher<TextMessage> = sink.asFlux()
```

!!! note
    The `STORE` companion object is a plain `mutableMapOf`, which is not
    thread-safe. That is fine for a local sample where the traffic is a
    handful of browser tabs. A real server wants a `ConcurrentHashMap` or
    an actual database.

### The LocalDateTime scalar

A custom scalar needs a matching implementation on the server. Create a
`LocalDateTimeScalar` class in package `com.example.demo`:

```kotlin
@DgsScalar(name = "LocalDateTime")
class LocalDateTimeScalar : Coercing<LocalDateTime, String> {

    @Throws(CoercingSerializeException::class)
    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String? {
        if (dataFetcherResult is LocalDateTime) {
            return dataFetcherResult.format(DateTimeFormatter.ISO_DATE_TIME)
        }
        throw CoercingSerializeException("Not a valid DateTime")
    }

    @Throws(CoercingParseValueException::class)
    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): LocalDateTime? {
        if (input is String) {
            return LocalDateTime.parse(input, DateTimeFormatter.ISO_DATE_TIME)
        }
        throw CoercingParseLiteralException("Can not parse DateTime")
    }
    // parseLiteral and valueToLiteral follow the same pattern
}
```

`@DgsScalar` registers it with the schema. A `Coercing` implementation has
three jobs, and this class covers all of them: `serialize` turns a server
value into a response value, `parseValue` turns a client variable into a
server value, and `parseLiteral` does the same for a literal written
inside the document. Formatting through `DateTimeFormatter.ISO_DATE_TIME`
means the Angular client receives a plain string such as
`"2026-11-15T09:30:00.123"`.

### Let the browser reach the server

The Angular dev server runs on port 4200 and the GraphQL server on 8080,
so every request the browser makes is cross-origin — and the browser
blocks the response unless the server opts in. Add a `WebMvcConfigurer`:

```kotlin
@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
    }
}
```

!!! note
    This mapping allows any origin, which is convenient locally and wrong
    in production. Narrow it to the origins you actually serve, for
    example
    `registry.addMapping("/graphql").allowedOrigins("http://localhost:4200")`.

### Configure the application

`src/main/resources/application.properties`:

```properties
logging.level.com.example=DEBUG
logging.level.web=DEBUG
logging.level.reactor.core.publisher=TRACE

spring.graphql.websocket.path=/graphql
spring.graphql.websocket.connection-init-timeout=30s
spring.graphql.websocket.keep-alive=60s
```

The three `spring.graphql.websocket.*` properties enable and tune the
WebSocket endpoint. `path` switches it on and gives it an address — the
same `/graphql` path as the HTTP endpoint, since the upgrade header is
what tells the server which protocol to speak. `connection-init-timeout`
caps how long the server waits for the client's `connection_init` message
before hanging up. `keep-alive` has the server send a `ping` frame on an
idle connection, which is exactly the case a proxy is most likely to drop.

## Step 5: Build the Angular client

One page: a list of messages, a text field, and a send button. Under the
hood it is **Apollo Client** wired into Angular through **apollo-angular**,
with the subscription routed to a WebSocket transport link.

### Create the project

Generate the Angular application with the CLI:

```bash
ng new dgs-subscription-ui --style=scss --routing=false --skip-git
```

Add Angular Material for the form controls, then pull in the GraphQL
dependencies:

```bash
npm install @apollo/client apollo-angular graphql graphql-ws
```

| Package | Role |
| ------- | ---- |
| `@apollo/client` | The cache and the link infrastructure |
| `apollo-angular` | The Angular integration and the `gql` tag |
| `graphql` | The document AST that the links print |
| `graphql-ws` | The GraphQL over WebSocket client |

### The endpoint configuration

`src/app/config.ts`:

```typescript
export const GRAPHQL_HTTP_URL = 'http://localhost:8080/graphql';
export const GRAPHQL_WS_URL = 'ws://localhost:8080/graphql';
```

Two URLs, because there are two protocols. Queries and mutations travel
over HTTP; the subscription travels over the socket. The paths are
identical — the scheme and the upgrade header are what tell them apart.

### The GraphQL documents

`src/app/graphql/messages.ts`:

```typescript
import { gql } from 'apollo-angular';

export interface TextMessage {
  id: string;
  body: string;
  sentAt: string | null;
}

export interface MessagesData {
  messages: TextMessage[];
}

export interface SendMessageData {
  send: TextMessage | null;
}

export interface MessageSentData {
  messageSent: TextMessage;
}

export const MESSAGES_QUERY = gql`
  query allMessages {
    messages {
      id
      body
      sentAt
    }
  }
`;

export const SEND_MESSAGE_MUTATION = gql`
  mutation sendMessage($message: TextMessageInput!) {
    send(message: $message) {
      id
      body
      sentAt
    }
  }
`;

export const MESSAGE_SENT_SUBSCRIPTION = gql`
  subscription onMessageSent {
    messageSent {
      id
      body
      sentAt
    }
  }
`;

export function mergeNewMessage(
  prev: MessagesData,
  options: { subscriptionData: { data?: MessageSentData | null } },
): MessagesData {
  const incoming = options.subscriptionData.data?.messageSent;
  if (!incoming) {
    return prev;
  }
  return { messages: [...(prev?.messages ?? []), incoming] };
}

export function describeError(err: unknown): string {
  if (Array.isArray(err)) {
    return err.map(describeError).join('; ');
  }
  if (err instanceof Error) {
    return err.message;
  }
  if (err && typeof err === 'object' && 'message' in err) {
    return String((err as { message: unknown }).message);
  }
  return String(err);
}
```

One document per operation. `mergeNewMessage` is the function that folds
subscription results into the query result, and `describeError` flattens
the several shapes an Apollo error can take into a single line of text for
the banner.

### The WebSocket transport link

Apollo ships a subscription link for WebSocket, so the transport comes down
to a few lines. `src/app/transport/ws-link.ts`:

```typescript
import { GraphQLWsLink } from '@apollo/client/link/subscriptions';
import { createClient, type ClientOptions } from 'graphql-ws';
import { BehaviorSubject } from 'rxjs';

export type ConnectionState = 'idle' | 'connecting' | 'open' | 'closed' | 'error';

// A dropped socket does not surface through QueryRef.valueChanges, so the transport
// publishes its own state for the component to render as a banner.
export const connectionState$ = new BehaviorSubject<ConnectionState>('idle');

export const wsClientListeners: NonNullable<ClientOptions['on']> = {
  connecting: () => connectionState$.next('connecting'),
  connected: () => connectionState$.next('open'),
  closed: () => connectionState$.next('closed'),
  error: () => connectionState$.next('error'),
};

export function createWsLink(url: string): GraphQLWsLink {
  return new GraphQLWsLink(
    createClient({ url, on: wsClientListeners }),
  );
}
```

`createClient` from `graphql-ws` opens the socket, performs the
`graphql-transport-ws` handshake, and deals with the `ping`/`pong`
heartbeats. The four listeners publish the connection state so the
component can render a banner — that is user story 4, and it is the only
reason this file exists.

### Wire up Apollo

`src/app/app.config.ts`:

```typescript
import { provideHttpClient } from '@angular/common/http';
import { inject, provideZonelessChangeDetection, type EnvironmentProviders, type Provider } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { InMemoryCache, split } from '@apollo/client';
import { getMainDefinition } from '@apollo/client/utilities';
import { provideApollo } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';
import { GRAPHQL_HTTP_URL, GRAPHQL_WS_URL } from './config';
import { createWsLink } from './transport/ws-link';

export function appProviders(): (Provider | EnvironmentProviders)[] {
  return [
    provideZonelessChangeDetection(),
    provideAnimationsAsync(),
    provideHttpClient(),
    provideApollo(() => {
      const httpLink = inject(HttpLink).create({ uri: GRAPHQL_HTTP_URL });
      const wsLink = createWsLink(GRAPHQL_WS_URL);

      return {
        link: split(
          ({ query }) => {
            const definition = getMainDefinition(query);
            return (
              definition.kind === 'OperationDefinition' &&
              definition.operation === 'subscription'
            );
          },
          wsLink,
          httpLink,
        ),
        cache: new InMemoryCache(),
      };
    }),
  ];
}
```

The **split link** does the routing. It inspects every incoming operation
and sends subscriptions down the WebSocket link while everything else goes
over HTTP. One Apollo client, two transports, and the decision made per
operation instead of per endpoint.

### The chat component

`src/app/app.component.ts`:

```typescript
import { AsyncPipe } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Apollo } from 'apollo-angular';
import { BehaviorSubject, catchError, map, Observable, of, shareReplay } from 'rxjs';
import {
  MESSAGES_QUERY,
  MESSAGE_SENT_SUBSCRIPTION,
  SEND_MESSAGE_MUTATION,
  describeError,
  mergeNewMessage,
  type MessageSentData,
  type MessagesData,
  type SendMessageData,
  type TextMessage,
} from './graphql/messages';
import { connectionState$, type ConnectionState } from './transport/ws-link';

interface QueryState {
  loading: boolean;
  messages: TextMessage[];
  error: string | null;
}

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly apollo = inject(Apollo);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({ body: ['', Validators.required] });

  private readonly messagesQueryRef = this.apollo.watchQuery<MessagesData>({
    query: MESSAGES_QUERY,
  });

  // One shared subscription feeds both the list and the error banner, so the
  // query is issued once no matter how many async pipes read it.
  private readonly queryState$: Observable<QueryState> = this.messagesQueryRef.valueChanges.pipe(
    map(result => ({
      loading: result.loading,
      // Apollo types an in-flight result as a deep partial; a resolved result is complete.
      messages: (result.data?.messages ?? []) as TextMessage[],
      error: result.error ? describeError(result.error) : null,
    })),
    catchError(err =>
      of<QueryState>({ loading: false, messages: [], error: describeError(err) }),
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly messages$: Observable<TextMessage[] | null> = this.queryState$.pipe(
    map(state => (state.loading ? null : state.messages)),
  );

  readonly error$: Observable<string | null> = this.queryState$.pipe(
    map(state => state.error),
  );

  readonly connectionState$: Observable<ConnectionState> = connectionState$;

  private readonly sendingSubject = new BehaviorSubject<boolean>(false);
  readonly sending$: Observable<boolean> = this.sendingSubject.asObservable();

  private readonly sendErrorSubject = new BehaviorSubject<string | null>(null);
  readonly sendError$: Observable<string | null> = this.sendErrorSubject.asObservable();

  private stopSubscription?: () => void;

  ngOnInit(): void {
    // Pushed messages are merged into the query result, so no separate stream is
    // rendered; the cache broadcast is what updates the list.
    this.stopSubscription = this.messagesQueryRef.subscribeToMore<MessageSentData>({
      document: MESSAGE_SENT_SUBSCRIPTION,
      updateQuery: (prev, { subscriptionData }) =>
        mergeNewMessage(
          { messages: (prev?.messages ?? []) as TextMessage[] },
          { subscriptionData },
        ),
    });
  }

  ngOnDestroy(): void {
    this.stopSubscription?.();
  }

  send(): void {
    const body = this.form.controls.body.value.trim();
    if (!body) {
      return;
    }

    this.sendingSubject.next(true);
    this.sendErrorSubject.next(null);

    this.apollo
      .mutate<SendMessageData>({
        mutation: SEND_MESSAGE_MUTATION,
        variables: { message: { body } },
      })
      .subscribe({
        next: result => {
          this.sendingSubject.next(false);
          if (result.error) {
            this.sendErrorSubject.next(describeError(result.error));
            return;
          }
          // the new message arrives through the subscription, not from this response
          this.form.controls.body.reset('');
        },
        error: err => {
          this.sendingSubject.next(false);
          this.sendErrorSubject.next(describeError(err));
        },
      });
  }
}
```

Three moving parts. The **query** is watched with `watchQuery`, so Apollo
keeps it subscribed and re-reads it whenever the cache changes. The
**subscription** hangs off that query with `subscribeToMore`: each pushed
message is merged into the query result by `mergeNewMessage`, and the
cache broadcast is what re-renders the list — the component never renders
a separate stream of its own. The **mutation** sends the message, and
notice that it does not add anything to the list; the subscription does
that. The mutation response is used only to clear the form and to surface
errors.

`subscribeToMore` hands back a teardown function, and `ngOnDestroy` calls
it so the socket closes along with the component. Skip that and the
connection leaks for the rest of the page's life.

The template:

```html
<main class="container">
  <h1>DGS Subscription — WebSocket</h1>

  @if (connectionState$ | async; as state) {
    @if (state !== 'open' && state !== 'idle') {
      <p class="banner" role="status">Subscription {{ state }}</p>
    }
  }

  @if (error$ | async; as err) {
    <p class="banner banner-error" role="alert">{{ err }}</p>
  }

  @if (messages$ | async; as messages) {
    <ul class="messages">
      @for (m of messages; track m.id) {
        <li>
          <span class="body">{{ m.body }}</span>
          <time class="sent-at">{{ m.sentAt }}</time>
        </li>
      } @empty {
        <li class="empty">No messages yet.</li>
      }
    </ul>
  } @else {
    <ul class="messages">
      <li class="empty">Loading messages…</li>
    </ul>
  }

  <form class="message-box" [formGroup]="form" (ngSubmit)="send()">
    <mat-form-field class="message-field">
      <mat-label>Message</mat-label>
      <input matInput formControlName="body" name="body" autocomplete="off" />
      @if (form.controls.body.hasError('required')) {
        <mat-error>Message body can not be empty.</mat-error>
      }
    </mat-form-field>

    <button
      mat-icon-button
      type="submit"
      [disabled]="form.invalid || (sending$ | async)"
      aria-label="Send message"
    >
      <mat-icon>send</mat-icon>
    </button>
  </form>

  @if (sendError$ | async; as err) {
    <p class="banner banner-error" role="alert">{{ err }}</p>
  }
</main>
```

The banner at the top is user story 4 in the UI: whenever the transport is
not open, the page says so.

## Step 6: Run the server and the client

Time to use the thing.

### Start the server

```bash
cd dgs-subscription-ws
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run` instead. The chat server has no
database, so there is no Docker step to worry about.

Once the log shows `Started DemoApplication`, the server is listening on
`http://localhost:8080`.

### Start the client

```bash
cd dgs-subscription-ui
npm install
npm start
```

Open `http://localhost:4200` and you should see an empty message list
above the text field.

### Try it

1. Type something and hit send. It shows up in the list and the field
   clears.
2. Open a second tab on `http://localhost:4200`. Send a message in one tab
   and it appears in the other. That is the subscription at work — the
   server pushed the message to every connected subscriber, and neither
   tab asked for it.
3. Kill the server. The connection banner appears once the socket closes.
   Bring the server back and reload the page to pick the connection up
   again.

The query and the mutation are ordinary HTTP requests to the same
endpoint, so `curl` handles them without ceremony. List the stored
messages:

```bash
curl http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ messages { id body sentAt } }"}'
```

Send a message:

```bash
curl http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation sendMessage($message: TextMessageInput!) { send(message: $message) { id body sentAt } }",
    "variables": { "message": { "body": "Hello from curl" } }
  }'
```

A WebSocket subscription is harder to inspect from a shell, because the
`graphql-transport-ws` protocol opens with a `connection_init` handshake
before any operation can be sent. The browser's network tab shows the
socket and its frames, the integration test in Step 9 exercises the full
handshake, and a tool such as `websocat` lets you drive the protocol by
hand if you want to read the raw frames. The SSE version in Step 7 is the
one that a single `curl` command can watch.

GraphiQL lives at `http://localhost:8080/graphiql` — handy for the query
and the mutation, with completion and schema documentation.

## Step 7: Switch to SSE subscriptions

The WebSocket version works, and it is the richer protocol — the socket is
bidirectional, so the client could push over the same connection later. It
is also the heavier one to operate. A WebSocket is not an ordinary HTTP
request, so every proxy, load balancer, and corporate firewall between the
browser and the server has to cooperate with the upgrade handshake. When
one of them does not, the chat quietly stops receiving messages.

**Server-Sent Events** remove that whole category of problem. An SSE
subscription is a normal HTTP request whose response stays open, and the
server writes one event per message. Nothing upgrades and nothing
negotiates a second protocol, so the HTTP infrastructure you already run
handles it as-is.

The server changes are minor. The client changes are the interesting part,
because Apollo has no SSE link and you write one.

### The server

The SSE subscription is served by the HTTP endpoint, so the WebSocket
starter is no longer needed. Remove it from the POM:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

Leaving it in place does no harm — the WebSocket endpoint simply goes
unused — but the `dgs-subscription-sse` sample module drops it, and so
should you.

The WebSocket properties give way to the one that bounds an SSE response:

```properties
logging.level.com.example=DEBUG
logging.level.web=DEBUG
logging.level.com.netflix.graphql.dgs=TRACE

spring.mvc.async.request-timeout=45000
```

An SSE response is an asynchronous servlet request that stays open for as
long as the subscription lives, and `spring.mvc.async.request-timeout`
bounds it: after 45 seconds the server ends the stream.

The `graphql-sse` client treats that as a retryable disconnect and
reconnects on its own — five attempts with a randomised exponential backoff
by default, tunable through the `retryAttempts` and `retry` options. A
client that reconnects picks up the most recent message from the sink's
replay buffer, so narrow the buffer to match the shorter stream:

```kotlin
private val sink: Many<TextMessage> = Sinks.many().replay().latest()
```

Holding a hundred messages for a connection that lives 45 seconds wastes
memory, and replaying a hundred messages to every reconnecting client
wastes traffic. The latest message is enough to show that the stream is
alive again.

### The client

Swap the transport package:

```bash
npm uninstall graphql-ws
npm install graphql-sse
```

The WebSocket URL is gone, because the SSE subscription uses the HTTP
endpoint. `src/app/config.ts`:

```typescript
export const GRAPHQL_HTTP_URL = 'http://localhost:8080/graphql';
```

Now the interesting part. Apollo has no SSE subscription link, so you
write one: an `ApolloLink` that turns an Apollo operation into a request
for the `graphql-sse` client and feeds the results back.
`src/app/transport/sse-link.ts`:

```typescript
import { ApolloLink, type FetchResult, type Operation } from '@apollo/client';
import { print, type DocumentNode } from 'graphql';
import { createClient, type Client, type ClientOptions } from 'graphql-sse';
import { BehaviorSubject, Observable } from 'rxjs';

export type ConnectionState = 'idle' | 'connecting' | 'open' | 'closed' | 'error';

// A dropped stream does not surface through QueryRef.valueChanges, so the transport
// publishes its own state for the component to render as a banner.
export const connectionState$ = new BehaviorSubject<ConnectionState>('idle');

// graphql-sse exposes only connecting/message/connected at client level. The closed
// and error transitions are detected in the link's sink instead, see request() below.
export const sseClientListeners: NonNullable<ClientOptions<false>['on']> = {
  connecting: () => connectionState$.next('connecting'),
  connected: () => connectionState$.next('open'),
};

export function createSseClient(url: string): Client {
  return createClient({ url, on: sseClientListeners });
}

export class GraphQLSSELink extends ApolloLink {
  constructor(private readonly client: Client) {
    super();
  }

  override request(operation: Operation): Observable<FetchResult> {
    return new Observable<FetchResult>(observer => {
      const dispose = this.client.subscribe(
        {
          query: print(operation.query as DocumentNode),
          variables: operation.variables,
          operationName: operation.operationName,
          extensions: operation.extensions,
        },
        {
          next: result => observer.next(result as unknown as FetchResult),
          error: err => {
            connectionState$.next('error');
            observer.error(Array.isArray(err) ? err : [err]);
          },
          complete: () => {
            connectionState$.next('closed');
            observer.complete();
          },
        },
      );

      return dispose;
    });
  }
}
```

Three jobs. The link converts an Apollo operation into the request object
`graphql-sse` expects, printing the document AST back into a query string.
The `next`, `error`, and `complete` callbacks of the SSE sink are
forwarded to the RxJS observer, which is what makes this an ordinary
Apollo link. And the dispose function that comes back is the observable's
teardown, so unsubscribing closes the stream.

Compare it with the WebSocket link. There, Apollo did the translation and
the `graphql-ws` package reported all four connection transitions. Here the
`graphql-sse` package only reports `connecting` and `connected`, so the
link has to spot `closed` and `error` itself in the sink. That is the
price of having no first-party Apollo link — roughly forty lines.

Point the application config at the new link:

```typescript
import { provideHttpClient } from '@angular/common/http';
import { inject, provideZonelessChangeDetection, type EnvironmentProviders, type Provider } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { InMemoryCache, split } from '@apollo/client';
import { getMainDefinition } from '@apollo/client/utilities';
import { provideApollo } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';
import { GRAPHQL_HTTP_URL } from './config';
import { GraphQLSSELink, createSseClient } from './transport/sse-link';

export function appProviders(): (Provider | EnvironmentProviders)[] {
  return [
    provideZonelessChangeDetection(),
    provideAnimationsAsync(),
    provideHttpClient(),
    provideApollo(() => {
      const httpLink = inject(HttpLink).create({ uri: GRAPHQL_HTTP_URL });
      const sseLink = new GraphQLSSELink(createSseClient(GRAPHQL_HTTP_URL));

      return {
        link: split(
          ({ query }) => {
            const definition = getMainDefinition(query);
            return (
              definition.kind === 'OperationDefinition' &&
              definition.operation === 'subscription'
            );
          },
          sseLink,
          httpLink,
        ),
        cache: new InMemoryCache(),
      };
    }),
  ];
}
```

The component does not change at all. The split link still routes
subscriptions to the transport link; the transport link just happens to be
an SSE link now. That is the payoff of hiding the transport behind a link:
the UI does not know or care which protocol carries the subscription.

### Try it again

Stop the WebSocket server, start the SSE one, and restart the client:

```bash
cd dgs-subscription-sse
./mvnw spring-boot:run
```

```bash
cd dgs-subscription-ui-sse
npm install
npm start
```

The chat behaves the same. The difference shows up in the network tab: the
browser now holds an event-stream response open instead of a WebSocket.

The subscription is also much easier to watch from a shell now. `-N`
turns off curl's output buffering, and the `Accept: text/event-stream`
header is what selects the SSE transport on the server:

```bash
curl -N http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"query": "subscription { messageSent { id body sentAt } }"}'
```

The response stays open and prints one event per message. The framing
follows the
[GraphQL over SSE protocol](https://github.com/enisdenjo/graphql-sse/blob/master/PROTOCOL.md):
a `next` event whose `data:` line carries the execution result, followed
by a `complete` event when the stream ends.

```text
event:next
data:{"data":{"messageSent":{"id":"3a2f...","body":"Hello from curl","sentAt":"2026-11-15T09:30:00.123"}}}
```

Run the send mutation from another terminal while the subscription is open
and the event shows up right away. `Ctrl+C` ends the subscription.

### What changed, and what did not

| | WebSocket | SSE |
| - | --- | --- |
| Protocol | `graphql-transport-ws` | GraphQL over SSE |
| Direction | both directions | server to client only |
| Subscription URL | `ws://localhost:8080/graphql` | `http://localhost:8080/graphql` |
| Client library | `graphql-ws` | `graphql-sse` |
| Server dependency | `spring-boot-starter-websocket` | none beyond `spring-boot-starter-webmvc` |
| Server configuration | `spring.graphql.websocket.*` | `spring.mvc.async.request-timeout` |
| Replay on reconnect | last 100 messages | the latest message |
| Client link code | Apollo's `GraphQLWsLink`, about 10 lines | a hand-written `ApolloLink`, about 40 lines |
| Proxy and firewall | needs the upgrade handshake | ordinary HTTP |

!!! warning
    The two WebSocket protocols are not interchangeable. The
    `graphql-transport-ws` subprotocol used here is maintained, while the
    older `subscriptions-transport-ws` protocol is not. A client speaking
    one cannot talk to a server speaking the other, so migrate the client
    and the server together.

## Step 8: Switch the Angular client to signals

The component version works, but it threads state through RxJS observables
and `async` pipes. The signals version says the same thing with signals
and the `rxResource` interop API.

### The transport publishes a signal

The only change in the transport is the state holder.
`src/app/transport/ws-link.ts`:

```typescript
import { signal, type WritableSignal } from '@angular/core';
import { GraphQLWsLink } from '@apollo/client/link/subscriptions';
import { createClient, type ClientOptions } from 'graphql-ws';

export type ConnectionState = 'idle' | 'connecting' | 'open' | 'closed' | 'error';

export const connectionState: WritableSignal<ConnectionState> = signal<ConnectionState>('idle');

export const wsClientListeners: NonNullable<ClientOptions['on']> = {
  connecting: () => connectionState.set('connecting'),
  connected: () => connectionState.set('open'),
  closed: () => connectionState.set('closed'),
  error: () => connectionState.set('error'),
};

export function createWsLink(url: string): GraphQLWsLink {
  return new GraphQLWsLink(
    createClient({ url, on: wsClientListeners }),
  );
}
```

A `WritableSignal` takes the place of the `BehaviorSubject`. The SSE
variant of the signals application makes the same swap in its
`sse-link.ts`, and its `GraphQLSSELink` is otherwise unchanged.

### The component

`src/app/app.component.ts`:

```typescript
import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Apollo } from 'apollo-angular';
import { catchError, filter, map, of } from 'rxjs';
import {
  MESSAGES_QUERY,
  MESSAGE_SENT_SUBSCRIPTION,
  SEND_MESSAGE_MUTATION,
  describeError,
  type MessageSentData,
  type MessagesData,
  type SendMessageData,
} from './graphql/messages';
import { connectionState } from './transport/ws-link';

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
})
export class AppComponent {
  private readonly apollo = inject(Apollo);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({ body: ['', Validators.required] });

  // ── Query as a one-time resource ──────────────────────────────────────
  // rxResource accepts Observables via 'stream'; the Apollo query Observable
  // emits once and completes, which is valid here.
  readonly messagesResource = rxResource({
    stream: () =>
      this.apollo.query<MessagesData>({ query: MESSAGES_QUERY }).pipe(
        map(r => r.data?.messages ?? []),
      ),
  });

  // ── Subscription as a streaming resource ──────────────────────────────
  // rxResource accepts Observables natively in 'stream', so no toSignal() bridge
  // is needed. Each emission from the Apollo subscription updates the resource value.
  readonly subscriptionResource = rxResource({
    stream: () =>
      this.apollo.subscribe<MessageSentData>({ query: MESSAGE_SENT_SUBSCRIPTION }).pipe(
        filter(result => !!result.data),
        map(result => (result.data as MessageSentData).messageSent ?? null),
        catchError(() => of(null)),
      ),
  });

  // ── Computed signals ──────────────────────────────────────────────────
  readonly messages = computed(() => {
    const initial = this.messagesResource.hasValue() ? this.messagesResource.value()! : [];
    const newMessage = this.subscriptionResource.hasValue()
      ? this.subscriptionResource.value()
      : null;
    if (!newMessage) return initial;
    return [...initial, newMessage];
  });

  readonly loading = computed(() => this.messagesResource.isLoading());
  readonly error = computed(() => {
    const err = this.messagesResource.error();
    return err ? describeError(err) : null;
  });

  readonly connection = connectionState;

  readonly sending = signal(false);
  readonly sendError = signal<string | null>(null);

  send(): void {
    const body = this.form.controls.body.value.trim();
    if (!body) {
      return;
    }

    this.sending.set(true);
    this.sendError.set(null);

    this.apollo
      .mutate<SendMessageData>({
        mutation: SEND_MESSAGE_MUTATION,
        variables: { message: { body } },
      })
      .subscribe({
        next: result => {
          this.sending.set(false);
          if (result.error) {
            this.sendError.set(describeError(result.error));
            return;
          }
          // the new message arrives through the subscription, not from this response
          this.form.controls.body.reset('');
        },
        error: err => {
          this.sending.set(false);
          this.sendError.set(describeError(err));
        },
      });
  }
}
```

The template keeps the same shape and swaps the pipe syntax for call
syntax:

```html
<main class="container">
  <h1>DGS Subscription — WebSocket</h1>

  @if (connection() !== 'open' && connection() !== 'idle') {
    <p class="banner" role="status">Subscription {{ connection() }}</p>
  }

  @if (error(); as err) {
    <p class="banner banner-error" role="alert">{{ err }}</p>
  }

  <ul class="messages">
    @for (m of messages(); track m.id) {
      <li>
        <span class="body">{{ m.body }}</span>
        <time class="sent-at">{{ m.sentAt }}</time>
      </li>
    } @empty {
      <li class="empty">
        @if (loading()) { Loading messages… } @else { No messages yet. }
      </li>
    }
  </ul>

  <form class="message-box" [formGroup]="form" (ngSubmit)="send()">
    <mat-form-field class="message-field">
      <mat-label>Message</mat-label>
      <input matInput formControlName="body" name="body" autocomplete="off" />
      @if (form.controls.body.hasError('required')) {
        <mat-error>Message body can not be empty.</mat-error>
      }
    </mat-form-field>

    <button
      mat-icon-button
      type="submit"
      [disabled]="form.invalid || sending()"
      aria-label="Send message"
    >
      <mat-icon>send</mat-icon>
    </button>
  </form>

  @if (sendError(); as err) {
    <p class="banner banner-error" role="alert">{{ err }}</p>
  }
</main>
```

Side by side, the two versions differ in four places:

| Concern | Components | Signals |
| ------- | ---------- | ------- |
| Query result | `watchQuery` + `valueChanges` | `rxResource` with `stream` |
| Subscription | `subscribeToMore` merged into the cache | `rxResource` with `stream` |
| Derived list | `map` in an RxJS pipeline | `computed` |
| Connection state | `BehaviorSubject` + `async` pipe | `WritableSignal` |
| Transient UI state | `BehaviorSubject` | `signal` |
| Reading in the template | `messages$ \| async` | `messages()` |

!!! note
    One behaviour differs, and it is worth knowing about. The components
    version merges every pushed message into the query result, so the list
    keeps growing. The signals version appends only the **latest** pushed
    message to the initial list, because a resource holds just the most
    recent value of its stream. To accumulate everything, keep the pushed
    messages in a signal array and append to it inside the stream pipeline:

    ```typescript
    readonly pushed = signal<TextMessage[]>([]);

    readonly subscriptionResource = rxResource({
      stream: () =>
        this.apollo.subscribe<MessageSentData>({ query: MESSAGE_SENT_SUBSCRIPTION }).pipe(
          filter(result => !!result.data),
          map(result => (result.data as MessageSentData).messageSent ?? null),
          tap(message => {
            if (message) {
              this.pushed.update(list => [...list, message]);
            }
          }),
        ),
    });

    readonly messages = computed(() => [
      ...(this.messagesResource.hasValue() ? this.messagesResource.value()! : []),
      ...this.pushed(),
    ]);
    ```

    The Apollo cache stays the source of truth for the query; the signal
    array only carries what has arrived over the subscription since the
    page loaded.

## Step 9: Test the subscriptions

Clicking around is a poor way to test a subscription. The samples test it
on the server instead.

### Test the subscription stream

`DgsQueryExecutor` runs a subscription and hands back the publisher, which
Reactor's `StepVerifier` can assert against. This test is transport
agnostic — it exercises the fetcher, not the protocol. Create a
`SubscriptionTest` class in `src/test/kotlin/com/example/demo`:

```kotlin
@SpringBootTest
@EnableDgsTest
class SubscriptionTest {

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Test
    fun testMessages() {
        val executionResult = dgsQueryExecutor.execute("subscription { messageSent { body } }")
        val publisher = executionResult.getData<Publisher<ExecutionResult>>()

        val verifier = StepVerifier.create(publisher)
            .consumeNextWith {
                assertThat(
                    it.getData<Map<String, Map<String, Any>>>()["messageSent"]
                        ?.get("body") as String
                ).contains("text1")
            }
            .consumeNextWith {
                assertThat(
                    it.getData<Map<String, Map<String, Any>>>()["messageSent"]
                        ?.get("body") as String
                ).contains("text2")
            }
            .thenCancel()
            .verifyLater()

        val sendText1 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation sendMessage(\$msg: TextMessageInput!) { send(message:\$msg) { body}}",
            "data.send.body",
            mapOf("msg" to (mapOf("body" to "text1 message")))
        )
        assertThat(sendText1).contains("text1")

        val sendText2 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation sendMessage(\$msg: TextMessageInput!) { send(message:\$msg) { body}}",
            "data.send.body",
            mapOf("msg" to (mapOf("body" to "text2 message")))
        )
        assertThat(sendText2).contains("text2")

        // verify it now.
        verifier.verify()
    }
}
```

The ordering is the whole trick. Start the verifier with `verifyLater()`,
send the messages, and only then call `verify()`: the subscription has to
be registered before the mutations run, or the first message is emitted
while nobody is listening.

### Test over the transport

The `DgsQueryExecutor` test never touches the network, so it says nothing
about the protocol. The integration tests do.

The DGS client library ships two generations of every client. The
`Dgs`-prefixed names — `DgsRestClientGraphQLClient`,
`DgsWebClientGraphQLClient`, and `DgsGraphqlSSESubscriptionGraphQLClient` —
are the Jackson 3 variants that match DGS 12 and Spring Boot 4. The
unprefixed names — `RestClientGraphQLClient`, `WebClientGraphQLClient`,
and `GraphqlSSESubscriptionGraphQLClient` — are their Jackson 2
predecessors. Use the prefixed ones in new code.

For WebSocket, Spring GraphQL ships its own `WebSocketGraphQlClient`:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {

    @LocalServerPort
    var port = 0

    lateinit var webClient: DgsWebClientGraphQLClient
    lateinit var socketClient: WebSocketGraphQlClient

    @BeforeEach
    fun setup() {
        val baseUrl = "http://localhost:$port/graphql"
        this.webClient = DgsWebClientGraphQLClient(WebClient.create(baseUrl))

        val subscriptionUrl = "ws://localhost:$port/graphql"
        this.socketClient = WebSocketGraphQlClient.create(
            URI.create(subscriptionUrl),
            ReactorNettyWebSocketClient()
        )
        this.socketClient.start().subscribe()
    }

    @AfterEach
    fun teardown() {
        this.socketClient.stop().subscribe()
    }

    @Test
    fun testMessages() {
        val executionResult = socketClient.document("subscription { messageSent { body } }")
            .executeSubscription()
            .map {
                val messageSent = it.getData<Map<String, Any>>()!!["messageSent"]!! as Map<String, Any>
                messageSent["body"] as String
            }

        val verifier = StepVerifier.create(executionResult)
            .thenAwait(Duration.ofMillis(1500))
            .consumeNextWith { assertThat(it).isEqualTo("text1") }
            .consumeNextWith { assertThat(it).isEqualTo("text2") }
            .thenCancel()
            .verifyLater()

        // give the websocket subscription time to register on the server before sending,
        // otherwise the first message may be emitted before the subscription is active
        Thread.sleep(1000)

        // send the messages over HTTP, then
        verifier.verify()
    }
}
```

For SSE, DGS ships a client for the GraphQL over SSE protocol:

```kotlin
package com.example.demo

import com.jayway.jsonpath.TypeRef
import com.netflix.graphql.dgs.client.DgsGraphqlSSESubscriptionGraphQLClient
import com.netflix.graphql.dgs.client.DgsRestClientGraphQLClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {

    @LocalServerPort
    var port = 0;

    // The Dgs-prefixed clients are the Jackson 3 variants; GraphqlSSESubscriptionGraphQLClient and
    // RestClientGraphQLClient are their Jackson 2 predecessors.
    lateinit var sseClient: DgsGraphqlSSESubscriptionGraphQLClient
    lateinit var restClient: DgsRestClientGraphQLClient

    @BeforeEach
    fun setup() {
        val baseUrl = "http://localhost:$port/graphql"
        sseClient = DgsGraphqlSSESubscriptionGraphQLClient(
            baseUrl,
            WebClient.create(baseUrl)
        )
        restClient = DgsRestClientGraphQLClient(RestClient.builder().baseUrl(baseUrl).build())
    }

    @Test
    fun testMessages() {
        val subscriptionQuery = "subscription { messageSent { body } }"
        val variables = emptyMap<String, Any>()
        val result = sseClient.reactiveExecuteQuery(subscriptionQuery, variables).map {
            it.extractValueAsObject("data.messageSent", object : TypeRef<Map<String, Any>>() {})["body"] as String
        }

        val verifier = StepVerifier.create(result)
            .consumeNextWith { assertThat(it).isEqualTo("text1 message") }
            .consumeNextWith { assertThat(it).isEqualTo("text2 message") }
            .thenCancel()
            .verifyLater()

        val sendMessageQuery = "mutation sendMessage(\$msg: TextMessageInput!) { send(message:\$msg) { body}}"

        val sendText1 = restClient.executeQuery(
            sendMessageQuery,
            mapOf("msg" to (mapOf("body" to "text1 message")))
        ).extractValueAsObject("data.send.body", String::class.java)
        assertThat(sendText1).isEqualTo("text1 message")

        val sendText2 = restClient.executeQuery(
            sendMessageQuery,
            mapOf("msg" to (mapOf("body" to "text2 message")))
        ).extractValueAsObject("data.send.body", String::class.java)
        assertThat(sendText2).isEqualTo("text2 message")

        //verify it now.
        verifier.verify();

        val allMessagesQuery = " { messages { body }}"

        val allMessages = restClient.executeQuery(allMessagesQuery, emptyMap<String, Any>())
            .extractValueAsObject("data.messages[*].body", object : TypeRef<List<String>>() {})
        assertThat(allMessages).allMatch { s: String -> s.contains("message") }
    }
}
```

Both transports have the same race: the subscription must be active on the
server before the mutation runs. The WebSocket test waits for it
explicitly, because the socket handshake adds a round trip that the
in-process test does not have.

### Test the client

The Angular applications have unit tests for the transport link and the
merge logic. Run them with:

```bash
cd dgs-subscription-ui
npm test -- --watch=false
```

The transport tests drive the transport sink directly with a fake client,
so they assert the state transitions, the forwarding of results, and the
disposal of the stream on unsubscribe — all without a server. The
`messages.spec.ts` test covers `mergeNewMessage` and `describeError`.

## Conclusion

You now have a working chat application on Netflix DGS and Angular, and you
have walked the whole path from user story to running system.

The use cases mapped onto three GraphQL operations — a query, a mutation,
and a subscription — and the connection state turned out to belong outside
the schema entirely.

On the server, a `@DgsComponent` class with a `Sinks.Many` broadcast
channel, a custom scalar, and a CORS mapping for the browser.

On the client, an Apollo cache with a split link that sends subscriptions
to the transport and everything else to HTTP.

WebSocket and SSE turn out to be interchangeable behind that link. The
WebSocket version needed Apollo's ready-made link; the SSE version needed
a hand-written one; the component never changed either way.

And signals replace the observables in the UI without touching what the
transport does.

Six modules in the
[spring-graphql-sample repository](https://github.com/hantsy/spring-graphql-sample)
implement exactly this:
[`dgs-subscription-ws`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ws),
[`dgs-subscription-ui`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui),
[`dgs-subscription-ui-signals`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui-signals),
[`dgs-subscription-sse`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-sse),
[`dgs-subscription-ui-sse`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui-sse),
and
[`dgs-subscription-ui-sse-signals`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui-sse-signals).

Where to go next:

- [Building GraphQL APIs with Netflix DGS](./start.md) covers the rest of
  the DGS programming model — DataLoaders, code generation, error handling.
- [An introduction to GraphQL and Spring](../intro.md) has the protocol
  details behind the two WebSocket protocols and the SSE specification.

Two related examples in the same repository: [dgs-webflux](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-webflux)
runs the same subscription on the reactive stack, and
[spring-graphql-webmvc-http](https://github.com/hantsy/spring-graphql-sample/tree/master/spring-graphql-webmvc-http)
serves the same schema with Spring GraphQL instead of DGS.
