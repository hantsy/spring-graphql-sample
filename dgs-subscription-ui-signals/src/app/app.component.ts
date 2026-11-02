import { Component, computed, inject, resource, signal, type Signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Apollo } from 'apollo-angular';
import { catchError, filter, firstValueFrom, map, of } from 'rxjs';
import {
  MESSAGES_QUERY,
  MESSAGE_SENT_SUBSCRIPTION,
  SEND_MESSAGE_MUTATION,
  describeError,
  type MessageSentData,
  type MessagesData,
  type SendMessageData,
} from './graphql/messages';
import { connectionState } from './transport/ws-link';

/** Shape emitted by the streaming subscription resource. */
type SubStreamItem = { value: MessageSentData | null } | { error: Error };

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
})
export class AppComponent {
  private readonly apollo = inject(Apollo);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({ body: ['', Validators.required] });

  // ── Query as a one-time resource ──────────────────────────────────────
  readonly messagesResource = resource({
    loader: () =>
      firstValueFrom(
        this.apollo.query<MessagesData>({ query: MESSAGES_QUERY }).pipe(
          map(r => r.data?.messages ?? []),
        ),
      ),
  });

  // ── Subscription as a streaming resource ──────────────────────────────
  // apollo.subscribe() emits SubscribeResult<T> (with optional .data).
  // The stream loader must return Signal<ResourceStreamItem<U>>, so we wrap
  // each emission in { value: … } and map errors to { error: … }.
  private readonly subscriptionSignal: Signal<SubStreamItem> = toSignal(
    this.apollo.subscribe<MessageSentData>({ query: MESSAGE_SENT_SUBSCRIPTION }).pipe(
      filter(result => !!result.data),
      map(result => ({ value: result.data as MessageSentData | null })),
      catchError(err => of({ error: err instanceof Error ? err : new Error(String(err)) })),
    ),
    { initialValue: { value: null as MessageSentData | null } },
  )!;

  readonly subscriptionResource = resource<MessageSentData | null, null>({
    stream: () => this.subscriptionSignal,
  });

  // ── Computed signals ──────────────────────────────────────────────────
  readonly messages = computed(() => {
    const initial = this.messagesResource.hasValue() ? this.messagesResource.value()! : [];
    const newMessage = this.subscriptionResource.value();
    if (!newMessage) return initial;
    return [...initial, newMessage.messageSent];
  });

  readonly loading = computed(() => this.messagesResource.isLoading());
  readonly error = computed(() => {
    const err = this.messagesResource.error();
    return err ? describeError(err) : null;
  });

  readonly connection = connectionState;

  readonly sending = signal(false);
  readonly sendError = signal<string | null>(null);

  send(): void {
    const body = this.form.controls.body.value.trim();
    if (!body) {
      return;
    }

    this.sending.set(true);
    this.sendError.set(null);

    this.apollo
      .mutate<SendMessageData>({
        mutation: SEND_MESSAGE_MUTATION,
        variables: { message: { body } },
      })
      .subscribe({
        next: result => {
          this.sending.set(false);
          if (result.error) {
            this.sendError.set(describeError(result.error));
            return;
          }
          // the new message arrives through the subscription, not from this response
          this.form.controls.body.reset('');
        },
        error: err => {
          this.sending.set(false);
          this.sendError.set(describeError(err));
        },
      });
  }
}
