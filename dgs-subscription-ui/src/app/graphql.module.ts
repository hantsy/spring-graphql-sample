import {
  provideHttpClient,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { inject, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ApolloClientOptions, InMemoryCache, split } from '@apollo/client/core';
import { WebSocketLink } from '@apollo/client/link/ws';
import { getMainDefinition } from '@apollo/client/utilities';
import { APOLLO_OPTIONS, provideApollo } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';

@NgModule({
  imports: [BrowserModule],
  providers: [
    provideApollo(() => {
      const httpLink = inject(HttpLink);

      // Create an http link:
      const http = httpLink.create({
        uri: 'http://localhost:8080/graphql',
      });
      // Create a WebSocket link:
      const ws = new WebSocketLink({
        uri: 'ws://localhost:8080/graphql',
        options: {
          reconnect: true,
        },
      });
      // using the ability to split links, you can send data to each link
      // depending on what kind of operation is being sent
      const link = split(
        // split based on operation type
        ({ query }) => {
          const definition = getMainDefinition(query);
          return (
            definition.kind === 'OperationDefinition' &&
            definition.operation === 'subscription'
          );
        },
        ws,
        http
      );
      return {
        link,
        cache: new InMemoryCache(),
      };
    }),
  ],
})
export class GraphQLModule {}
