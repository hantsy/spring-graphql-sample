import { beforeEach, describe, expect, it } from 'vitest';
import { connectionState, wsClientListeners } from './ws-link';

describe('wsClientListeners', () => {
  beforeEach(() => connectionState.set('idle'));

  it('reports connecting', () => {
    wsClientListeners.connecting!(false);
    expect(connectionState()).toBe('connecting');
  });

  it('reports open once connected', () => {
    wsClientListeners.connected!(null, undefined, false);
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
