# Angular UI Modernization and SSE Client — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Modernize `dgs-subscription-ui` to Angular 22 with standalone components, signals and zoneless change detection, and add a sibling `dgs-subscription-ui-sse` that consumes the same chat schema over Server-Sent Events.

**Architecture:** Two fully standalone Angular apps with identical structure. The Apollo cache is the single source of truth: subscription events fold in via `QueryRef.subscribeToMore`, and RxJS is bridged to signals with `toSignal`, so `app.component.ts` is byte-identical between the apps. Only `app.config.ts` and the `transport/` module differ — the WS app uses Apollo's own `GraphQLWsLink`, the SSE app uses a hand-written `GraphQLSSELink` over `graphql-sse`.

**Tech Stack:** Angular 22.2.4, Angular Material 22.2.5, apollo-angular 14.2.0, Apollo Client 4.3.5, graphql 17.0.2, graphql-ws 6.4.1, graphql-sse 2.6.3, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-02-angular-ui-signals-sse-design.md`

## Global Constraints

Every task's requirements implicitly include this section. Exact values, verified against the npm registry and the packages' own typings.

- Angular packages: **22.2.4**. `@angular/cli`: **22.2.5**.
- `@angular/material` and `@angular/cdk`: **22.2.5**. Material peer-pins `@angular/cdk` to *exactly* `22.2.5` — do not use a range.
- `apollo-angular`: **14.2.0**. Peer floors: `@angular/core ^20||^21||^22`, `@apollo/client ^4.2.3`, `graphql ^16||^17`, `rxjs ^7.8.0`.
- `@apollo/client`: **4.3.5**. `graphql`: **17.0.2**. `rxjs`: **~7.8**.
- `graphql-ws`: **6.4.1** — WS app only. `graphql-sse`: **2.6.3** — SSE app only. Peer `graphql >=0.11 <=17`.
- Node engine required by Angular 22: `^22.22.3 || ^24.15.0 || >=26.0.0`. **Node 20 is unsupported.** CI uses Node `24`.
- **Zoneless**: no `zone.js` dependency and no `zone.js` in `angular.json` polyfills.
- **Standalone only**: no NgModules in application code. The single exception is `ApolloTestingModule` imported into `TestBed` in specs, because that is the only form `apollo-angular/testing` offers.
- File naming: scaffold with `--file-name-style-guide 2016` so the root component is `app.component.ts`, matching the rest of this repo. The CLI default is `2025`, which would produce `app.ts`.
- Both apps talk to `http://localhost:8080/graphql`. The WS app additionally uses `ws://localhost:8080/graphql`. The spec calls this "a single `GRAPHQL_URL` constant"; this plan realizes it as `src/app/config.ts` exporting `GRAPHQL_HTTP_URL` in both apps plus `GRAPHQL_WS_URL` in the WS app only, because the WebSocket needs a different scheme.
- `legacy/**` is never modified.
- No shared library or workspace tooling between the two apps; duplication is intentional.

### Verified API facts the code below relies on

- `apollo-angular` 14 exports `provideApollo`, `Apollo`, `QueryRef`, `gql`. `QueryRef` exposes `valueChanges`, `subscribeToMore`, `refetch`, `updateQuery`, `fetchMore`, `startPolling`, `stopPolling`, `setVariables`, `reobserve`, `getCurrentResult`. **There are no signal helpers**, hence the explicit `toSignal` bridge.
- `apollo-angular/testing` exports `ApolloTestingModule`, `ApolloTestingController`, `TestOperation`, `APOLLO_TESTING_CACHE`, `APOLLO_TESTING_NAMED_CACHE`. **There is no `provideApolloTesting()`.** `TestOperation` offers `flushData`, `graphqlErrors`, `networkError`, `flush`, `complete`. Its `flush` deliberately does *not* auto-complete SUBSCRIPTION operations.
- `@apollo/client` 4.3.5 root exports `ApolloLink`, `execute`, `split`, `from`, `concat`, `empty`, `InMemoryCache`, and the types `FetchResult`, `Operation`, `GraphQLRequest`, `RequestHandler`. `GraphQLWsLink` lives at `@apollo/client/link/subscriptions`. `getMainDefinition` lives at `@apollo/client/utilities`.
- `ObservableQuery.Result<TData>` has `error?: ErrorLike`, `loading: boolean`, `data`.
- `graphql-ws` `ClientOptions.on` accepts `'connecting' | 'opened' | 'connected' | 'ping' | 'pong' | 'message' | 'closed' | 'error'`.
- `graphql-sse` `EventListeners` accepts **only** `connecting`, `message`, `connected`. There is no `closed` or `error` listener. `Client.subscribe(request, sink, on?)` returns a dispose function. `Sink<T>` is `{ next(value: T): void; error(error: unknown): void; complete(): void }`. Both `Client` and `Sink` are importable from the `graphql-sse` package root.
- `ng new` in CLI 22.2.5 supports `--standalone` (default true), `--zoneless`, `--test-runner vitest` (default), `--style css`, `--file-name-style-guide 2016`, `--ssr=false`, `--skip-git`, `--defaults`, `--directory`.
- `ng add @angular/material` accepts `--theme=azure-blue`, which satisfies the otherwise-interactive theme prompt.

---

### Task 1: Commit the already-verified pending work

Three files are modified in the working tree from earlier, verified work. Commit them first so the scaffold task starts from a clean tree.

**Files:**
- Modify (already changed): `README.md`
- Modify (already changed): `.github/dependabot.yml`
- Modify (already changed): `dgs-subscription-sse/src/main/kotlin/com/example/demo/DemoApplication.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: a clean working tree; the `WebConfig` CORS bean in `dgs-subscription-sse` that Task 10's SSE app depends on at runtime.

- [ ] **Step 1: Confirm exactly these three files are modified**

Run: `git status --short`
Expected: exactly ` M .github/dependabot.yml`, ` M README.md`, ` M dgs-subscription-sse/src/main/kotlin/com/example/demo/DemoApplication.kt`. If anything else is modified, stop and reconcile before continuing.

- [ ] **Step 2: Commit the docs and config changes**

```bash
git add README.md .github/dependabot.yml
git commit -m "Update README for Java 25 and normalize dependabot across active projects"
```

- [ ] **Step 3: Commit the CORS change separately**

```bash
git add dgs-subscription-sse/src/main/kotlin/com/example/demo/DemoApplication.kt
git commit -m "$(cat <<'EOF'
Add permissive CORS config to dgs-subscription-sse

Mirrors the WebConfig already present in dgs-subscription-ws. Without it a
browser client on :4200 cannot reach the SSE backend on :8080, and the
application/json POST preflight is rejected.
EOF
)"
```

- [ ] **Step 4: Verify the SSE backend still builds**

Run: `cd dgs-subscription-sse && ./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL`. Note the build spends ~30s in graceful shutdown waiting on an active SSE request; that is pre-existing behaviour, not a failure.

---

### Task 2: Scaffold the modernized `dgs-subscription-ui`

Replace the Angular 19 NgModule app with a freshly scaffolded Angular 22 standalone, zoneless, Vitest app that already builds. No application logic is ported in this task.

**Files:**
- Create (via scaffold): everything under `dgs-subscription-ui/`
- Delete: the previous contents of `dgs-subscription-ui/` — recoverable from git history

**Interfaces:**
- Consumes: a clean working tree from Task 1.
- Produces: `dgs-subscription-ui/` containing `src/main.ts`, `src/app/app.config.ts`, `src/app/app.component.ts`, `src/app/app.component.html`, `src/app/app.component.css`, `src/app/app.component.spec.ts`, `angular.json`, `tsconfig.json`, `tsconfig.app.json`, `tsconfig.spec.json`, `package.json`, `package-lock.json`, `.gitignore`. Later tasks add `src/app/config.ts`, `src/app/graphql/messages.ts`, `src/app/transport/ws-link.ts`.

- [ ] **Step 1: Verify the old app is committed before deleting it**

Run: `git status --short dgs-subscription-ui`
Expected: no output. If the old app has uncommitted changes, stop — deleting it would lose work.

- [ ] **Step 2: Scaffold into a temporary directory**

```bash
cd /d/hantsylabs/spring-graphql-sample
npx --yes @angular/cli@22.2.5 new dgs-subscription-ui \
  --directory .ng-tmp/dgs-subscription-ui \
  --standalone \
  --zoneless \
  --test-runner vitest \
  --style css \
  --file-name-style-guide 2016 \
  --ssr=false \
  --skip-git \
  --defaults
```

Expected: completes without prompting. `--defaults` plus every explicit flag above means no interactive input is requested.

- [ ] **Step 3: Confirm the scaffold is zoneless and Vitest-based**

Run: `cd .ng-tmp/dgs-subscription-ui && grep -c "zone.js" package.json angular.json; grep -o "vitest" package.json | head -1`
Expected: `zone.js` count is `0` in both files, and `vitest` is printed once. If `zone.js` appears, the `--zoneless` flag did not take effect — stop and re-scaffold rather than hand-editing.

- [ ] **Step 4: Add Angular Material non-interactively**

```bash
cd /d/hantsylabs/spring-graphql-sample/.ng-tmp/dgs-subscription-ui
npx ng add @angular/material@22.2.5 --theme=azure-blue --skip-confirmation
```

Expected: adds `@angular/material` and `@angular/cdk` and edits `src/styles.css`. Passing `--theme` explicitly is what prevents the interactive palette prompt.

- [ ] **Step 5: Add the GraphQL dependencies**

```bash
cd /d/hantsylabs/spring-graphql-sample/.ng-tmp/dgs-subscription-ui
npm install apollo-angular@14.2.0 @apollo/client@4.3.5 graphql@17.0.2 graphql-ws@6.4.1
```

- [ ] **Step 6: Verify the scaffold builds and tests pass**

```bash
cd /d/hantsylabs/spring-graphql-sample/.ng-tmp/dgs-subscription-ui
npm run build
npm test -- --watch=false
```

Expected: build succeeds; the generated `app.component.spec.ts` passes under Vitest. `--watch=false` is required or Vitest stays attached to the terminal.

- [ ] **Step 7: Swap the scaffold into place**

```bash
cd /d/hantsylabs/spring-graphql-sample
git rm -r --quiet dgs-subscription-ui
rm -rf dgs-subscription-ui
mv .ng-tmp/dgs-subscription-ui dgs-subscription-ui
rmdir .ng-tmp
```

- [ ] **Step 8: Confirm the legacy scaffolding is gone**

Run: `ls dgs-subscription-ui/src dgs-subscription-ui/src/app; test -f dgs-subscription-ui/karma.conf.js && echo "KARMA STILL PRESENT" || echo "no karma"`
Expected: no `polyfills.ts`, no `test.ts`, no `environments/`, no `app.module.ts`, no `graphql.module.ts`; `no karma`.

- [ ] **Step 9: Commit**

```bash
git add dgs-subscription-ui
git commit -m "Scaffold dgs-subscription-ui on Angular 22 standalone, zoneless, Vitest"
```

---

### Task 3: GraphQL documents, model and `mergeNewMessage`

The only pure logic shared by both apps. Written test-first because `mergeNewMessage` is what folds subscription events into the cache, and a subtle bug there silently loses messages.

**Files:**
- Create: `dgs-subscription-ui/src/app/graphql/messages.ts`
- Test: `dgs-subscription-ui/src/app/graphql/messages.spec.ts`

**Interfaces:**
- Consumes: `gql` from `apollo-angular`.
- Produces, for Task 5 and Task 7:
  - `interface TextMessage { id: string; body: string; sentAt: string | null }`
  - `interface MessagesData { messages: TextMessage[] }`
  - `interface SendMessageData { send: TextMessage | null }`
  - `interface MessageSentData { messageSent: TextMessage }`
  - `const MESSAGES_QUERY`, `const SEND_MESSAGE_MUTATION`, `const MESSAGE_SENT_SUBSCRIPTION` — `DocumentNode`s with operation names `allMessages`, `sendMessage`, `onMessageSent`. These names are what the specs match on.
  - `function mergeNewMessage(prev: MessagesData, options: { subscriptionData: { data?: MessageSentData | null } }): MessagesData`
  - `function describeError(err: unknown): string`

- [ ] **Step 1: Write the failing test**

Create `dgs-subscription-ui/src/app/graphql/messages.spec.ts`:

```ts
import { describe, expect, it } from 'vitest';
import {
  mergeNewMessage,
  describeError,
  type MessageSentData,
  type MessagesData,
  type TextMessage,
} from './messages';

const msg = (id: string, body: string): TextMessage => ({ id, body, sentAt: null });
const sub = (messageSent: TextMessage | null) => ({
  subscriptionData: { data: messageSent ? ({ messageSent } as MessageSentData) : null },
});

describe('mergeNewMessage', () => {
  it('appends the incoming message to the existing list', () => {
    const prev: MessagesData = { messages: [msg('1', 'first')] };
    const result = mergeNewMessage(prev, sub(msg('2', 'second')));
    expect(result.messages).toEqual([msg('1', 'first'), msg('2', 'second')]);
  });

  it('returns the previous object unchanged when the subscription carries no data', () => {
    const prev: MessagesData = { messages: [msg('1', 'first')] };
    expect(mergeNewMessage(prev, sub(null))).toBe(prev);
  });

  it('does not mutate the previous result', () => {
    const prev: MessagesData = { messages: [msg('1', 'first')] };
    mergeNewMessage(prev, sub(msg('2', 'second')));
    expect(prev.messages).toHaveLength(1);
  });
});

describe('describeError', () => {
  it('extracts the message from an Error', () => {
    expect(describeError(new Error('boom'))).toBe('boom');
  });

  it('joins an array of errors', () => {
    expect(describeError([new Error('a'), new Error('b')])).toBe('a; b');
  });

  it('stringifies anything else', () => {
    expect(describeError('nope')).toBe('nope');
  });
});
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `cd dgs-subscription-ui && npm test -- --watch=false`
Expected: FAIL — cannot resolve `./messages`.

- [ ] **Step 3: Write the implementation**

Create `dgs-subscription-ui/src/app/graphql/messages.ts`:

```ts
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

- [ ] **Step 4: Run the test and verify it passes**

Run: `cd dgs-subscription-ui && npm test -- --watch=false`
Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add dgs-subscription-ui/src/app/graphql
git commit -m "Add GraphQL documents and cache-merge logic to dgs-subscription-ui"
```

---

### Task 4: WebSocket transport module

Owns the `connectionState` signal and builds the `GraphQLWsLink`. The listener object is exported separately from `createWsLink` specifically so it can be unit-tested without opening a socket.

**Files:**
- Create: `dgs-subscription-ui/src/app/config.ts`
- Create: `dgs-subscription-ui/src/app/transport/ws-link.ts`
- Test: `dgs-subscription-ui/src/app/transport/ws-link.spec.ts`

**Interfaces:**
- Consumes: `GraphQLWsLink` from `@apollo/client/link/subscriptions`; `createClient` from `graphql-ws`.
- Produces, for Task 5:
  - `const GRAPHQL_HTTP_URL: string` and `const GRAPHQL_WS_URL: string` from `config.ts`
  - `type ConnectionState = 'idle' | 'connecting' | 'open' | 'closed' | 'error'`
  - `const connectionState: WritableSignal<ConnectionState>`
  - `function createWsLink(url: string): GraphQLWsLink`

- [ ] **Step 1: Write the failing test**

Create `dgs-subscription-ui/src/app/transport/ws-link.spec.ts`:

```ts
import { beforeEach, describe, expect, it } from 'vitest';
import { connectionState, wsClientListeners } from './ws-link';

describe('wsClientListeners', () => {
  beforeEach(() => connectionState.set('idle'));

  it('reports connecting', () => {
    wsClientListeners.connecting!(false);
    expect(connectionState()).toBe('connecting');
  });

  it('reports open once connected', () => {
    wsClientListeners.connected!(false);
    expect(connectionState()).toBe('open');
  });

  it('reports closed', () => {
    wsClientListeners.closed!({} as never);
    expect(connectionState()).toBe('closed');
  });

  it('reports error', () => {
    wsClientListeners.error!(new Error('socket died'));
    expect(connectionState()).toBe('error');
  });
});
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `cd dgs-subscription-ui && npm test -- --watch=false`
Expected: FAIL — cannot resolve `./ws-link`.

- [ ] **Step 3: Write the implementation**

Create `dgs-subscription-ui/src/app/config.ts`:

```ts
export const GRAPHQL_HTTP_URL = 'http://localhost:8080/graphql';
export const GRAPHQL_WS_URL = 'ws://localhost:8080/graphql';
```

Create `dgs-subscription-ui/src/app/transport/ws-link.ts`:

```ts
import { signal, type WritableSignal } from '@angular/core';
import { GraphQLWsLink } from '@apollo/client/link/subscriptions';
import { createClient, type ClientOptions } from 'graphql-ws';

export type ConnectionState = 'idle' | 'connecting' | 'open' | 'closed' | 'error';

export const connectionState: WritableSignal<ConnectionState> = signal<ConnectionState>('idle');

// A dropped socket does not surface through QueryRef.valueChanges, so the transport
// publishes its own state for the component to render as a banner.
export const wsClientListeners: NonNullable<ClientOptions<false>['on']> = {
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

If `ClientOptions` turns out not to be generic in `graphql-ws@6.4.1`, use `NonNullable<ClientOptions['on']>` instead; the exported member names are what Task 5 depends on, not this type expression.

- [ ] **Step 4: Run the test and verify it passes**

Run: `cd dgs-subscription-ui && npm test -- --watch=false`
Expected: PASS, 10 tests total (6 from Task 3 plus 4 here).

- [ ] **Step 5: Commit**

```bash
git add dgs-subscription-ui/src/app/config.ts dgs-subscription-ui/src/app/transport
git commit -m "Add WebSocket transport and connection-state signal to dgs-subscription-ui"
```

---

### Task 5: Signal-based `AppComponent`

Wires Apollo into `app.config.ts`, replaces the generated component with the signals-based chat UI, and tests it through `ApolloTestingModule`.

**Files:**
- Modify: `dgs-subscription-ui/src/app/app.config.ts`
- Modify: `dgs-subscription-ui/src/main.ts`
- Modify: `dgs-subscription-ui/src/app/app.component.ts`
- Modify: `dgs-subscription-ui/src/app/app.component.html`
- Modify: `dgs-subscription-ui/src/app/app.component.css`
- Test: `dgs-subscription-ui/src/app/app.component.spec.ts` (replaces the generated one)

**Interfaces:**
- Consumes: everything Task 3 and Task 4 produce.
- Produces: a working app; `AppComponent` exposes `messages`, `loading`, `error`, `connection`, `sending`, `sendError` and `send()` — Task 7 copies this file verbatim into the SSE app.

- [ ] **Step 1: Write the failing test**

Replace `dgs-subscription-ui/src/app/app.component.spec.ts` with:

```ts
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations';
import { ApolloTestingController, ApolloTestingModule } from 'apollo-angular/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  let controller: ApolloTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppComponent, ApolloTestingModule],
      providers: [provideHttpClient(), provideAnimationsAsync()],
    });
    controller = TestBed.inject(ApolloTestingController);
  });

  afterEach(() => {
    // subscribeToMore opens a subscription that TestOperation.flush never
    // auto-completes, so drain it before asserting no operations are outstanding
    controller.match(() => true).forEach(op => op.complete());
    controller.verify();
  });

  it('renders messages returned by the query', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    controller.expectOne('allMessages').flushData({
      messages: [{ id: '1', body: 'hello world', sentAt: null }],
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('hello world');
  });

  it('sends the mutation with the typed body', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    controller.expectOne('allMessages').flushData({ messages: [] });

    const component = fixture.componentInstance;
    component.form.controls.body.setValue('hi there');
    component.send();

    const mutation = controller.expectOne('sendMessage');
    expect(mutation.operation.variables).toEqual({ message: { body: 'hi there' } });
    mutation.flushData({ send: null });
  });

  it('shows an error banner when the query fails', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    controller.expectOne('allMessages').networkError(new Error('boom'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('boom');
  });
});
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `cd dgs-subscription-ui && npm test -- --watch=false`
Expected: FAIL — the generated `AppComponent` has no `form` member and issues no `allMessages` operation.

- [ ] **Step 3: Write `app.config.ts`**

Replace `dgs-subscription-ui/src/app/app.config.ts` with:

```ts
import { provideHttpClient } from '@angular/common/http';
import { inject, provideZonelessChangeDetection, type Provider } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations';
import { InMemoryCache, split } from '@apollo/client';
import { getMainDefinition } from '@apollo/client/utilities';
import { provideApollo } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';
import { GRAPHQL_HTTP_URL, GRAPHQL_WS_URL } from './config';
import { createWsLink } from './transport/ws-link';

export function appProviders(): Provider[] {
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

Keep `provideZonelessChangeDetection()` exactly once — the scaffold already emits it, and this file replaces the scaffolded version wholesale.

- [ ] **Step 4: Write `main.ts`**

Replace `dgs-subscription-ui/src/main.ts` with:

```ts
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appProviders } from './app/app.config';

bootstrapApplication(AppComponent, { providers: appProviders() }).catch(err =>
  console.error(err),
);
```

- [ ] **Step 5: Write the component**

Replace `dgs-subscription-ui/src/app/app.component.ts` with:

```ts
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Apollo } from 'apollo-angular';
import { catchError, map, of } from 'rxjs';
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
import { connectionState } from './transport/ws-link';

interface UiState {
  loading: boolean;
  messages: TextMessage[];
  error: string | null;
}

const LOADING: UiState = { loading: true, messages: [], error: null };

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

  private readonly queryRef = this.apollo.watchQuery<MessagesData>({
    query: MESSAGES_QUERY,
  });

  // The subscription folds into the cache, so valueChanges is the only stream the
  // component reads. subscribeToMore must be created for the subscription to start.
  private readonly moreSubscription = this.queryRef.subscribeToMore<MessageSentData>({
    document: MESSAGE_SENT_SUBSCRIPTION,
    updateQuery: mergeNewMessage,
  });

  private readonly state = toSignal<UiState>(
    this.queryRef.valueChanges.pipe(
      map(r => ({
        loading: r.loading,
        messages: r.data?.messages ?? [],
        error: r.error ? describeError(r.error) : null,
      })),
      catchError(err => of({ loading: false, messages: [], error: describeError(err) })),
    ),
    { initialValue: LOADING },
  );

  readonly messages = computed(() => this.state().messages);
  readonly loading = computed(() => this.state().loading);
  readonly error = computed(() => this.state().error);
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
          if (result.errors?.length) {
            this.sendError.set(describeError(result.errors));
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

`moreSubscription` is intentionally unused after assignment. If the linter flags it, rename to `private readonly _moreSubscription` and add `// eslint-disable-next-line @typescript-eslint/no-unused-vars` — do not delete the call, or the subscription never starts.

- [ ] **Step 6: Write the template**

Replace `dgs-subscription-ui/src/app/app.component.html` with:

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

- [ ] **Step 7: Write the styles**

Replace `dgs-subscription-ui/src/app/app.component.css` with:

```css
.container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 40rem;
  margin: 2rem auto;
  padding: 0 1rem;
}

.messages {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-height: 8rem;
}

.messages li {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.5rem 0.75rem;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.04);
}

.messages .empty {
  justify-content: center;
  background: none;
  color: rgba(0, 0, 0, 0.5);
}

.sent-at {
  color: rgba(0, 0, 0, 0.5);
  font-size: 0.8rem;
}

.message-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.message-field {
  flex: 1;
}

.banner {
  margin: 0;
  padding: 0.5rem 0.75rem;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
}

.banner-error {
  background: rgba(244, 67, 54, 0.12);
  color: #b00020;
}
```

- [ ] **Step 8: Run the tests and verify they pass**

Run: `cd dgs-subscription-ui && npm test -- --watch=false`
Expected: PASS — all suites green, including the three `AppComponent` cases.

If `expectOne('allMessages')` reports that no operation was found, the `watchQuery` never fired, which means `provideApollo` is missing from `app.config.ts`.

- [ ] **Step 9: Verify the production build**

Run: `cd dgs-subscription-ui && npm run build`
Expected: `BUILD SUCCESSFUL`-style completion with no TypeScript errors.

- [ ] **Step 10: Commit**

```bash
git add dgs-subscription-ui/src
git commit -m "Port dgs-subscription-ui chat to standalone signals component over graphql-ws"
```

---

### Task 6: CI, lockfile and manual verification for `dgs-subscription-ui`

**Files:**
- Modify: `.github/workflows/dgs-subscription-ui.yml`
- Modify: `dgs-subscription-ui/package-lock.json` (already generated by Task 2, must be committed)

**Interfaces:**
- Consumes: a building, testing app from Task 5.
- Produces: a green CI workflow that Task 10 copies for the SSE app.

- [ ] **Step 1: Confirm the lockfile exists**

Run: `test -f dgs-subscription-ui/package-lock.json && echo present || echo MISSING`
Expected: `present`. If missing, run `cd dgs-subscription-ui && npm install` to regenerate it.

- [ ] **Step 2: Rewrite the workflow**

Replace `.github/workflows/dgs-subscription-ui.yml` with:

```yaml
name: dgs-subscription-ui

on:
  push:
    paths:
      - "dgs-subscription-ui/**"
    branches: [ master ]
  pull_request:
    paths:
      - "dgs-subscription-ui/**"
    types:
      - opened
      - synchronize
      - reopened

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4
      - name: Setup NodeJS
        uses: actions/setup-node@v4
        with:
          node-version: "24"

      - uses: actions/cache@v4
        with:
          path: ~/.npm
          key: ${{ runner.os }}-node-${{ hashFiles('dgs-subscription-ui/package-lock.json') }}
          restore-keys: |
            ${{ runner.os }}-node-

      - name: Install, build and test
        run: |
          cd ./dgs-subscription-ui
          npm ci
          npm run build
          npm test -- --watch=false
```

Node moves from `"22"` to `"24"` because Angular 22 requires `^22.22.3 || ^24.15.0 || >=26.0.0`, and the cache key moves off `**/package-lock.json` — that glob matched no file before this task, so the cache never restored.

- [ ] **Step 3: Verify the workflow YAML parses**

Run: `python -c "import yaml,sys; yaml.safe_load(open('.github/workflows/dgs-subscription-ui.yml')); print('valid yaml')"`
Expected: `valid yaml`.

- [ ] **Step 4: Verify a clean-install build from the lockfile**

```bash
cd dgs-subscription-ui
rm -rf node_modules
npm ci
npm run build
npm test -- --watch=false
```

Expected: `npm ci` succeeds from the lockfile alone (proving the lockfile is complete), then build and tests pass. This is exactly what CI will run.

- [ ] **Step 5: Manual smoke test against the real backend**

```bash
cd /d/hantsylabs/spring-graphql-sample/dgs-subscription-ws && ./gradlew bootRun
```

In a second terminal:

```bash
cd /d/hantsylabs/spring-graphql-sample/dgs-subscription-ui && npm start
```

Open two browser tabs at `http://localhost:4200`. Send a message from one tab.
Expected: the message appears in **both** tabs — the sender through the mutation response folding into the cache, the other through the subscription. Also confirm no "Subscription closed/error" banner is showing, which would mean the socket never opened.

Stop both processes before continuing.

- [ ] **Step 6: Commit**

```bash
git add .github/workflows/dgs-subscription-ui.yml dgs-subscription-ui/package-lock.json
git commit -m "Run dgs-subscription-ui tests in CI on Node 24 with a committed lockfile"
```

---

### Task 7: Copy `dgs-subscription-ui` to `dgs-subscription-ui-sse`

Duplicate the finished WS app under its new name. The copy still uses WebSockets at the end of this task, and that is deliberate: every commit in this plan must build and pass its tests. Tasks 8 and 9 add the SSE transport and then switch over.

**Files:**
- Create: `dgs-subscription-ui-sse/` (copied from `dgs-subscription-ui/`, excluding `node_modules`, `dist`, `.angular`)
- Modify: `dgs-subscription-ui-sse/package.json`
- Modify: `dgs-subscription-ui-sse/angular.json`
- Modify: `dgs-subscription-ui-sse/src/index.html`

**Interfaces:**
- Consumes: the completed, green `dgs-subscription-ui` from Tasks 5 and 6.
- Produces: `dgs-subscription-ui-sse/`, a building Angular 22 app whose project name, package name and window title identify it as the SSE sample. Its `src/app/` is still identical to the WS app's, including `transport/ws-link.ts` and `config.ts` exporting both `GRAPHQL_HTTP_URL` and `GRAPHQL_WS_URL`.

- [ ] **Step 1: Copy the app without build artifacts**

```bash
cd /d/hantsylabs/spring-graphql-sample
cp -r dgs-subscription-ui dgs-subscription-ui-sse
rm -rf dgs-subscription-ui-sse/node_modules dgs-subscription-ui-sse/dist dgs-subscription-ui-sse/.angular
```

- [ ] **Step 2: Rename in `package.json`**

In `dgs-subscription-ui-sse/package.json`, change `"name": "dgs-subscription-ui"` to `"name": "dgs-subscription-ui-sse"`.

- [ ] **Step 3: Rename in `angular.json`**

In `dgs-subscription-ui-sse/angular.json`, change the top-level project key `dgs-subscription-ui` to `dgs-subscription-ui-sse`, together with any `outputPath`, `tsConfig` or `assets` entry that embeds the old name.

- [ ] **Step 4: Verify no stale references to the old name remain**

Run: `grep -rn "dgs-subscription-ui[^-]" dgs-subscription-ui-sse/angular.json dgs-subscription-ui-sse/package.json`
Expected: no output. Any hit is a stale path or key that must be renamed before continuing.

- [ ] **Step 5: Update the window title**

In `dgs-subscription-ui-sse/src/index.html`, change the `<title>` element to `dgs-subscription-ui-sse`.

- [ ] **Step 6: Install and verify the copy is green**

```bash
cd /d/hantsylabs/spring-graphql-sample/dgs-subscription-ui-sse
npm install
npm run build
npm test -- --watch=false
```

Expected: install succeeds, build succeeds, and the copied `messages.spec.ts`, `ws-link.spec.ts` and `app.component.spec.ts` all pass. If anything fails here, the fault is in the rename, not in the copied code — Task 6 already proved the source app green.

- [ ] **Step 7: Commit**

```bash
git add dgs-subscription-ui-sse
git commit -m "Copy dgs-subscription-ui to dgs-subscription-ui-sse ahead of the SSE transport swap"
```

---

### Task 8: `GraphQLSSELink`

The hand-written Apollo link over `graphql-sse`, written test-first and added purely additively, so the app still builds. `graphql-sse` exposes no `closed` or `error` client listener, so this link is also where those two connection-state transitions are detected.

**Files:**
- Modify: `dgs-subscription-ui-sse/package.json` (adds `graphql-sse`)
- Create: `dgs-subscription-ui-sse/src/app/transport/sse-link.ts`
- Test: `dgs-subscription-ui-sse/src/app/transport/sse-link.spec.ts`

**Interfaces:**
- Consumes: `Client` and `Sink` types from `graphql-sse`; `ApolloLink` and `execute` from `@apollo/client`; `gql` from `apollo-angular`.
- Produces, for Task 9:
  - `type ConnectionState = 'idle' | 'connecting' | 'open' | 'closed' | 'error'`
  - `const connectionState: WritableSignal<ConnectionState>`
  - `const sseClientListeners` — `{ connecting, connected }` only
  - `function createSseClient(url: string): Client`
  - `class GraphQLSSELink extends ApolloLink` with `constructor(client: Client)`

- [ ] **Step 1: Install `graphql-sse`**

```bash
cd /d/hantsylabs/spring-graphql-sample/dgs-subscription-ui-sse
npm install graphql-sse@2.6.3
```

`graphql-ws` stays installed for now; Task 9 removes it together with `ws-link.ts`.

- [ ] **Step 2: Write the failing test**

Create `dgs-subscription-ui-sse/src/app/transport/sse-link.spec.ts`:

```ts
import { execute } from '@apollo/client';
import { gql } from 'apollo-angular';
import type { Client, Sink } from 'graphql-sse';
import { firstValueFrom } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GraphQLSSELink, connectionState } from './sse-link';

const QUERY = gql`
  subscription onMessageSent {
    messageSent {
      id
    }
  }
`;

/** Captures the sink the link hands to graphql-sse so tests can drive it. */
function fakeClient(dispose: () => void) {
  let captured: Sink<unknown> | undefined;
  const subscribe = vi.fn((_request: unknown, sink: Sink<unknown>) => {
    captured = sink;
    return dispose;
  });
  const client = { subscribe, dispose: () => {} } as unknown as Client;
  return { client, subscribe, sink: () => captured! };
}

describe('GraphQLSSELink', () => {
  beforeEach(() => connectionState.set('idle'));

  it('forwards sink values to the observable', async () => {
    const fake = fakeClient(() => {});
    const pending = firstValueFrom(
      execute(new GraphQLSSELink(fake.client), { query: QUERY }),
    );

    fake.sink().next({ data: { messageSent: { id: '1' } } });

    await expect(pending).resolves.toEqual({ data: { messageSent: { id: '1' } } });
  });

  it('passes the operation through to graphql-sse', () => {
    const fake = fakeClient(() => {});
    execute(new GraphQLSSELink(fake.client), {
      query: QUERY,
      variables: { a: 1 },
    }).subscribe();

    expect(fake.subscribe).toHaveBeenCalledTimes(1);
    const request = fake.subscribe.mock.calls[0][0] as Record<string, unknown>;
    expect(request.variables).toEqual({ a: 1 });
    expect(request.operationName).toBe('onMessageSent');
  });

  it('disposes the SSE subscription when the observable is unsubscribed', () => {
    const dispose = vi.fn();
    const fake = fakeClient(dispose);
    const sub = execute(new GraphQLSSELink(fake.client), { query: QUERY }).subscribe();

    sub.unsubscribe();
    expect(dispose).toHaveBeenCalledTimes(1);
  });

  it('marks the connection errored when the sink errors', () => {
    const fake = fakeClient(() => {});
    execute(new GraphQLSSELink(fake.client), { query: QUERY }).subscribe({
      error: () => {},
    });

    fake.sink().error(new Error('stream died'));
    expect(connectionState()).toBe('error');
  });

  it('marks the connection closed when the sink completes', () => {
    const fake = fakeClient(() => {});
    execute(new GraphQLSSELink(fake.client), { query: QUERY }).subscribe();

    fake.sink().complete();
    expect(connectionState()).toBe('closed');
  });
});
```

- [ ] **Step 3: Run the test and verify it fails**

Run: `cd dgs-subscription-ui-sse && npm test -- --watch=false`
Expected: FAIL — cannot resolve `./sse-link`. The pre-existing suites still pass.

- [ ] **Step 4: Write the implementation**

Create `dgs-subscription-ui-sse/src/app/transport/sse-link.ts`:

```ts
import { signal, type WritableSignal } from '@angular/core';
import { ApolloLink, type FetchResult, type Operation } from '@apollo/client';
import { createClient, type Client, type ClientOptions } from 'graphql-sse';
import { Observable } from 'rxjs';

export type ConnectionState = 'idle' | 'connecting' | 'open' | 'closed' | 'error';

export const connectionState: WritableSignal<ConnectionState> = signal<ConnectionState>('idle');

// graphql-sse exposes only connecting/message/connected at client level. The closed
// and error transitions are detected in the link's sink instead, see request() below.
export const sseClientListeners: ClientOptions<false>['on'] = {
  connecting: () => connectionState.set('connecting'),
  connected: () => connectionState.set('open'),
};

export function createSseClient(url: string): Client {
  return createClient({ url, on: sseClientListeners });
}

export class GraphQLSSELink extends ApolloLink {
  constructor(private readonly client: Client) {
    super();
  }

  request(operation: Operation): Observable<FetchResult> {
    return new Observable<FetchResult>(observer => {
      const dispose = this.client.subscribe(
        {
          query: operation.query,
          variables: operation.variables,
          operationName: operation.operationName,
          extensions: operation.extensions,
        },
        {
          next: result => observer.next(result as unknown as FetchResult),
          error: err => {
            connectionState.set('error');
            observer.error(Array.isArray(err) ? err : [err]);
          },
          complete: () => {
            connectionState.set('closed');
            observer.complete();
          },
        },
      );

      return dispose;
    });
  }
}
```

`graphql-sse`'s `ExecutionResult` and Apollo's `FetchResult` are structurally compatible but nominally distinct, hence the double assertion in `next`.

If `ClientOptions` is not generic in `graphql-sse@2.6.3`, use `ClientOptions['on']` instead. The exported member names are what Task 9 depends on, not this type expression.

- [ ] **Step 5: Run the tests and verify they pass**

Run: `cd dgs-subscription-ui-sse && npm test -- --watch=false`
Expected: PASS — `sse-link.spec.ts` adds 5 tests alongside the still-present `messages.spec.ts`, `ws-link.spec.ts` and `app.component.spec.ts`.

- [ ] **Step 6: Verify the app still builds**

Run: `cd dgs-subscription-ui-sse && npm run build`
Expected: succeeds. Nothing imports `sse-link.ts` yet, so this is an additive change.

- [ ] **Step 7: Commit**

```bash
git add dgs-subscription-ui-sse/src/app/transport dgs-subscription-ui-sse/package.json dgs-subscription-ui-sse/package-lock.json
git commit -m "Add GraphQLSSELink over graphql-sse with sink-driven connection state"
```

---

### Task 9: Switch the SSE app from WebSockets to SSE

The single switchover: repoint the component, rewrite `app.config.ts` with the split link, and remove the now-dead WebSocket transport. Ends green.

**Files:**
- Modify: `dgs-subscription-ui-sse/src/app/app.config.ts`
- Modify: `dgs-subscription-ui-sse/src/app/app.component.ts` (one import line)
- Modify: `dgs-subscription-ui-sse/src/app/app.component.html` (the `<h1>`)
- Modify: `dgs-subscription-ui-sse/src/app/config.ts`
- Modify: `dgs-subscription-ui-sse/package.json` (removes `graphql-ws`)
- Delete: `dgs-subscription-ui-sse/src/app/transport/ws-link.ts`
- Delete: `dgs-subscription-ui-sse/src/app/transport/ws-link.spec.ts`

**Interfaces:**
- Consumes: `createSseClient` and `GraphQLSSELink` from Task 8; `GRAPHQL_HTTP_URL` from Task 7.
- Produces: the finished SSE app. After this task `dgs-subscription-ui-sse/src/app/app.component.ts` differs from `dgs-subscription-ui/src/app/app.component.ts` by exactly one import path.

- [ ] **Step 1: Narrow `config.ts` to the single SSE URL**

Replace `dgs-subscription-ui-sse/src/app/config.ts` with:

```ts
// The SSE backend serves queries, mutations and subscriptions from this one URL;
// subscriptions are distinguished by the Accept: text/event-stream header.
export const GRAPHQL_HTTP_URL = 'http://localhost:8080/graphql';
```

- [ ] **Step 2: Repoint the component's one transport import**

In `dgs-subscription-ui-sse/src/app/app.component.ts`, change

```ts
import { connectionState } from './transport/ws-link';
```

to

```ts
import { connectionState } from './transport/sse-link';
```

No other line in the file changes. That is the whole point of folding subscription events into the Apollo cache.

- [ ] **Step 3: Rewrite `app.config.ts` with the split link**

Replace `dgs-subscription-ui-sse/src/app/app.config.ts` with:

```ts
import { provideHttpClient } from '@angular/common/http';
import { inject, provideZonelessChangeDetection, type Provider } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations';
import { InMemoryCache, split } from '@apollo/client';
import { getMainDefinition } from '@apollo/client/utilities';
import { provideApollo } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';
import { GRAPHQL_HTTP_URL } from './config';
import { GraphQLSSELink, createSseClient } from './transport/sse-link';

export function appProviders(): Provider[] {
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

Queries and mutations go over plain HTTP; only subscriptions go over SSE, to the same URL.

- [ ] **Step 4: Update the visible heading**

In `dgs-subscription-ui-sse/src/app/app.component.html`, change

```html
<h1>DGS Subscription — WebSocket</h1>
```

to

```html
<h1>DGS Subscription — Server-Sent Events</h1>
```

- [ ] **Step 5: Delete the WebSocket transport and its dependency**

```bash
cd /d/hantsylabs/spring-graphql-sample/dgs-subscription-ui-sse
rm src/app/transport/ws-link.ts src/app/transport/ws-link.spec.ts
npm uninstall graphql-ws
```

- [ ] **Step 6: Verify no WebSocket references survive**

Run: `grep -rn "graphql-ws\|ws-link\|GRAPHQL_WS_URL" dgs-subscription-ui-sse/src dgs-subscription-ui-sse/package.json`
Expected: no output.

- [ ] **Step 7: Run the full test suite**

Run: `cd dgs-subscription-ui-sse && npm test -- --watch=false`
Expected: PASS — `messages.spec.ts`, `sse-link.spec.ts` and `app.component.spec.ts` all green, and `ws-link.spec.ts` gone.

- [ ] **Step 8: Verify the production build**

Run: `cd dgs-subscription-ui-sse && npm run build`
Expected: succeeds with no TypeScript errors.

- [ ] **Step 9: Confirm the two components differ only by the transport import**

Run: `diff dgs-subscription-ui/src/app/app.component.ts dgs-subscription-ui-sse/src/app/app.component.ts`
Expected: exactly one differing line pair — `./transport/ws-link` versus `./transport/sse-link`. Anything more means the copy drifted and should be reconciled now.

- [ ] **Step 10: Commit**

```bash
git add -A dgs-subscription-ui-sse
git commit -m "Route dgs-subscription-ui-sse subscriptions through GraphQLSSELink"
```

---

### Task 10: Repository wiring for `dgs-subscription-ui-sse`

**Files:**
- Create: `.github/workflows/dgs-subscription-ui-sse.yml`
- Modify: `.github/workflows/dgs-subscription-ui.yml` — only if Task 6 has not already been done
- Modify: `.github/dependabot.yml`
- Modify: `README.md`
- Modify: `dgs-subscription-ui-sse/package-lock.json` (generated, must be committed)

**Interfaces:**
- Consumes: a building, testing SSE app from Task 9.
- Produces: CI coverage, Dependabot coverage and a README entry for the new project.

- [ ] **Step 1: Verify the lockfile exists**

Run: `test -f dgs-subscription-ui-sse/package-lock.json && echo present || echo MISSING`
Expected: `present`. If missing, run `cd dgs-subscription-ui-sse && npm install`.

- [ ] **Step 2: Create the workflow**

Create `.github/workflows/dgs-subscription-ui-sse.yml`:

```yaml
name: dgs-subscription-ui-sse

on:
  push:
    paths:
      - "dgs-subscription-ui-sse/**"
    branches: [ master ]
  pull_request:
    paths:
      - "dgs-subscription-ui-sse/**"
    types:
      - opened
      - synchronize
      - reopened

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4
      - name: Setup NodeJS
        uses: actions/setup-node@v4
        with:
          node-version: "24"

      - uses: actions/cache@v4
        with:
          path: ~/.npm
          key: ${{ runner.os }}-node-${{ hashFiles('dgs-subscription-ui-sse/package-lock.json') }}
          restore-keys: |
            ${{ runner.os }}-node-

      - name: Install, build and test
        run: |
          cd ./dgs-subscription-ui-sse
          npm ci
          npm run build
          npm test -- --watch=false
```

Note the `paths` filter must be `dgs-subscription-ui-sse/**`. Because `dgs-subscription-ui/**` is not a prefix-match on the directory separator, the existing UI workflow does **not** also trigger for the new directory; both filters are needed and neither is redundant.

- [ ] **Step 3: Add the Dependabot entry**

In `.github/dependabot.yml`, append after the existing `/dgs-subscription-ui` block:

```yaml
  # Angular client for dgs-subscription-sse. Same ignore policy as
  # dgs-subscription-ui: Angular majors need a coordinated toolchain migration.
  - package-ecosystem: "npm"
    directory: "/dgs-subscription-ui-sse"
    schedule:
      interval: weekly
    open-pull-requests-limit: 10
    reviewers:
      - "hantsy"
    assignees:
      - "hantsy"
    labels:
      - "dependencies"
    ignore:
      - dependency-name: "@angular/*"
        update-types: ["version-update:semver-major"]
      - dependency-name: "*"
        update-types: ["version-update:semver-major", "version-update:semver-minor"]
```

- [ ] **Step 4: Verify Dependabot coverage matches the build files exactly**

```bash
cd /d/hantsylabs/spring-graphql-sample
find . -maxdepth 3 \( -name "build.gradle" -o -name "build.gradle.kts" -o -name "package.json" \) \
  -not -path "*/node_modules/*" -not -path "*/build/*" -not -path "*/bin/*" \
  | sed 's|^\./||;s|/[^/]*$||' | sort -u > /tmp/actual.txt
grep -oE 'directory: "/[^"]*"' .github/dependabot.yml \
  | sed 's|directory: "/||;s|"||' | grep -v '^$' | sort -u > /tmp/configured.txt
echo "--- build dirs missing from dependabot (expect only legacy/*) ---"
comm -23 /tmp/actual.txt /tmp/configured.txt
echo "--- dependabot dirs with no build file (expect none) ---"
comm -13 /tmp/actual.txt /tmp/configured.txt
```

Expected: the first list contains exactly the five `legacy/...` directories; the second list is empty.

- [ ] **Step 5: Add the README row**

In `README.md`, in the "Example Codes" table directly after the `dgs-subscription-ui` row, add:

```markdown
|[dgs-subscription-ui-sse](./dgs-subscription-ui-sse)  | Angular Client app for dgs-subscription-sse, consuming subscriptions over Server-Sent Events|
```

Then, immediately after that table, add:

```markdown
Both Angular clients default to `http://localhost:8080/graphql`. `dgs-subscription-ui` pairs with the `dgs-subscription-ws` backend and `dgs-subscription-ui-sse` pairs with `dgs-subscription-sse`; running the wrong backend, or both at once, means the UI silently talks to the wrong transport.
```

- [ ] **Step 6: Verify the YAML files parse**

Run: `python -c "import yaml; [yaml.safe_load(open(f, encoding='utf-8')) for f in ['.github/dependabot.yml', '.github/workflows/dgs-subscription-ui-sse.yml']]; print('valid yaml')"`
Expected: `valid yaml`.

- [ ] **Step 7: Commit**

```bash
git add .github/workflows/dgs-subscription-ui-sse.yml .github/dependabot.yml README.md dgs-subscription-ui-sse/package-lock.json
git commit -m "Wire dgs-subscription-ui-sse into CI, dependabot and the README"
```

---

### Task 11: Final verification

**Files:** none modified.

**Interfaces:**
- Consumes: everything above.
- Produces: evidence that both apps and the touched backend are green.

- [ ] **Step 1: Verify both apps from a clean install**

```bash
cd /d/hantsylabs/spring-graphql-sample
for app in dgs-subscription-ui dgs-subscription-ui-sse; do
  echo "##### $app #####"
  (cd "$app" && rm -rf node_modules && npm ci && npm run build && npm test -- --watch=false) \
    || echo "##### FAILED: $app #####"
done
```

Expected: both print a successful build and a passing Vitest run, with no `FAILED` marker.

- [ ] **Step 2: Verify the SSE backend is still green**

Run: `cd dgs-subscription-sse && ./gradlew build --console=plain`
Expected: `BUILD SUCCESSFUL`, with both test classes reporting 0 failures.

- [ ] **Step 3: Manual smoke test the SSE app**

```bash
cd /d/hantsylabs/spring-graphql-sample/dgs-subscription-sse && ./gradlew bootRun
```

In a second terminal:

```bash
cd /d/hantsylabs/spring-graphql-sample/dgs-subscription-ui-sse && npm start
```

Open two tabs at `http://localhost:4200`. Send a message from one.
Expected: it appears in both tabs. Then leave both tabs idle for over 45 seconds and send another message — the backend's `spring.mvc.async.request-timeout=45000` will have dropped the idle stream, so this proves the client reconnects and delivery resumes.

Stop both processes.

- [ ] **Step 4: Confirm nothing outside scope changed**

Run: `git status --short; git diff --name-only origin/upgrade/spring-boot-4.1.2-java25..HEAD -- legacy/`
Expected: a clean working tree, and no files under `legacy/` in the diff.

- [ ] **Step 5: Push**

```bash
git push origin upgrade/spring-boot-4.1.2-java25
```

Then confirm the two UI workflows and `dgs-subscription-sse` all report success:

Run: `gh run list --branch upgrade/spring-boot-4.1.2-java25 --limit 20`
Expected: `dgs-subscription-ui`, `dgs-subscription-ui-sse` and `dgs-subscription-sse` all `completed` / `success`.
