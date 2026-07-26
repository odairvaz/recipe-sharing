# Recipe Sharing Platform

A full-stack web application built with Java and Spring Boot that allows users to share, discover, review, and save their favorite recipes.

-----

## Features

  * **User Authentication:** Secure user registration and login functionality using Spring Security.
  * **Recipe Management:** Authenticated users can create, view, and manage their recipes.
  * **Search Functionality:** Users can search for recipes by keywords found in the title, description, or ingredients.
  * **Categorization:** Recipes are organized by categories (e.g., Dessert, Main Dish) using an Enum for type safety.
  * **Reviews & Ratings:** Logged-in users can add reviews and ratings to recipes. Owners can delete their own reviews.
  * **Favorites System:** Users can save their favorite recipes and view them on a dedicated "My Favorites" page.

-----

## Technologies Used

  * **Backend:** Java, Spring Boot, Spring Security, Spring Data JPA (Hibernate)
  * **Frontend:** Thymeleaf, Bootstrap 5, Font Awesome
  * **Database:** PostgreSQL (for production)
  * **Build Tool:** Maven
  * **Utilities:** Lombok

-----

## Getting Started

### Prerequisites
- [Docker](https://www.docker.com/) and Docker Compose
- Java 17+ and Maven (only if running the app outside Docker)

### Running with Docker
```bash
# 1. Clone the repository
git clone https://github.com/odairvaz/recipe-sharing.git
cd recipe-sharing

# 2. Start the application and database
docker compose up

# 3. Open the app
# http://localhost:8080
```

The database schema is created automatically on first startup.
