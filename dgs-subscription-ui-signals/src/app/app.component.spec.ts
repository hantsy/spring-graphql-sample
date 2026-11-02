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
});
