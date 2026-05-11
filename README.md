# BoostUp — Startup Investment Platform (Java Backend)

> BoostUp is a web application developed as part of the **PIDEV 3A** coursework at **Esprit School of Engineering** (2024–2025). It bridges the gap between entrepreneurs and investors, enabling startups to showcase their projects and receive funding through a seamless digital platform.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Directory Structure](#directory-structure)
- [Installation](#installation)
- [Usage](#usage)
- [Contributions](#contributions)
- [Acknowledgments](#acknowledgments)
- [Licence](#licence)

---

## Overview

**BoostUp** addresses a real-world challenge in the entrepreneurship ecosystem: connecting startups with the right investors. The platform allows:

- 🚀 **Startups** to post their projects, funding needs, and business vision
- 💼 **Investors** to discover, evaluate, and financially support promising ventures
- 💰 **Financial Management** of investment transactions and funding flows between both parties

This repository contains the **backend layer** of the application, built with **Java (Spring Boot)**, exposing REST APIs consumed by the Symfony frontend.

---

## Features

- 🔐 JWT-based authentication and role management (Startup / Investor)
- 📋 RESTful API for startup project management
- 💳 Financial management module — investment transaction processing and funding flow logic
- 📊 Portfolio and analytics endpoints for investor dashboards
- 🔎 Advanced filtering and search API for startups
- 📬 Notification service for funding events
- 🔒 Secure data validation and error handling

---

## Tech Stack

### Backend
- **Java 17** — core programming language
- **Spring Boot 3** — REST API framework
- **Spring Security** — authentication and authorization
- **Spring Data JPA / Hibernate** — ORM and database access
- **Maven** — dependency management

### Frontend
- **Symfony 6** — PHP frontend framework
- See the [Symfony repository](https://github.com/GoldenBOY591/boostup) for frontend setup

### Other Tools
- **MySQL** — relational database
- **Postman** — API testing
- **Git & GitHub** — version control
- **IntelliJ IDEA** — development environment

---

## Directory Structure

```
boostup-java/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/boostup/
│       │       ├── controller/     # REST API endpoints
│       │       ├── service/        # Business logic
│       │       ├── repository/     # JPA repositories
│       │       ├── entity/         # JPA entities
│       │       ├── dto/            # Data Transfer Objects
│       │       └── security/       # JWT & auth config
│       └── resources/
│           └── application.properties
└── pom.xml                         # Maven dependencies
```

---

## Installation

1. **Clone the repository:**
```bash
git clone https://github.com/GoldenBOY591/boostup.git
cd boostup
```

2. **Configure the database in `application.properties`:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/boostup
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

3. **Build the project with Maven:**
```bash
mvn clean install
```

4. **Run the Spring Boot application:**
```bash
mvn spring-boot:run
```

5. **The API will be available at:**
```
http://localhost:8080
```

---

## Usage

### API Endpoints (examples)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Authenticate user |
| GET | `/api/startups` | List all startups |
| POST | `/api/investments` | Create an investment |
| GET | `/api/investments/{id}` | Get investment details |
| PUT | `/api/investments/{id}` | Update investment status |

### Testing the API
Use **Postman** or any REST client to interact with the API endpoints.  
Import the collection from `/docs/postman-collection.json` if available.

---

## Contributions

This project was developed by a team of 3rd-year engineering students at **Esprit School of Engineering** as part of the PIDEV integrated project module.

### Contributors
- [rabie ](https://github.com/Rabiejl55) — User Management Module (Full-stack: symfony +java)
- [Rayen Amri](https://github.com/GoldenBOY591)* — Events Management Module (Full-stack: symfony +java)
- [emna ](https://github.com/Em10na) — Accompagnement Management Module (Full-stack: symfony +java)
- [feriel ](https://github.com/faryoula11) — Financial Management Module (Full-stack: Symfony + Java)
- [arij ](https://github.com/arijbensalem) — Candidatures Management Module (Full-stack: symfony +java)
---

## Acknowledgments

This project was completed under the guidance of our tutor at **Esprit School of Engineering**.  
Special thanks to the PIDEV teaching team for their support throughout the 2024–2025 academic year.

---

## Licence

This project is licensed under the MIT License.  
It was created strictly for educational purposes as part of the PIDEV coursework at **Esprit School of Engineering**.
