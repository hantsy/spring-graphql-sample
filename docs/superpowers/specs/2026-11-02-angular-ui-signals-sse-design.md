# Angular UI modernization and SSE client — design

**Date:** 2026-11-02
**Status:** Approved design, pending implementation plan
**Scope:** `dgs-subscription-ui` (modernize), `dgs-subscription-ui-sse` (new), `dgs-subscription-sse` (CORS, already applied)

## Goal

Bring `dgs-subscription-ui` to current Angular on a signals-first, standalone, zoneless
architecture, and add a sibling `dgs-subscription-ui-sse` that consumes the same chat
schema over Server-Sent Events instead of WebSockets. The two apps should be readable
side by side and differ only in their transport link.

## Background

### Current state of `dgs-subscription-ui`

Angular 19.1.6, NgModule-based (`standalone: false`), bootstrapped with
`platformBrowserDynamic().bootstrapModule(AppModule)`. Component state is plain class
fields mutated inside RxJS `.subscribe()` callbacks — no signals. Templates use `*ngFor`
and `*ngIf` with a template-driven `ngModel` form.

Pre-v15 scaffolding is still present and now dead weight: `src/polyfills.ts`,
`src/test.ts`, `src/environments/`, `karma.conf.js`.

Data layer: `apollo-angular@10` with `@apollo/client@3.13.1`, `graphql@16`, and
`rxjs@~6.6.0`. Subscriptions go through `WebSocketLink` from `@apollo/client/link/ws`,
which is backed by `subscriptions-transport-ws@^0.11.0` — a protocol that is deprecated
upstream and already disabled in this repo's `graphql-kotlin` backend.

The single existing test, `app.component.spec.ts`, is stale `ng new` scaffolding that no
longer matches the component: it asserts `app.title` equals `'dgs-subscription-ui'` and
queries `.content span` for "dgs-subscription-ui app is running!", while the real
component sets `title = 'client'` and renders a chat form. It would fail if executed. It
never is executed — CI runs only `npm install && npm run build`.

### Backends

Both backends expose an identical schema — `messages` query, `send` mutation,
`messageSent` subscription over a `TextMessage { id, body, sentAt }` type — and both
serve it from `/graphql`. They differ only in subscription transport:

- `dgs-subscription-ws`: WebSocket, `spring.graphql.websocket.path=/graphql`,
  `connection-init-timeout=30s`, `keep-alive=60s`. Speaks `graphql-ws`.
- `dgs-subscription-sse`: HTTP/SSE at the same path, verified by its own integration test
  which uses DGS's `GraphqlSSESubscriptionGraphQLClient`. Speaks `graphql-transport-sse`.
  Sets `spring.mvc.async.request-timeout=45000` and configures no keep-alive, so idle
  streams are dropped by the server at 45s and must be re-established by the client.

## Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Modernization depth | Full: standalone, signals, zoneless, `@if`/`@for`, Vitest, `graphql-ws` | Lands on the architecture Angular currently recommends; signals and zoneless are the natural pairing |
| App relationship | Fully standalone, no shared library | Matches this repo's convention that every example is self-contained; keeps each sample copy-pasteable |
| SSE data layer | Custom `ApolloLink` over `graphql-sse`, combined with `split()` | Both apps keep one Apollo cache and one component; only the link differs |
| UI kit | Keep Angular Material in both apps | Preserves the existing look; siblings stay visually comparable |
| Testing | Real Vitest tests, run in CI | The current spec is broken and CI never runs tests |
| Execution approach | Regenerate with `ng new`, then port | Avoids three chained `ng update` majors (19→20→21→22); the app is ~150 lines, so porting is cheaper and more reliable than migrating |

## Verified version matrix

All versions confirmed against the npm registry, not assumed.

| Package | Version | Notes |
| --- | --- | --- |
| `@angular/*` | 22.2.4 | latest |
| `@angular/cli` | 22.2.5 | |
| `@angular/material`, `@angular/cdk` | 22.2.5 | Material peer-pins `@angular/cdk` to exactly `22.2.5` |
| `apollo-angular` | 14.2.0 | peers: `@angular/core ^20\|\|^21\|\|^22`, `@apollo/client ^4.2.3`, `graphql ^16\|\|^17`, `rxjs ^7.8.0` |
| `@apollo/client` | 4.3.5 | major bump from 3.13; exports `./link/subscriptions` (`GraphQLWsLink`) and deprecated `./link/ws`; **no SSE link** |
| `graphql` | 17.0.2 | |
| `rxjs` | ~7.8 | forced: `apollo-angular` 14 floor is `^7.8.0`, current app pins `~6.6.0` |
| `graphql-ws` | 6.4.1 | replaces `subscriptions-transport-ws` |
| `graphql-sse` | 2.6.3 | peer `graphql >=0.11 <=17`; exports no Apollo link, so the link is hand-written |
| Node | `^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0` | Angular 22 engine requirement; Node 20 is unsupported |

`zone.js` is dropped entirely — zoneless change detection does not require it.

Two library API facts were verified from package typings because the design depends on
them:

- `apollo-angular@14` exports only `provideApollo`, `provideNamedApollo`, `Apollo`,
  `ApolloBase`, `QueryRef`, `APOLLO`, `gql`, `onlyComplete*` plus the `./headers`,
  `./http`, `./persisted-queries` and `./testing` entry points. **There are no
  signal-based helpers**, so the RxJS→signal bridge is explicit via `toSignal`.
- `apollo-angular@14`'s `QueryRef` **does** expose `subscribeToMore`, alongside
  `valueChanges`, `refetch`, `updateQuery`, `fetchMore`, `startPolling`, `stopPolling`,
  `setVariables`, `reobserve` and `getCurrentResult`. The existing cache-merge logic
  therefore ports unchanged.
- `@apollo/client@4.3.5`'s `ObservableQuery.Result<TData>` carries `error?: ErrorLike`,
  `loading: boolean` and `data`, so the state envelope below reads real fields rather than
  assumed ones.
- `ng new` in Angular CLI 22.2.5 defaults to `--standalone` and `--test-runner vitest`,
  and accepts `--zoneless`. Its `--file-name-style-guide` defaults to `"2025"`, which names
  the root component `app.ts`; this design passes `"2016"` to keep the `app.component.ts`
  naming the rest of the repo uses.

## Architecture and project layout

Both apps are standalone Angular 22 projects with identical structure. Each is scaffolded
by `ng new` and then has the chat UI ported into it.

```
dgs-subscription-ui/              dgs-subscription-ui-sse/
  package.json                      package.json
  package-lock.json                 package-lock.json
  angular.json                      angular.json
  tsconfig.json                     tsconfig.json
  tsconfig.app.json                 tsconfig.app.json
  tsconfig.spec.json                tsconfig.spec.json
  .editorconfig                     .editorconfig
  .gitignore                        .gitignore
  src/
    main.ts            bootstrapApplication(AppComponent, { providers: [appProviders()] })
    index.html
    styles.css         Material prebuilt theme + chat layout
    app/
      app.config.ts    provideZonelessChangeDetection, provideHttpClient,
                       provideAnimationsAsync, provideApollo(...)
      app.component.ts          standalone, signal-based
      app.component.html        @if / @for
      app.component.css
      app.component.spec.ts
      graphql/
        messages.ts    gql documents + TextMessage model + mergeNewMessage
      transport/
        ws-link.ts              (WS app only)
        sse-link.ts             (SSE app only)
        sse-link.spec.ts        (SSE app only)
```

Deleted from the current app: `app.module.ts`, `graphql.module.ts`, `karma.conf.js`,
`src/polyfills.ts`, `src/test.ts`, `src/environments/`, and the broken
`app.component.spec.ts`. `graphql.module.ts`'s provider array becomes the `appProviders()`
function in `app.config.ts`. `environments/` is replaced by a single `GRAPHQL_URL`
constant, since both samples always talk to `localhost:8080`.

## Data flow and signals

The Apollo cache is the single source of truth. Subscription events fold into it via
`queryRef.subscribeToMore({ document, updateQuery: mergeNewMessage })`; the component
never keeps its own array.

RxJS is bridged to signals with `toSignal()` from `@angular/core/rxjs-interop`, mapping
`valueChanges` into a small state envelope so that errors become data rather than thrown
exceptions:

```ts
private readonly queryRef = this.apollo.watchQuery<MessagesData>({ query: MESSAGES_QUERY });
private readonly moreSub = this.queryRef.subscribeToMore({
  document: MESSAGE_SENT_SUBSCRIPTION,
  updateQuery: mergeNewMessage,
});

private readonly state = toSignal(
  this.queryRef.valueChanges.pipe(
    map(r => ({
      loading: r.loading,
      messages: r.data?.messages ?? [],
      error: r.error ? describeError(r.error) : null,
    })),
    catchError(err => of({ loading: false, messages: [], error: describeError(err) })),
  ),
  { initialValue: { loading: true, messages: [] as TextMessage[], error: null as string | null } },
);

readonly messages = computed(() => this.state().messages);
readonly loading  = computed(() => this.state().loading);
readonly error    = computed(() => this.state().error);
```

Input and mutation state are plain signals — `body`, `sending`, `sendError` — with
`apollo.mutate()` driving `send()`. The form becomes a standalone reactive form rather
than `ngModel`, and the template uses `@if`/`@for`.

Because merging happens in the cache, `app.component.ts` is identical between the two
apps. Only `app.config.ts` differs.

## Transport layer

Both apps use the same shape inside `provideApollo`:

```ts
provideApollo(() => ({
  link: split(isSubscription, transportLink, httpLink),
  cache: new InMemoryCache(),
}))
```

where `isSubscription` tests the operation's main definition, and `httpLink` comes from
`apollo-angular/http`.

- **WS app** — `GraphQLWsLink` from `@apollo/client/link/subscriptions` over a
  `graphql-ws` `createClient({ url: 'ws://localhost:8080/graphql' })`. This replaces the
  deprecated `WebSocketLink`/`subscriptions-transport-ws` pairing.
- **SSE app** — `GraphQLSSELink extends ApolloLink`. Its `request(operation)` wraps
  `graphql-sse`'s `client.subscribe(request, sink)` in an RxJS `Observable`, forwarding
  `next` to `observer.next`, `error` to `observer.error`, `complete` to
  `observer.complete`, and returning the dispose function that `subscribe()` yields as the
  Observable's teardown.

`GraphQLSSELink` takes an already-constructed `graphql-sse` `Client` through its
constructor rather than creating one internally. This is deliberate: it is what makes the
link unit-testable without a network. It also receives the transport module's
`connectionState` signal, which it writes from the sink's `error` and `complete` paths,
because `graphql-sse` exposes neither at client level.

In both apps the `transport/` module owns `connectionState` and constructs the link; the
component only reads the signal.

## Error handling

- **Query and mutation errors** live in the state envelope. `valueChanges` results carry
  `r.error` for network and GraphQL errors; `catchError` covers stream-level failures.
  Mutation failure sets `sendError` and deliberately leaves `body` intact so the user can
  retry.
- **Silent subscription death is the principal risk.** A `subscribeToMore` internal
  subscription that errors does not surface through `valueChanges`, so a dropped socket
  presents as a chat that simply stops updating. Each app's transport module therefore
  publishes a `connectionState` signal that the component renders as a banner. The two
  libraries differ in what they expose; both were read from their shipped typings:
  - `graphql-ws@6.4.1` — `ClientOptions.on` is a mapped type over
    `'connecting' | 'opened' | 'connected' | 'ping' | 'pong' | 'message' | 'closed' | 'error'`,
    so every transition the banner needs is available at client level.
  - `graphql-sse@2.6.3` — `EventListeners` exposes **only** `connecting`, `message` and
    `connected`. There is no `closed` or `error` callback; stream failure and termination
    arrive through each `subscribe()` call's `Sink`, whose shape is
    `next(value)`, `error(error: unknown)`, `complete()`. The SSE app's signal is therefore
    fed from two places: the client-level `on` for `connecting`/`connected`, and the sink
    wrapper inside `GraphQLSSELink.request()` for error and completion. This is a further
    reason the transport module, not the component, must construct the link.
- The SSE backend's 45s async timeout with no keep-alive means idle streams will be
  dropped and reconnect periodically. The banner makes that visible rather than mysterious.
- A small `describeError(err: unknown): string` helper normalizes `ApolloError`, arrays of
  GraphQL errors, and arbitrary throwables. Duplicated per app, consistent with the
  standalone convention.

## CORS — already applied

`dgs-subscription-sse` had no CORS configuration, so a browser app on `:4200` could not
reach `:8080`; the `application/json` POST also triggers a preflight. `dgs-subscription-ws`
already solves this with a permissive `WebConfig : WebMvcConfigurer` calling
`registry.addMapping("/**")`.

The identical bean has been added to
`dgs-subscription-sse/src/main/kotlin/com/example/demo/DemoApplication.kt`. Verified:
`./gradlew build` succeeds, both test classes pass with 0 failures and 0 errors, and
`WebConfig.class` is present in the compiled output.

This is the only backend change in the design. Incidental observation: the module's build
now spends ~30s in graceful shutdown waiting on a still-active SSE request. That is
inherent to long-lived SSE connections and is not caused by the CORS bean.

## Testing

Vitest via Angular's `@angular/build:unit-test` target with a jsdom environment, which
needs no Chrome binary in CI — unlike Karma.

1. **`app.component.spec.ts`** (both apps). `apollo-angular/testing` exports
   `ApolloTestingModule`, `ApolloTestingController`, `TestOperation`,
   `APOLLO_TESTING_CACHE` and `APOLLO_TESTING_NAMED_CACHE`. There is **no**
   `provideApolloTesting()` function, so the testing backend is wired by importing
   `ApolloTestingModule` into `TestBed.configureTestingModule` and injecting
   `ApolloTestingController`. This is the only NgModule the design retains, and it is the
   library's rather than ours. Mocked results are delivered through
   `TestOperation.flushData()`, `.graphqlErrors()` and `.networkError()`, and every test
   ends with `controller.verify()`. Cases:
   - messages returned by the query render through `@for`
   - `send()` issues the mutation with the expected variables
   - a failed query surfaces the error banner
2. **`mergeNewMessage`** tested as a pure function — it appends the new message and
   tolerates a missing previous list. Driving this through `ApolloTestingController` is
   awkward and low-value; the merge function is the only real logic in that path.
3. **`sse-link.spec.ts`** (SSE app only) with an injected fake `graphql-sse` `Client`:
   - `request()` forwards sink `next` values as `FetchResult`s
   - sink `error` propagates to the observer
   - unsubscribing calls the dispose function

The WS app needs no counterpart to (3) because `GraphQLWsLink` is Apollo's own code.

## Repository wiring

- **New workflow** `.github/workflows/dgs-subscription-ui-sse.yml`, path-filtered to
  `dgs-subscription-ui-sse/**`, otherwise a structural copy of the existing UI workflow.
- **Both workflows** gain an `npm test` step and switch from `npm install` to `npm ci`.
- **Node** moves from a loose `"22"` to `"24"` in both workflows, for headroom against
  Angular's `^22.22.3` floor.
- **Lockfiles.** No `package-lock.json` is committed anywhere in the repo today. That makes
  `npm install` non-reproducible *and* silently breaks the existing cache, whose key
  `hashFiles('**/package-lock.json')` matches no file and therefore never restores. Both
  apps commit a lockfile, and each cache key is scoped to its own project, e.g.
  `hashFiles('dgs-subscription-ui/package-lock.json')`.
- **Dependabot.** Add a `/dgs-subscription-ui-sse` npm entry carrying the same `ignore`
  rules as the existing UI entry (no majors; no majors or minors for non-Angular packages),
  matching the config normalized earlier the same day.
- **README.** Add a `dgs-subscription-ui-sse` row to the example table, and note that both
  UIs default to `http://localhost:8080`, so `dgs-subscription-ws` and
  `dgs-subscription-sse` cannot be run simultaneously without changing a port — otherwise
  the two samples silently talk to the wrong backend.

## Out of scope

- No shared library or workspace tooling; the apps are intentionally duplicated.
- No routing, state-management library, or i18n.
- No change to either backend's GraphQL schema.
- No auth: neither backend requires credentials for this sample, unlike `dgs-kotlin-co`.
- The `legacy/**` projects are untouched, per standing instruction.

## Risks

| Risk | Mitigation |
| --- | --- |
| `@apollo/client` 3→4 is a breaking major | Verified `apollo-angular@14` peer-requires `^4.2.3`, so the pairing is supported; check `InMemoryCache`/`split`/`FetchResult` imports at implementation time |
| Zoneless surfaces unmarked async writes | All async results flow through `toSignal`/signals, which mark consumers dirty; the connection banner is also signal-driven |
| Hand-written SSE link mishandles teardown | Covered by `sse-link.spec.ts` asserting dispose on unsubscribe |
| SSE connection state has two sources (client `on` and the link's sink) | Both write one signal owned by the transport module; `sse-link.spec.ts` asserts the error and complete paths |
| Vitest + jsdom config differs from Karma | Use `ng new`'s generated test scaffolding rather than hand-writing the config |

## Verification plan

Each app must pass, in order: `npm ci`, `npm run build`, `npm test`. Then manual
verification against the real backend — start `dgs-subscription-ws` (or
`dgs-subscription-sse`), serve the matching UI, send a message from one browser tab and
confirm it appears in a second tab through the subscription. The `dgs-subscription-sse`
Gradle build must also remain green after the CORS addition.
