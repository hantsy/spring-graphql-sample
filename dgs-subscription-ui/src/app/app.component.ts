import { Component, OnDestroy, OnInit } from '@angular/core';
import { Apollo, QueryRef, gql } from 'apollo-angular';

const MESSAGES_QUERY = gql`
  query allMessages {
    messages {
      id
      body
      sentAt
    }
  }
`;

const SEND_MESSAGE_QUERY = gql`
  mutation sendMessage($message: TextMessageInput!) {
    send(message: $message) {
      id
      body
      sentAt
    }
  }
`;

const MESSAGE_SUBSCRIPTION_QUERY = gql`
  subscription onMessageSent {
    messageSent {
      id
      body
      sentAt
    }
  }
`;

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'client';
  message: string | null = '';
  messages: any[] = [];

  messagesQuery: QueryRef<any>;

  constructor(private apollo: Apollo) {
    this.messagesQuery = this.apollo.watchQuery({
      query: MESSAGES_QUERY,
    });
    this.messagesQuery.valueChanges.subscribe(
      ({ data }) => {
        console.log('data: ', data);
        if (data) {
          this.messages = data.messages;
        }
      },
      (error) => console.log('errors: {}', error)
    );
  }

  subscriptionToNewMessage() {
    this.messagesQuery.subscribeToMore({
      document: MESSAGE_SUBSCRIPTION_QUERY,
      variables: {},
      updateQuery: (prev, { subscriptionData }) => {
        console.log('prev: {}', prev);
        console.log('subscription data: {}', subscriptionData);
        if (!subscriptionData.data) {
          return prev;
        }
        const newMsg = subscriptionData.data.messageSent;
        if (prev && prev.messages) {
          return { messages: [...prev.messages, newMsg] };
        } else {
          return {
            messages: [newMsg],
          };
        }
      },
    });
  }

  ngOnInit(): void {
    this.messages = [];
    this.subscriptionToNewMessage();
  }

  addMessage(msg: any) {
    this.messages = [...this.messages, msg];
    //console.log("messages::" + this.messages);
  }

  ngOnDestroy(): void {}

  sendMessage() {
    console.log('sending message:' + this.message);
    this.apollo
      .mutate({
        mutation: SEND_MESSAGE_QUERY,
        variables: {
          message: {
            body: this.message,
          },
        },
      })
      .subscribe(
        ({ data }) => {
          console.log('content:', data);
        },
        (error) => console.error(error)
      );
    this.message = null;
  }
}
