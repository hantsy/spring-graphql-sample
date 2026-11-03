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
