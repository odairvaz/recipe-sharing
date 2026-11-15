# Recipe Sharing Platform

A full-stack web application built with Java and Spring Boot that allows users to share, discover, review, and save their favorite recipes.

-----

## Features

  * **User Authentication:** Registration with email confirmation and login using Spring Security. Accounts must be activated through the emailed link before they can sign in.
  * **Recipe Management:** Authenticated users can create recipes with an optional image.
  * **Search Functionality:** Users can search for recipes by keywords found in the title, description, or ingredients.
  * **Categorization:** Recipes are organized by categories (e.g., Dessert, Main Dish) using an Enum for type safety.
  * **Reviews & Ratings:** Logged-in users can add one review and rating per recipe. Owners can delete their own reviews.
  * **Favorites System:** Users can save their favorite recipes and view them on a dedicated "My Favorites" page.
  * **Caching:** Recipe details, listings and search results are cached in Redis.

-----

## Technologies Used

  * **Backend:** Java 17, Spring Boot 3, Spring Security, Spring Data JPA (Hibernate), Flyway
  * **Frontend:** Thymeleaf, Bootstrap 5, Font Awesome
  * **Database:** PostgreSQL
  * **Cache:** Redis
  * **Build Tool:** Maven
  * **Testing:** JUnit 5, Mockito, Spring MockMvc, Testcontainers
  * **Utilities:** Lombok, Passay

-----

## Running locally

### Prerequisites

  * Java 17
  * Docker (for PostgreSQL, Redis and Mailpit, and for the integration tests)

### 1. Start the backing services

```bash
docker compose up -d postgres redis mailpit
```

This starts PostgreSQL on `5432`, Redis on `6379` and [Mailpit](https://mailpit.axllent.org/) on `1025` (SMTP).
Confirmation emails can be read in Mailpit's web UI at <http://localhost:8025>.

### 2. Run the application with the `dev` profile

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The app is available at <http://localhost:8081>. The `dev` profile (`application-dev.properties`) provides defaults
that match `docker-compose.yml`, stores uploads in `./uploads` and logs in `./logs`.

### Alternative: run everything in Docker

```bash
docker compose --profile app up --build
```

-----

## Configuration

Outside the `dev` profile, the application expects these environment variables (or system properties):

| Variable | Description |
| --- | --- |
| `datasourceUrl`, `datasourceUsername`, `datasourcePassword` | PostgreSQL connection |
| `mailHost`, `mailPort`, `mailUsername`, `mailPassword` | SMTP server used for confirmation emails (STARTTLS) |
| `storageBaseLocation` | Directory where avatars and recipe images are stored |
| `logDir` | Directory for log files |
| `redisHost`, `redisPort` | Redis connection (default `localhost:6379`) |

The database schema is managed by Flyway (`src/main/resources/db/migration`). Hibernate only validates it
(`ddl-auto=validate`), so schema changes must be added as new versioned migrations (`V5__...sql`).

-----

## Tests

```bash
./mvnw verify
```

Unit and web-layer tests always run. Integration tests start PostgreSQL and Redis with Testcontainers and are
skipped automatically when Docker is not available. CI runs the full suite on every pull request.
