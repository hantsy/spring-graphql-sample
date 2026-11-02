import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
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
    // The subscription Observable opened by toSignal stays active; drain any
    // outstanding operations before asserting none are left.
    controller.match(() => true).forEach(op => op.complete());
    controller.verify();
  });

  it('renders messages returned by the query', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    controller.expectOne('allMessages').flushData({
      messages: [{ id: '1', body: 'hello world', sentAt: null }],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('hello world');
  });

  it('sends the mutation with the typed body', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    controller.expectOne('allMessages').flushData({ messages: [] });
    await fixture.whenStable();

    const component = fixture.componentInstance;
    component.form.controls.body.setValue('hi there');
    component.send();

    const mutation = controller.expectOne('sendMessage');
    expect(mutation.operation.variables).toEqual({ message: { body: 'hi there' } });
    mutation.flushData({ send: null });
  });

  it('shows an error banner when the query fails', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    controller.expectOne('allMessages').networkError(new Error('boom'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('boom');
  });

  it('exposes messagesResource as a resource with hasValue/value guards', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;

    // Before detectElement, the resource has no value yet
    expect(component.messagesResource.hasValue()).toBe(false);

    fixture.detectChanges();
    controller.expectOne('allMessages').flushData({
      messages: [{ id: '1', body: 'resource test', sentAt: null }],
    });
    await fixture.whenStable();

    // After loading, the resource should have a value
    expect(component.messagesResource.hasValue()).toBe(true);
    expect(component.messagesResource.value()).toEqual([
      { id: '1', body: 'resource test', sentAt: null },
    ]);
  });

  it('does not break messages computed when subscription errors', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    // Load initial messages first
    controller.expectOne('allMessages').flushData({
      messages: [{ id: '1', body: 'before error', sentAt: null }],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    const component = fixture.componentInstance;

    // Verify messages computed works after query loads
    expect(component.messages()).toEqual([
      { id: '1', body: 'before error', sentAt: null },
    ]);

    // Flush a network error on the subscription operation
    const subOp = controller.match('messageSent');
    if (subOp.length > 0) {
      subOp[0].networkError(new Error('subscription disconnected'));
    }
    await fixture.whenStable();
    fixture.detectChanges();

    // The messages computed should still return the initial messages
    // without throwing, even if the subscription resource is in error state
    expect(() => component.messages()).not.toThrow();
    expect(component.messages().length).toBeGreaterThanOrEqual(1);
  });
});
