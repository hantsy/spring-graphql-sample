import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Apollo } from 'apollo-angular';
import { catchError, map, of } from 'rxjs';
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
import { connectionState } from './transport/ws-link';

interface UiState {
  loading: boolean;
  messages: TextMessage[];
  error: string | null;
}

const LOADING: UiState = { loading: true, messages: [], error: null };

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

  private readonly queryRef = this.apollo.watchQuery<MessagesData>({
    query: MESSAGES_QUERY,
  });

  // The subscription folds into the cache, so valueChanges is the only stream the
  // component reads. subscribeToMore must be created for the subscription to start.
  private readonly moreSubscription = this.queryRef.subscribeToMore<MessageSentData>({
    document: MESSAGE_SENT_SUBSCRIPTION,
    updateQuery: mergeNewMessage as any,
  });

  private readonly state$ = this.queryRef.valueChanges.pipe(
    map(r => ({
      loading: r.loading,
      messages: (r.data as MessagesData | undefined)?.messages ?? [],
      error: r.error ? describeError(r.error) : null,
    })),
    catchError(err => of({ loading: false as boolean, messages: [] as TextMessage[], error: describeError(err) as string | null })),
  );

  private readonly state = toSignal(this.state$, { initialValue: LOADING });

  readonly messages = computed(() => this.state().messages);
  readonly loading = computed(() => this.state().loading);
  readonly error = computed(() => this.state().error);
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
