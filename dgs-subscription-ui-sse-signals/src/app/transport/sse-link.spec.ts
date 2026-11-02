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
      execute(new GraphQLSSELink(fake.client), { query: QUERY }, {} as any),
    );

    fake.sink().next({ data: { messageSent: { id: '1' } } });

    await expect(pending).resolves.toEqual({ data: { messageSent: { id: '1' } } });
  });

  it('passes the operation through to graphql-sse', () => {
    const fake = fakeClient(() => {});
    execute(new GraphQLSSELink(fake.client), {
      query: QUERY,
      variables: { a: 1 },
    }, {} as any).subscribe();

    expect(fake.subscribe).toHaveBeenCalledTimes(1);
    const request = fake.subscribe.mock.calls[0][0] as Record<string, unknown>;
    expect(request['variables']).toEqual({ a: 1 });
    expect(request['operationName']).toBe('onMessageSent');
  });

  it('disposes the SSE subscription when the observable is unsubscribed', () => {
    const dispose = vi.fn();
    const fake = fakeClient(dispose);
    const sub = execute(new GraphQLSSELink(fake.client), { query: QUERY }, {} as any).subscribe();

    sub.unsubscribe();
    expect(dispose).toHaveBeenCalledTimes(1);
  });

  it('marks the connection errored when the sink errors', () => {
    const fake = fakeClient(() => {});
    execute(new GraphQLSSELink(fake.client), { query: QUERY }, {} as any).subscribe({
      error: () => {},
    });

    fake.sink().error(new Error('stream died'));
    expect(connectionState()).toBe('error');
  });

  it('marks the connection closed when the sink completes', () => {
    const fake = fakeClient(() => {});
    execute(new GraphQLSSELink(fake.client), { query: QUERY }, {} as any).subscribe();

    fake.sink().complete();
    expect(connectionState()).toBe('closed');
  });
});
