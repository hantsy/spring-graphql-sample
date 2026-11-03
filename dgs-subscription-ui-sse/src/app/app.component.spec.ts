import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ApolloTestingController, ApolloTestingModule } from 'apollo-angular/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { connectionState$ } from './transport/sse-link';
import { AppComponent } from './app.component';
import type { TextMessage } from './graphql/messages';

const hello: TextMessage = { id: '1', body: 'hello world', sentAt: null };

describe('AppComponent', () => {
  let controller: ApolloTestingController;

  beforeEach(() => {
    connectionState$.next('idle');
    TestBed.configureTestingModule({
      imports: [AppComponent, ApolloTestingModule],
      providers: [provideHttpClient(), provideAnimationsAsync()],
    });
    controller = TestBed.inject(ApolloTestingController);
  });

  afterEach(() => {
    // The subscription opened in ngOnInit stays active; drain any outstanding
    // operations before asserting none are left.
    controller.match(() => true).forEach(op => op.complete());
    controller.verify();
  });

  it('loads messages as Observable', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    controller.expectOne('allMessages').flushData({ messages: [hello] });
    fixture.detectChanges();

    await expect(firstValueFrom(fixture.componentInstance.messages$)).resolves.toEqual([hello]);
  });

  it('renders messages returned by the query', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    controller.expectOne('allMessages').flushData({ messages: [hello] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('hello world');
  });

  it('shows a loading placeholder until the query resolves', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Loading messages');

    controller.expectOne('allMessages').flushData({ messages: [] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No messages yet');
  });

  it('appends messages pushed through the subscription', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    controller.expectOne('allMessages').flushData({ messages: [hello] });
    await fixture.whenStable();

    controller.expectOne('onMessageSent').flushData({
      messageSent: { id: '2', body: 'pushed later', sentAt: null },
    });
    await fixture.whenStable();
    fixture.detectChanges();

    await expect(firstValueFrom(fixture.componentInstance.messages$)).resolves.toEqual([
      hello,
      { id: '2', body: 'pushed later', sentAt: null },
    ]);
    expect(fixture.nativeElement.textContent).toContain('pushed later');
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
    await fixture.whenStable();

    expect(component.form.controls.body.value).toBe('');
  });

  it('disables the send button only while the mutation is in flight', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    controller.expectOne('allMessages').flushData({ messages: [] });
    await fixture.whenStable();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    );
    fixture.componentInstance.form.controls.body.setValue('in flight');
    fixture.detectChanges();
    expect(button.disabled).toBe(false);

    fixture.componentInstance.send();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(button.disabled).toBe(true);

    controller.expectOne('sendMessage').flushData({ send: null });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.banner-error')).toBeNull();
    await expect(firstValueFrom(fixture.componentInstance.sendError$)).resolves.toBeNull();
  });

  it('shows a failed mutation in a banner and keeps the draft', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    controller.expectOne('allMessages').flushData({ messages: [] });
    await fixture.whenStable();

    const component = fixture.componentInstance;
    component.form.controls.body.setValue('keep me');
    component.send();

    controller.expectOne('sendMessage').networkError(new Error('socket busy'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('socket busy');
    expect(component.form.controls.body.value).toBe('keep me');
  });

  it('shows an error banner when the query fails', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    controller.expectOne('allMessages').networkError(new Error('boom'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('boom');
    await expect(firstValueFrom(fixture.componentInstance.messages$)).resolves.toEqual([]);
  });

  it('shows the transport banner from the connection state stream', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    controller.expectOne('allMessages').flushData({ messages: [] });
    await fixture.whenStable();

    connectionState$.next('error');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Subscription error');
  });
});
