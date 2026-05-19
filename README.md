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

Inspect the bootstrap table:

```bash
docker exec opal-rm-db psql -U opal-rm -d opal-rm-db -c "select * from rm_connectivity_probe;"
```

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
