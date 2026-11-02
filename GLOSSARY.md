# KtorAOT

The Person API domain: a WebApi that serves persons and a ConsoleClient that consumes it.

## Language

**Consumer**:
A client application that calls the Person API and depends on its responses. ConsoleClient is the consumer.
_Avoid_: Client (when it could mean the HTTP client library)

**Provider**:
The service that answers a consumer's requests. WebApi is the provider.
_Avoid_: Server, backend

**Contract**:
A recorded set of interactions that describes what a consumer expects from a provider. Stored as a Pact file.
_Avoid_: Spec, schema

**Interaction**:
One expected request and its response within a contract, for example "GET a person by id returns 200".
_Avoid_: Test case, example

**Provider state**:
A named precondition that the provider must set up before an interaction can be verified, for example "a person with this id exists".
_Avoid_: Fixture, given
