# Create a Chat Application with Netflix DGS

Real-time chat is the smallest application that needs all three GraphQL
operation types: a **query** to load the history, a **mutation** to send a
message, and a **subscription** to push new messages to every connected
client. In this tutorial you build exactly that application with the Netflix
DGS framework on the server and Angular on the client.

You will build it twice. First over **Server-Sent Events (SSE)**, where the
subscription is an ordinary HTTP response that stays open. Then over
**WebSocket**, where the subscription rides a persistent socket. On the client
side you will build the UI twice as well: once with the classic Angular
component API, and once with **signals**.

The examples correspond to working modules in the
[spring-graphql-sample repository](https://github.com/hantsy/spring-graphql-sample):

| Module | Role |
| ------ | ---- |
| [`dgs-subscription-sse`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-sse) | DGS server, subscriptions over SSE |
| [`dgs-subscription-ws`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ws) | DGS server, subscriptions over WebSocket |
| [`dgs-subscription-ui-sse`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui-sse) | Angular client, components, SSE transport |
| [`dgs-subscription-ui-sse-signals`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui-sse-signals) | Angular client, signals, SSE transport |
| [`dgs-subscription-ui`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui) | Angular client, components, WebSocket transport |
| [`dgs-subscription-ui-signals`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui-signals) | Angular client, signals, WebSocket transport |

Code excerpts omit package declarations and imports when they are not needed
to understand the step.

## Prerequisites

Before you begin, install or prepare the following:

- Java 25. The examples target Spring Boot 4.1.2 and Kotlin 2.4.x.
- Node.js 24 and npm. The Angular clients use Angular 22 and the Vitest
  test runner.
- Git, if you want to clone and run the sample repository.

You do not need to install Maven separately; Spring Initializr generates a
Maven Wrapper with the project.

If you have not built a DGS application before, read
[Building GraphQL APIs with Netflix DGS](./start.md) first. That guide
explains the DGS programming model, the schema-first workflow, data fetchers,
DataLoaders, and error handling. This tutorial assumes you know those ideas
and reuses the same project layout, so the steps here stay focused on
subscriptions and the client.

## Step 1: Define the use cases

Start with the users, not the schema. A chat application is small enough to
reason about completely, and the use cases tell you which GraphQL operations
you actually need.

### User stories

**Story 1 — catch up on the conversation.** As a visitor, I want to see the
recent messages when I open the chat, so that I can follow the conversation
without asking anyone to repeat it.

**Story 2 — send a message.** As a visitor, I want to type a message and
send it, so that I can take part in the conversation.

**Story 3 — see messages arrive.** As a visitor, I want messages sent by
other people to appear without refreshing the page, so that the chat feels
alive.

**Story 4 — know when the live feed breaks.** As a visitor, I want to see
when the live connection drops, so that I do not sit in front of a list that
silently stopped updating.

**Story 5 — one contract.** As an operator, I want reads, writes, and live
updates served from a single endpoint against a single schema, so that the
client needs no second API and no second contract to maintain.

### From stories to operations

Map each story onto the operation type that satisfies it:

| Story | Operation | Field |
| ----- | --------- | ----- |
| 1 — catch up | Query | `messages` |
| 2 — send | Mutation | `send(message: TextMessageInput!)` |
| 3 — live updates | Subscription | `messageSent` |
| 4 — connection state | — | transport level, outside the schema |
| 5 — one contract | All three | the same `/graphql` endpoint |

Three observations shape the rest of the tutorial:

- Stories 1 and 2 are ordinary GraphQL operations. They work over plain HTTP
  and need no special server setup.
- Story 3 is the only one that needs a **long-lived connection**. This is
  where the transport decision — SSE or WebSocket — actually matters.
- Story 4 cannot be expressed in the schema. A subscription that silently
  dies looks exactly like a chat room where nobody is talking. The client
  therefore has to observe the transport directly, and every client in this
  tutorial renders a connection banner.

Because stories 1, 2, and 5 map onto the three root types, the chat schema
needs exactly one query, one mutation, and one subscription. Everything else
is detail.

## Step 2: Create the project

The chat server is the DGS application from
[Building GraphQL APIs with Netflix DGS](./start.md), with two differences:
it is written in Kotlin, and it has no database — the message history lives
in memory, which keeps the focus on the subscription stream.

Generate the project with [Spring Initializr](https://start.spring.io/):

1. Select Kotlin and Spring Boot 4.1.2.
2. Set the group to `com.example` and the artifact to `demo` to match the
   package names in the examples.
3. Add Spring Web and the DGS dependency. The generated POM needs the DGS
   BOM, the starter, and the code generation plugin:

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

The `dgs-starter` dependency configures DGS and its GraphQL execution
support, `spring-boot-starter-webmvc` provides the HTTP endpoint, and
`reactor-core` supplies the reactive types the subscription returns.

Configure the Kotlin compiler and the DGS code generation plugin in the
build section. The code generator reads the SDL and produces Kotlin data
classes and a typed Java client API, so the fetchers do not hand-write
types:

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

Running `mvnw generate-sources` (or `mvnw.cmd generate-sources` on Windows)
generates the types from the schema. The plugin writes the data classes into
`com.example.demo.gql.types` and the typed client API into
`com.example.demo.gql.client`; the examples use the former, and the latter
is what you would use when writing a Java or Kotlin client against this
schema.

## Step 3: Define the chat schema

**A GraphQL schema** is the contract that describes which data clients can
read or change and what each operation returns. DGS is schema-first: you
write that contract in the GraphQL Schema Definition Language (SDL), then
connect Kotlin code to its fields.

Create a `schema.graphql` file in `src/main/resources/schema`:

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

The schema follows directly from the user stories:

- `TextMessage` is the message type. `id` and `body` are required;
  `sentAt` is a `LocalDateTime`, a **custom scalar** the server defines
  for ISO-8601 timestamps.
- `Query.messages` serves story 1 and returns the whole history.
- `Mutation.send` serves story 2 and takes a `TextMessageInput`. The
  client sends only the body; the server assigns the id and the timestamp.
- `Subscription.messageSent` serves story 3. It takes no arguments, because
  a chat room delivers every message to every member.

Two details are worth pausing on. First, `send` returns the stored message,
but the client does not need that response to render the new message — the
subscription delivers it. The mutation response exists for correctness and
for error reporting, not for the happy path. Second, `messageSent` returns
a single message per event, not a list: a subscription delivers one result
at a time, and the stream of events is the list.

## Step 4: Implement the DGS data fetchers

**A data fetcher** resolves a GraphQL schema field. Mark the containing class
with `@DgsComponent`, then bind methods to schema fields with DGS
annotations. A **subscription** fetcher returns a reactive `Publisher`
rather than a value, and the transport forwards each emitted item to the
client.

Create a `MessagesDataFetcher` class in package `com.example.demo`:

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

    private val sink: Many<TextMessage> = Sinks.many().replay().latest()

    companion object {
        val STORE = mutableMapOf<String, TextMessage>()
    }
}
```

The annotations map directly onto the schema:

- `@DgsQuery` binds `messages()` to the `Query.messages` field.
- `@DgsMutation` binds `send(...)` to the `Mutation.send` field, and
  `@InputArgument("message")` binds the GraphQL argument to the parameter.
  The name is given explicitly because the parameter is `message` while the
  generated input type is `TextMessageInput`.
- `@DgsSubscription` binds `messageSent()` to the `Subscription.messageSent`
  field.

### The event stream

The heart of a chat server is the **sink** — a Reactor `Sinks.Many`, which
is a broadcasting channel that your imperative code pushes into and your
reactive code reads from.

```kotlin
private val sink: Many<TextMessage> = Sinks.many().replay().latest()
```

Three parts, each doing a job:

- `Sinks.many()` creates a multi-subscriber sink. Every connected browser
  gets its own subscription to the same stream.
- `.replay()` buffers emitted values so that a subscriber joining later
  still receives them. Without replay, a client that opens the page after
  the conversation started would see nothing until the next message.
- `.latest()` replays only the most recent value. The WebSocket variant
  uses `.limit(100)` instead, replaying the last hundred values to a client
  that reconnects; SSE keeps only the latest because its stream is short
  lived and reconnecting frequently.

```kotlin
sink.emitNext(msg, Sinks.EmitFailureHandler.FAIL_FAST)
```

The mutation emits the new message into the sink. Every subscriber — every
open browser tab — receives it. `FAIL_FAST` means the emission fails
immediately if the sink is closed or terminated, rather than buffering a
message that will never be delivered.

The subscription fetcher simply exposes the sink as a `Flux`:

```kotlin
@DgsSubscription
fun messageSent(): Publisher<TextMessage> = sink.asFlux()
```

!!! note
    The `STORE` companion object is a plain `mutableMapOf`, which is not
    thread-safe. That is fine for a local sample, where the traffic is a
    handful of browser tabs. In a real server, use a
    `ConcurrentHashMap` or a database.

### The LocalDateTime scalar

The schema declares a custom scalar, so the server needs a matching
implementation. Create a `LocalDateTimeScalar` class in package
`com.example.demo`:

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

`@DgsScalar` registers the implementation with the schema. The three
`Coercing` methods are the three places a scalar is touched: turning a
server value into a response value, turning a client variable into a server
value, and turning a literal written in the document into a server value.
The example serializes to ISO-8601, so the Angular client receives a string
such as `"2026-11-15T09:30:00.123"`.

### Allow the browser to call the server

The Angular development server runs on port 4200 and the GraphQL server on
port 8080. That makes every request **cross-origin**, and the browser
blocks the response unless the server opts in. Add a `WebMvcConfigurer` to
the application:

```kotlin
@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
    }
}
```

!!! note
    This mapping allows any origin, which is convenient locally and wrong in
    production. Restrict it to the origins you actually serve, for example
    `registry.addMapping("/graphql").allowedOrigins("http://localhost:4200")`.

### Configure the application

Create `src/main/resources/application.properties`:

```properties
logging.level.com.example=DEBUG
logging.level.web=DEBUG
logging.level.com.netflix.graphql.dgs=TRACE

spring.mvc.async.request-timeout=45000
```

The last property is the one that matters for SSE. An SSE response is an
asynchronous servlet request that stays open for the whole life of the
subscription, and `spring.mvc.async.request-timeout` bounds it: after 45
seconds the server ends the stream.

The `graphql-sse` client treats that as a retryable disconnect and reconnects
on its own — five attempts with a randomised exponential backoff by default,
tunable through the `retryAttempts` and `retry` options. A client that
reconnects picks up the most recent message from the sink's replay buffer.
The WebSocket variant later widens that buffer to the last hundred messages.

## Step 5: Build the Angular client

The client is a single-page chat: a list of messages, a text field, and a
send button. It uses **Apollo Client** through **apollo-angular**, with a
custom transport link for the subscription.

### Create the project

Generate the Angular application with the CLI:

```bash
ng new dgs-subscription-ui-sse --style=scss --routing=false --skip-git
```

Add Angular Material for the form controls, then install the GraphQL
dependencies:

```bash
npm install @apollo/client apollo-angular graphql graphql-sse
```

| Package | Role |
| ------- | ---- |
| `@apollo/client` | The cache and the link infrastructure |
| `apollo-angular` | The Angular integration and the `gql` tag |
| `graphql` | The document AST that the links print |
| `graphql-sse` | The GraphQL over SSE client |

### The endpoint configuration

Create `src/app/config.ts`:

```typescript
export const GRAPHQL_HTTP_URL = 'http://localhost:8080/graphql';
```

One URL serves everything. The SSE transport is distinguished by the
`Accept: text/event-stream` header, not by a different address.

### The GraphQL documents

Create `src/app/graphql/messages.ts`:

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

The three documents map onto the three operations. `mergeNewMessage` is the
function that stitches the subscription into the query result, and
`describeError` turns the many shapes Apollo errors take into one line of
text for the banner.

### The SSE transport link

Apollo ships a WebSocket subscription link, but there is no first-party
Apollo link for SSE. Wrap the `graphql-sse` client in an `ApolloLink`
instead. Create `src/app/transport/sse-link.ts`:

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

Three things happen here:

- The link translates an Apollo operation into the request object the
  `graphql-sse` client expects, printing the document AST back into a
  query string.
- The `next`, `error`, and `complete` callbacks of the SSE sink are
  forwarded to the RxJS observer, which is what makes the link a normal
  Apollo link.
- The returned dispose function is the observable's teardown, so
  unsubscribing closes the SSE stream.

The `connectionState$` subject answers user story 4. The `graphql-sse`
client only reports `connecting` and `connected`, so the link detects
`closed` and `error` itself from the sink.

### Wire up Apollo

Create `src/app/app.config.ts`:

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

The **split link** is the heart of the setup. It inspects each operation and
routes subscriptions to the SSE link and everything else to the HTTP link.
One Apollo client, two transports, and the routing decision made per
operation rather than per endpoint.

### The chat component

Create `src/app/app.component.ts`:

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
import { connectionState$, type ConnectionState } from './transport/sse-link';

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

The component has three moving parts:

- The **query** is watched with `watchQuery`, so Apollo keeps it subscribed
  and re-reads it whenever the cache changes.
- The **subscription** is attached to that query with `subscribeToMore`.
  Every pushed message is merged into the query result by `mergeNewMessage`,
  and the cache broadcast is what re-renders the list. The component never
  renders a separate stream.
- The **mutation** sends the message. Notice that it does not add the
  message to the list itself — the subscription does that. The mutation
  response is used only to clear the form and to report errors.

The `subscribeToMore` subscription is stopped in `ngOnDestroy`, which
closes the SSE stream when the component is destroyed. Without that, the
stream would leak for the lifetime of the page.

Create `src/app/app.component.html`:

```html
<main class="container">
  <h1>DGS Subscription — SSE</h1>

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

The banner is user story 4 in the UI: whenever the transport is not open,
the page says so.

## Step 6: Run the server and the client

Now run both halves and use the chat.

### Start the server

```bash
cd dgs-subscription-sse
./mvnw spring-boot:run
```

On Windows, run `mvnw.cmd spring-boot:run` from the `dgs-subscription-sse`
directory. The chat server needs no database, so there is no Docker step
here.

When the `Started DemoApplication` log line appears, the server is ready
at `http://localhost:8080`.

### Start the client

```bash
cd dgs-subscription-ui-sse
npm install
npm start
```

Open `http://localhost:4200` in your browser. The page shows an empty
message list and a text field.

### Try it

1. Type a message and press the send button. It appears in the list, and
   the text field is cleared.
2. Open a second browser tab on `http://localhost:4200`. Send a message in
   one tab and watch it appear in the other. That is the subscription doing
   its job: the server pushed the message to every connected subscriber,
   and neither tab asked for it.
3. Stop the server. The connection banner appears in both tabs once the
   client has exhausted its reconnect attempts. Restart the server and
   reload the page to pick the stream back up.

The same operations are available from the command line. List the stored
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

Watch the subscription stream. The `-N` flag disables curl's output
buffering, and the `Accept: text/event-stream` header is what selects the
SSE transport on the server:

```bash
curl -N http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"query": "subscription { messageSent { id body sentAt } }"}'
```

The response stays open and prints one event per message. The framing
follows the
[GraphQL over SSE protocol](https://github.com/enisdenjo/graphql-sse/blob/master/PROTOCOL.md):
a `next` event whose `data:` line carries the execution result, followed by
a `complete` event when the stream ends.

```text
event:next
data:{"data":{"messageSent":{"id":"3a2f...","body":"Hello from curl","sentAt":"2026-11-15T09:30:00.123"}}}
```

Run the send mutation from another terminal while the subscription is open,
and the event appears immediately. Stop the subscription with `Ctrl+C`.

GraphiQL is available at `http://localhost:8080/graphiql`. Use it to run
the query and the mutation with completion and schema documentation; the
subscription is easiest to watch in the Angular client or with the `curl -N`
command above.

## Step 7: Switch to WebSocket subscriptions

SSE works, and it is the simpler transport: no upgrade handshake, no extra
dependency, ordinary HTTP all the way. But SSE is one-directional. If you
later want the client to push over the same connection — presence, typing
indicators, client-side cancellation — or if your infrastructure terminates
WebSocket connections, you need the WebSocket transport.

The good news is that the server-side code barely changes. The subscription
fetcher, the sink, and the schema are identical.

### The server

Add the WebSocket starter to the POM:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

The WebSocket endpoint is disabled by default in Spring GraphQL, so you also
have to give it a path in `application.properties`:

```properties
spring.graphql.websocket.path=/graphql
spring.graphql.websocket.connection-init-timeout=30s
spring.graphql.websocket.keep-alive=60s
```

- `spring.graphql.websocket.path` enables the endpoint. It is the same
  `/graphql` path as the HTTP endpoint — the upgrade request header tells
  the server which protocol to speak.
- `connection-init-timeout` bounds how long the server waits for the
  client's `connection_init` message.
- `keep-alive` makes the server send a `ping` frame when the connection
  has been idle, so that proxies do not drop a quiet chat.

Change the sink so that a reconnecting client catches up on more than the
last message:

```kotlin
private val sink: Many<TextMessage> = Sinks.many().replay().limit(100)
```

Now a client that reconnects receives the last hundred messages instead of
only the most recent one. That is the right trade-off for a long-lived
socket: the client catches up in one burst, and it does not miss the
messages sent while it was away.

### The client

Swap the transport package:

```bash
npm uninstall graphql-sse
npm install graphql-ws
```

Update `src/app/config.ts`:

```typescript
export const GRAPHQL_HTTP_URL = 'http://localhost:8080/graphql';
export const GRAPHQL_WS_URL = 'ws://localhost:8080/graphql';
```

Replace `src/app/transport/sse-link.ts` with `src/app/transport/ws-link.ts`:

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

This is much shorter than the SSE link, because Apollo ships the WebSocket
link. The `graphql-ws` client reports all four transitions — connecting,
connected, closed, and error — so the link only forwards them.

Update `src/app/app.config.ts` to use it:

```typescript
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

The component does not change at all. The split link still routes
subscriptions to the transport link, and the split link is now a WebSocket
link. This is the payoff of putting the transport behind a link: the UI is
independent of the protocol.

### What changed, and what did not

| | SSE | WebSocket |
| - | --- | --- |
| Protocol | GraphQL over SSE | `graphql-transport-ws` |
| Direction | server to client only | both directions |
| Subscription URL | `http://localhost:8080/graphql` | `ws://localhost:8080/graphql` |
| Client library | `graphql-sse` | `graphql-ws` |
| Server dependency | `spring-boot-starter-webmvc` | plus `spring-boot-starter-websocket` |
| Server configuration | `spring.mvc.async.request-timeout` | `spring.graphql.websocket.*` |
| Replay on reconnect | latest message | last 100 messages |
| Client link code | hand-written `ApolloLink` | Apollo's `GraphQLWsLink` |

!!! warning
    The two WebSocket protocols are not interchangeable. The
    `graphql-transport-ws` subprotocol used here is maintained, while the
    older `subscriptions-transport-ws` protocol is not. A client speaking
    one cannot talk to a server speaking the other, so migrate the client
    and the server together.

### Try it again

Stop the SSE server, start the WebSocket server, and restart the client:

```bash
cd dgs-subscription-ws
./mvnw spring-boot:run
```

```bash
cd dgs-subscription-ui
npm install
npm start
```

The chat behaves the same. The difference shows up in the network tab: the
browser now opens a WebSocket to `ws://localhost:8080/graphql` instead of
holding an event-stream response open.

## Step 8: Switch the Angular client to signals

The components version works, but it threads state through RxJS observables
and `async` pipes. The signals version expresses the same UI with signals
and the `rxResource` interop API.

### The transport publishes a signal

The only change in the transport is the state holder. Create
`src/app/transport/ws-link.ts` in the signals application:

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

A `WritableSignal` replaces the `BehaviorSubject`. The SSE variant of the
signals application makes the same change in its `sse-link.ts`.

### The component

Create `src/app/app.component.ts` in the signals application:

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

The template follows the same shape, with the call syntax instead of the
pipe syntax:

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

Compare the two versions:

| Concern | Components | Signals |
| ------- | ---------- | ------- |
| Query result | `watchQuery` + `valueChanges` | `rxResource` with `stream` |
| Subscription | `subscribeToMore` merged into the cache | `rxResource` with `stream` |
| Derived list | `map` in an RxJS pipeline | `computed` |
| Connection state | `BehaviorSubject` + `async` pipe | `WritableSignal` |
| Transient UI state | `BehaviorSubject` | `signal` |
| Reading in the template | `messages$ | async` | `messages()` |

!!! note
    There is one behavioral difference worth knowing. The components version
    merges every pushed message into the query result, so the list grows.
    The signals version as written above appends the **latest** pushed
    message to the initial list, because a resource holds only the most
    recent value of its stream. To accumulate every message, keep the
    pushed messages in a signal array and append to it in the stream
    pipeline:

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

The Apollo cache is still the source of truth for the query; the signal
array only carries what arrived over the subscription since the page loaded.

## Step 9: Test the subscriptions

A subscription is hard to test by clicking around, so the samples test it
on the server.

### Test the subscription stream

The `DgsQueryExecutor` runs a subscription and hands back the publisher,
which Reactor's `StepVerifier` can assert against. Create a
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
            "mutation sendMessage($msg: TextMessageInput!) { send(message:$msg) { body}}",
            "data.send.body",
            mapOf("msg" to (mapOf("body" to "text1 message")))
        )
        assertThat(sendText1).contains("text1")

        val sendText2 = dgsQueryExecutor.executeAndExtractJsonPath<String>(
            "mutation sendMessage($msg: TextMessageInput!) { send(message:$msg) { body}}",
            "data.send.body",
            mapOf("msg" to (mapOf("body" to "text2 message")))
        )
        assertThat(sendText2).contains("text2")

        // verify it now.
        verifier.verify()
    }
}
```

The pattern matters: start the verifier with `verifyLater()`, send the
messages, and only then call `verify()`. The subscription has to be
registered before the mutations run, otherwise the first message is emitted
before anybody is listening.

### Test over the transport

The `DgsQueryExecutor` test never touches the network, so it says nothing
about the transport. The integration tests do. For SSE, DGS ships a client
for the GraphQL over SSE protocol:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {

    @LocalServerPort
    var port = 0

    lateinit var sseClient: DgsGraphqlSSESubscriptionGraphQLClient
    lateinit var webClient: DgsWebClientGraphQLClient

    @BeforeEach
    fun setup() {
        val baseUrl = "http://localhost:$port/graphql"
        sseClient = DgsGraphqlSSESubscriptionGraphQLClient(
            baseUrl,
            WebClient.create(baseUrl)
        )
        webClient = DgsWebClientGraphQLClient(WebClient.create(baseUrl))
    }

    @Test
    fun testMessages() {
        val result = sseClient.reactiveExecuteQuery(
            "subscription { messageSent { body } }",
            emptyMap<String, Any>()
        )

        val verifier = StepVerifier.create(result)
            .consumeNextWith { assertThat(it).isEqualTo("text1 message") }
            .consumeNextWith { assertThat(it).isEqualTo("text2 message") }
            .thenCancel()
            .verifyLater()

        // send the messages, then
        verifier.verify()
    }
}
```

For WebSocket, the same test uses Spring GraphQL's own
`WebSocketGraphQlClient`:

```kotlin
val subscriptionUrl = "ws://localhost:$port/graphql"
this.socketClient = WebSocketGraphQlClient.create(
    URI.create(subscriptionUrl),
    ReactorNettyWebSocketClient()
)
this.socketClient.start().subscribe()

val executionResult = socketClient.document("subscription { messageSent { body } }")
    .executeSubscription()
    .map { (it.getData<Map<String, Any>>()!!["messageSent"]!! as Map<String, Any>)["body"] as String }

val verifier = StepVerifier.create(executionResult)
    .thenAwait(Duration.ofMillis(1500))
    .consumeNextWith { assertThat(it).isEqualTo("text1") }
    .consumeNextWith { assertThat(it).isEqualTo("text2") }
    .thenCancel()
    .verifyLater()

// give the websocket subscription time to register on the server before sending,
// otherwise the first message may be emitted before the subscription is active
Thread.sleep(1000)

// send the messages, then
verifier.verify()
```

Both transports have the same race: the subscription must be active on the
server before the mutation runs. The WebSocket test waits for it explicitly,
because the socket handshake adds a round trip that the in-process test does
not have.

### Test the client

The Angular applications have unit tests for the transport link and the
merge logic. Run them with:

```bash
cd dgs-subscription-ui-sse
npm test -- --watch=false
```

The transport tests drive the SSE sink directly with a fake client, so they
assert the state transitions, the forwarding of results, and the disposal of
the stream on unsubscribe — all without a server. The `messages.spec.ts`
test covers `mergeNewMessage` and `describeError`.

## Conclusion

You now have a working chat application built on Netflix DGS and Angular,
and you have seen the whole path from user story to running system:

- The use cases mapped onto three GraphQL operations — a query, a mutation,
  and a subscription — and the connection state turned out not to belong in
  the schema at all.
- The DGS side is a `@DgsComponent` class with a `Sinks.Many` broadcast
  channel, a custom scalar, and CORS for the browser.
- The client is an Apollo cache with a split link that routes subscriptions
  to the transport and everything else to HTTP.
- SSE and WebSocket are interchangeable behind that link, and the server
  changes by two properties and one dependency.
- Signals replace the observables in the UI without changing what the
  transport does.

Four modules in the
[spring-graphql-sample repository](https://github.com/hantsy/spring-graphql-sample)
implement exactly this:
[`dgs-subscription-sse`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-sse),
[`dgs-subscription-ws`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ws),
[`dgs-subscription-ui-sse`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui-sse),
[`dgs-subscription-ui`](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-subscription-ui),
and their `-signals` counterparts.

From here:

- Read [Building GraphQL APIs with Netflix DGS](./start.md) for the rest of
  the DGS programming model, including DataLoaders, code generation, and
  error handling.
- Read [An introduction to GraphQL and Spring](../intro.md) for the protocol
  details behind the two WebSocket protocols and the SSE specification.

Related examples: [dgs-webflux](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-webflux)
runs the same subscription on the reactive stack, and
[spring-graphql-webmvc-http](https://github.com/hantsy/spring-graphql-sample/tree/master/spring-graphql-webmvc-http)
serves the same schema with Spring GraphQL instead of DGS.
