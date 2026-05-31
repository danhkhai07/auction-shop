# Auction Shop

Auction Shop is a real-time auction platform built with a reactive Java backend and JavaFX desktop client. The system enables users to register, manage auction listings, place bids, and track transactions in real-time through a modern, responsive interface.

## Overview

Auction Shop consists of two main components: a Spring Boot WebFlux REST API backend that handles all business logic and persistence, and a JavaFX desktop application that provides users with an intuitive interface to interact with the auction system. The backend uses reactive programming with R2DBC for non-blocking database access to PostgreSQL, while authentication is secured using JWT tokens and BCrypt password hashing.

## Technologies & Requirements

The project is built on Java 17 with Spring Boot 3.2, utilizing Spring WebFlux for reactive endpoints and R2DBC for asynchronous database communication. The frontend is developed using JavaFX 25 with Jackson for JSON serialization. PostgreSQL serves as the persistent data store, managed conveniently through Docker Compose.

Additional dependencies include JWT support via io.jsonwebtoken, Lombok for reducing boilerplate code, and Spring Security for cryptographic utilities.

To run this project, you need Java Development Kit 17 or higher, Maven 3.6 or later, and Docker with docker-compose for database provisioning. The application has been tested on both Linux and macOS environments.

## Project Structure

The repository follows a Maven multi-module layout with clear separation of concerns:

```
auction-shop/
├── backend/
│   ├── src/main/java/com/shop/
│   │   ├── application/        # API endpoints and handlers
│   │   ├── domain/             # Core entities (User, Auction, Item, BidTransaction)
│   │   ├── dto/                # Data transfer objects
│   │   ├── db/                 # Database repositories
│   │   ├── cache/              # Caching layer
│   │   ├── config/             # Configuration classes
│   │   ├── security/           # Authentication and authorization
│   │   ├── filter/             # HTTP filters
│   │   ├── infra/              # Infrastructure utilities
│   │   └── Main.java           # Application entry point
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── pom.xml
├── frontend/
│   ├── src/main/java/com/frontendauction/
│   │   ├── controller/         # UI controllers (Login, Signup, Dashboard, LiveAuction)
│   │   ├── model/              # Data models
│   │   ├── service/            # Business logic and API communication
│   │   └── AuctionShopApplication.java
│   ├── src/main/resources/com/frontendauction/
│   │   ├── login.fxml
│   │   ├── signup.fxml
│   │   ├── dashboard.fxml
│   │   ├── live-auction.fxml
│   │   └── product-management.fxml
│   └── pom.xml
├── .env                        # Environment variables
└── pom.xml                     # Parent multi-module build file
```

## Getting Started

### Prerequisites

Ensure you have the following installed:

On Linux:

```sh
sudo apt-get install openjdk-17-jdk maven docker.io docker-compose
sudo usermod -aG docker $USER
newgrp docker
```

On macOS:

```sh
brew install openjdk@17 maven docker
# Start Docker Desktop from Applications
```

### Running the Backend Server

Navigate to the backend directory and choose one of these approaches:

For development with live reload:

```sh
cd backend
mvn spring-boot:run
```

To build and run as a standalone JAR:

```sh
cd backend
mvn -DskipTests package
java -jar target/auction-shop-1.0.0.jar
```

To run with Docker (includes PostgreSQL):

```sh
cd backend
docker compose up --build
```

The server will start on `http://localhost:1234` (configurable via the APP_PORT environment variable).

### Running the Frontend Client

Once the backend is running, start the JavaFX client in a separate terminal:

```sh
cd frontend
mvn javafx:run
```

The client will launch with the login screen. You can create a new account or sign in with existing credentials to access the auction features.

## Configuration

Environment variables are defined in the `.env` file at the project root:

```
POSTGRES_DB=fake_db
POSTGRES_PORT=5432
POSTGRES_USER=postgres
POSTGRES_PASSWORD=fake_password
JWT_SECRET=fake_secret
APP_PORT=1234
```

Update these values as needed for your environment. If using Docker, these variables are automatically passed to the containers.

## Features Implemented

The Auction Shop platform provides the following functionality:

**User Management & Security**

User registration and authentication with secure password hashing using BCrypt. JWT-based session management ensures stateless, scalable authentication across requests. Role-based access control with configurable permissions allows for flexible user management.

**Product & Auction Management**

Users can create and manage auction listings for items. Each auction has configurable status tracking (pending, active, closed, etc.). The system maintains complete audit trails through database transactions.

**Real-Time Bidding**

Participants can place bids on active auctions with automatic validation of bid amounts. The system records all bid transactions for transparency and dispute resolution.

**Reactive Architecture**

The backend uses Spring WebFlux for non-blocking I/O operations, handling multiple concurrent users efficiently. R2DBC provides async database access without blocking threads, ensuring responsive performance.

**Desktop Client Interface**

JavaFX provides a native desktop experience with responsive UI components. The client features dedicated screens for login, signup, dashboard overview, live auction participation, and product management.

## API Documentation

The backend provides the following REST API endpoints:

**Authentication**
- `POST /auth/register` - Create a new user account
- `POST /auth/login` - Obtain JWT token
- `POST /auth/me` - User profile infos

**Auctions**
- `GET /auctions` - List all auctions with pagination
- `POST /auctions` - Create new auction
- `GET /auctions/{id}` - Get auction details
- `POST /auctions/delete/{id}` - Cancel auction (seller only)

**Bids**
- `POST /auctions/{id}/bids` - Place a bid

**Items**
- `GET /items` - List items
- `POST /items` - Create new item
- `POST /items/delete/{id}` - Delete item

**And more, all included as features in the JavaFX client.**

## Report & Demo

For detailed documentation and feature demonstration, refer to the following resources:

- **Project Report (PDF) & Video Demo**: [auction-shop-report](https://drive.google.com/drive/folders/1iTyB82dVeNVgNrrzJ3uyFQbxkCa8HXAY?usp=sharing)

## Troubleshooting

If the frontend cannot connect to the backend, verify that the server is running on the configured port and that the network is accessible. Check the `.env` file to ensure `APP_PORT` matches the server configuration.

For Docker-related issues on macOS, ensure Docker Desktop is running. On Linux, confirm your user is in the docker group: `groups $USER`.

If you encounter Java version conflicts, ensure you have JDK 17+ installed: `java -version`.

## License

This project is developed for educational purposes as part of a coursework assignment.
