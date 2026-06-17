# Bruno API Collection

This directory contains Bruno requests for manually proving local RM connectivity,
including fetching a real AAD-backed token from a locally running `opal-user-service`
and using it against RM's `testing-support` endpoint.

## Getting Started

1. Install Bruno

```bash
brew install --cask bruno
```

2. Create a local environment file

```bash
cp environments/env.bru.template environments/local.bru
```

3. Update the local environment values if you are not using the default local ports

```text
vars {
  baseURL: http://localhost:4556
  userServiceBaseURL: http://localhost:4555
}
vars:secret [
  BEARER_TOKEN
]
```

4. Open the `bruno` directory as a Bruno collection

5. Run the `User Service/Get test user token` request, then copy the `access_token`
   value from the response into the `BEARER_TOKEN` secret in your local environment

6. Run `RM/ping` to verify the public testing-support endpoint is up
7. Run `RM/auth-check` to verify that RM accepts the token and can resolve user state

## Files to Commit

- `collections/`
- `config.json`
- `bruno.json`
- `environments/env.bru.template`

Do not commit local environment files containing real tokens.
