# opal-rm-service

Opal Reciprocal Maintenance service.

## Running locally

Start the local RM Postgres instance:

```bash
docker compose up -d opal-rm-db
```

Build the service:

```bash
./gradlew build
```

Start the service:

```bash
./gradlew run
```

Check health:

```bash
curl http://localhost:4556/health
```

OpenAPI and Swagger UI are available when the app is running:

```bash
curl http://localhost:4556/v3/api-docs
open http://localhost:4556/swagger-ui/index.html
```

Inspect the bootstrap table:

```bash
docker exec opal-rm-db psql -U opal-rm -d opal-rm-db -c "select * from rm_connectivity_probe;"
```

## Manual auth testing with Bruno

RM's `testing-support` auth endpoint is easiest to prove locally by running
`opal-user-service` first, using it to obtain a real AAD-backed token, and then
calling RM with that token.

1. Start local RM dependencies and run RM:

```bash
docker compose up -d opal-rm-db
./gradlew run
```

2. In `/Users/TomReed/opal/opal-user-service`, start `opal-user-service` locally and make sure it is reachable on `http://localhost:4555`

3. Install Bruno if needed:

```bash
brew install --cask bruno
```

4. Create a local Bruno environment in this repo:

```bash
cp bruno/environments/env.bru.template bruno/environments/local.bru
```

5. Open `/Users/TomReed/opal/opal-rm-service/bruno` as a Bruno collection

6. Run `User Service/Get test user token`, then copy the returned `access_token`
   into the `BEARER_TOKEN` secret in your Bruno environment

7. Run `RM/auth-check` and confirm RM returns an authenticated summary with
   resolved user-state details

8. You can also run `health/health` to confirm RM itself is reachable before
   testing the authenticated path

## Running with Docker

Build the application JAR first:

```bash
./gradlew build
```

Start RM service and RM Postgres together:

```bash
docker compose up --build
```

Check health:

```bash
curl http://localhost:4556/health
```

Stop the containers:

```bash
docker compose down
```

If you only want the database in Docker and prefer to run the app on the host:

```bash
docker compose up -d opal-rm-db
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
