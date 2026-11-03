import { beforeEach, describe, expect, it } from 'vitest';
import { firstValueFrom } from 'rxjs';
import { connectionState$, wsClientListeners, type ConnectionState } from './ws-link';

describe('wsClientListeners', () => {
  beforeEach(() => connectionState$.next('idle'));

  it('exposes the current state to late subscribers', async () => {
    connectionState$.next('open');
    await expect(firstValueFrom(connectionState$)).resolves.toBe('open');
  });

  it('reports connecting', async () => {
    wsClientListeners.connecting!(false);
    await expect(firstValueFrom(connectionState$)).resolves.toBe('connecting');
  });

  it('reports open once connected', async () => {
    wsClientListeners.connected!(null, undefined, false);
    await expect(firstValueFrom(connectionState$)).resolves.toBe('open');
  });

  it('reports closed', async () => {
    wsClientListeners.closed!({} as never);
    await expect(firstValueFrom(connectionState$)).resolves.toBe('closed');
  });

  it('reports error', async () => {
    wsClientListeners.error!(new Error('socket died'));
    await expect(firstValueFrom(connectionState$)).resolves.toBe('error');
  });

  it('emits every transition in order', () => {
    const states: ConnectionState[] = [];
    const subscription = connectionState$.subscribe(state => states.push(state));

    wsClientListeners.connecting!(false);
    wsClientListeners.connected!(null, undefined, false);
    wsClientListeners.closed!({} as never);

    subscription.unsubscribe();
    expect(states).toEqual(['idle', 'connecting', 'open', 'closed']);
  });
});
