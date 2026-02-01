# BookNet API

BookNet is a Spring Boot API for a large-scale, multi-structured database project.
It exposes REST endpoints to manage books, reviews, users, authors, genres, notifications, and sources 
while integrating MongoDB (document) and Neo4j (graph).

---

## Features

- RESTful API for BookNet entities (books, reviews, users, authors, genres, notifications, sources)
- JWT-based authentication (RSA keys)
- Data import endpoints (NDJSON and CSV)
- MongoDB + Neo4j
- Migration endpoints from MongoDB to Neo4j
- Prometheus metrics + Grafana dashboards (optional)

---

## Tech Stack

- Java 21
- Spring Boot 4.x
- MongoDB (document DB, sharded + replica set supported)
- Neo4j (graph DB)
- Springdoc OpenAPI (Swagger UI)
- Prometheus + Grafana (metrics)

---

## Repository Layout

- `src/main/java/it/unipi/booknetapi` – main Spring Boot application
- `src/main/resources/application.properties` – default configuration
- `docker-compose-databases.yml` – MongoDB/Neo4j
- `docker-compose-api.yml` – API container
- `docker-compose-metrics.yml` – Prometheus + Grafana
- `keys/` – RSA keys for JWT
- `mongodb.yaml`, `neo4j.yaml`, `redis.yaml` – Kubernetes manifests

---

## Prerequisites

- JDK 21
- Maven (or `./mvnw`)
- Docker Desktop (for local containers)
- Optional: Kubernetes enabled in Docker Desktop

---

## Configuration

Default properties live in `src/main/resources/application.properties`.
You can override them via environment variables:

```
APP_MONGO_URI
APP_MONGO_DATABASE
APP_NEO4J_URI
APP_NEO4J_USER
APP_NEO4J_PASSWORD
APP_REDIS_HOST
APP_REDIS_PORT
APP_PRIVATE_KEY_PATH
APP_PUBLIC_KEY_PATH
```

JWT keys are expected in `keys/` by default:
- `keys/private_key_pkcs8.pem`
- `keys/public_key.pem`

---

## Run Locally (without Docker)

1. Make sure MongoDB, Neo4j, Redis are running.
2. Update `application.properties` with your connection strings.
3. Start the app:

```
./mvnw spring-boot:run
```

---

## Run with Docker (Local Compose)

Create the network once:

```
./docker-network-create.sh
```

Start databases:

```
docker compose -f docker-compose-databases.yml up -d
```

Start the API:

```
docker compose -f docker-compose-api.yml up --build
```

The API will be available at:
`http://localhost:8080`

---

## API Docs (Swagger)

Once running, open:

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`

---

## Core Endpoints (high level)

- `/auth` – register, login, refresh, token info
- `/book` – books, import, migrate, analytics
- `/review` – reviews, import, migrate
- `/user` – users, lists, migrate, stats
- `/author`, `/genre`, `/notification`, `/source`

---

## Data Import

Admin-only endpoints for NDJSON files:

- `POST /book/upload/{idSource}`
- `POST /book/upload/similarity/{idSource}`
- `POST /book/upload/genre/{idSource}`
- `POST /author/upload/{idSource}`
- `POST /genre/upload/{idSource}`
- `POST /review/upload/{idSource}`

---

## Migration Endpoints

Admin-only endpoints to sync MongoDB → Neo4j:

- `/book/migrate`
- `/review/migrate`
- `/author/migrate`
- `/genre/migrate`
- `/user/migrate`
- `/user/migrate/reader`
- `/user/migrate/reviewer`

---

## Metrics (Prometheus / Grafana)

Start metrics stack:

```
docker compose -f docker-compose-metrics.yml up -d
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin / admin)

Prometheus scrapes:
- `http://booknet-api:8080/actuator/prometheus`

---

## Kubernetes (Docker Desktop)

See `DATABASES.md` for full steps.
Quick summary:

```
kubectl apply -f secrets.yaml
kubectl apply -f mongodb.yaml
kubectl apply -f neo4j.yaml
kubectl apply -f redis.yaml
```

Then initialize MongoDB replica set:

```
kubectl exec -it mongodb-0 -- mongosh --eval "rs.initiate({_id: 'rs0', members: [{_id: 0, host: 'mongodb-0.mongo:27017'}]})"
```

---

## Build

```
./mvnw clean package
```

---

## Notes

- This project is part of the University of Pisa “Large-Scale and Multi-Structured Databases” coursework (AY 2025–2026).
- Focus is on distributed data modeling and multi-database consistency.
