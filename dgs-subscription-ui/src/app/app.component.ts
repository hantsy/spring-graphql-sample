import { AsyncPipe } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Apollo } from 'apollo-angular';
import { BehaviorSubject, catchError, map, Observable, of, shareReplay } from 'rxjs';
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
import { connectionState$, type ConnectionState } from './transport/ws-link';

/** Everything the template needs to render the messages query. */
interface QueryState {
  loading: boolean;
  messages: TextMessage[];
  error: string | null;
}

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly apollo = inject(Apollo);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({ body: ['', Validators.required] });

  private readonly messagesQueryRef = this.apollo.watchQuery<MessagesData>({
    query: MESSAGES_QUERY,
  });

  // One shared subscription feeds both the list and the error banner, so the
  // query is issued once no matter how many async pipes read it.
  private readonly queryState$: Observable<QueryState> = this.messagesQueryRef.valueChanges.pipe(
    map(result => ({
      loading: result.loading,
      // Apollo types an in-flight result as a deep partial; a resolved result is complete.
      messages: (result.data?.messages ?? []) as TextMessage[],
      error: result.error ? describeError(result.error) : null,
    })),
    catchError(err =>
      of<QueryState>({ loading: false, messages: [], error: describeError(err) }),
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /** `null` while the query is still loading, so the template can tell the two apart. */
  readonly messages$: Observable<TextMessage[] | null> = this.queryState$.pipe(
    map(state => (state.loading ? null : state.messages)),
  );

  readonly error$: Observable<string | null> = this.queryState$.pipe(
    map(state => state.error),
  );

  readonly connectionState$: Observable<ConnectionState> = connectionState$;

  private readonly sendingSubject = new BehaviorSubject<boolean>(false);
  readonly sending$: Observable<boolean> = this.sendingSubject.asObservable();

  private readonly sendErrorSubject = new BehaviorSubject<string | null>(null);
  readonly sendError$: Observable<string | null> = this.sendErrorSubject.asObservable();

  private stopSubscription?: () => void;

  ngOnInit(): void {
    // Pushed messages are merged into the query result, so no separate stream is
    // rendered; the cache broadcast is what updates the list. Apollo hands the
    // previous result as a deep partial, hence the narrowing before merging.
    this.stopSubscription = this.messagesQueryRef.subscribeToMore<MessageSentData>({
      document: MESSAGE_SENT_SUBSCRIPTION,
      updateQuery: (prev, { subscriptionData }) =>
        mergeNewMessage(
          { messages: (prev?.messages ?? []) as TextMessage[] },
          { subscriptionData },
        ),
    });
  }

  ngOnDestroy(): void {
    this.stopSubscription?.();
  }

  send(): void {
    const body = this.form.controls.body.value.trim();
    if (!body) {
      return;
    }

    this.sendingSubject.next(true);
    this.sendErrorSubject.next(null);

    this.apollo
      .mutate<SendMessageData>({
        mutation: SEND_MESSAGE_MUTATION,
        variables: { message: { body } },
      })
      .subscribe({
        next: result => {
          this.sendingSubject.next(false);
          if (result.error) {
            this.sendErrorSubject.next(describeError(result.error));
            return;
          }
          // the new message arrives through the subscription, not from this response
          this.form.controls.body.reset('');
        },
        error: err => {
          this.sendingSubject.next(false);
          this.sendErrorSubject.next(describeError(err));
        },
      });
  }
}
