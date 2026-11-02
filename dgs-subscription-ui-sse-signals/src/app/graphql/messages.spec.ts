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
