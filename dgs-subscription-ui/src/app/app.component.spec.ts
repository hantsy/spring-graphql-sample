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
