# File upload

The GraphQL specification does not define file uploads. The interoperable
standard is the
[GraphQL multipart request specification](https://github.com/jaydenseric/graphql-multipart-request-spec),
which defines a multipart form structure that clients and servers from
different ecosystems can share. This page explains that structure and shows
how the examples in this repository receive uploads.

## The Upload scalar

A schema that accepts files declares a custom scalar, conventionally named
`Upload`, and uses it in a mutation:

```graphql
scalar Upload

type Mutation {
    upload(file: Upload!): Boolean
}
```

Uploads are mutations: they change server state, so they never run over HTTP
GET.

## The multipart form structure

A multipart request follows
[RFC 7578](https://tools.ietf.org/html/rfc7578) and orders its fields so the
server can resolve the operation while the files are still uploading:

1. `operations` — a JSON-encoded operations object (the usual `query`,
   `variables`, and `operationName`), with every file value replaced by
   `null`.
2. `map` — a JSON-encoded map from each file's form field name to an array of
   object paths locating that file inside `operations`, for example
   `variables.file`.
3. One field per file — the file itself, under the field name the map
   references.

The following request uploads a single file `a.txt` to the
[dgs-fileupload](https://github.com/hantsy/spring-graphql-sample/tree/master/dgs-fileupload)
example:

```bash
curl http://localhost:8080/graphql \
  -F operations='{ "query": "mutation ($file: Upload!) { upload(file: $file) }", "variables": { "file": null } }' \
  -F map='{ "0": ["variables.file"] }' \
  -F 0=@a.txt
```

The specification also supports file lists (`variables.files.0`,
`variables.files.1`), files nested anywhere in the variables, operation
batching, and file deduplication — one file field can map to several paths.

## Receiving uploads in DGS

The DGS framework maps the file to a Spring `MultipartFile`. The following
excerpt is from
[FileUploadMutation.java](https://github.com/hantsy/spring-graphql-sample/blob/master/dgs-fileupload/src/main/java/com/example/demo/gql/FileUploadMutation.java):

```java
@DgsComponent
public class FileUploadMutation {

    @DgsMutation
    public Boolean upload(DgsDataFetchingEnvironment env) {
        var file = (MultipartFile) env.getArgument("file");
        // read file.getInputStream(), file.getOriginalFilename(), ...
        return true;
    }
}
```

## Receiving uploads in Spring GraphQL

Spring GraphQL binds the file the same way. The following excerpt is from
[DataFetchers.java](https://github.com/hantsy/spring-graphql-sample/blob/master/spring-graphql/src/main/java/com/example/demo/gql/DataFetchers.java)
in the `spring-graphql` example:

```java
public DataFetcher<Boolean> upload() {
    return (DataFetchingEnvironment dfe) -> {
        MultipartFile file = dfe.getArgument("file");
        String content = StreamUtils.copyToString(
                file.getInputStream(), StandardCharsets.UTF_8);
        return true;
    };
}
```

In both frameworks the multipart parsing happens in the transport layer; the
data fetcher only sees the resolved `MultipartFile` argument.