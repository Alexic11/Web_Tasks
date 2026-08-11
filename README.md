# Web Tasks

[![Build and Test](https://github.com/Alexic11/Web_Tasks/actions/workflows/build.yml/badge.svg)](https://github.com/Alexic11/Web_Tasks/actions/workflows/build.yml)

A full-stack task management application built with **Java, Spring Boot, Vaadin, Spring Security, and MySQL**.

Web Tasks provides a Kanban-style workspace for organizing boards, lists, and tasks while supporting role-based collaboration, task assignments, comments, attachments, activity tracking, and administrative features.

The application was designed with a focus on **backend business logic, security, database-driven workflows, and role-based access control**.

---

## 🚀 Key Features

- 🔐 User authentication and authorization
- 👥 Role-based access control
- 📋 Board creation and management
- 👤 Board member management
- 📝 Kanban-style lists and task cards
- 🎯 Task assignment to users
- ⚡ Task priorities and due dates
- 🔄 Drag-and-drop task management
- 🔎 Task filtering and search
- 💬 Task comments
- 📎 File attachments
- 📜 Activity tracking and audit history
- 🗄️ Board archiving
- 👨‍💼 Administrative user management
- 🔒 Permission checks for board and task operations

---

## 🛠️ Technology Stack

### Backend

- **Java 21**
- **Spring Boot 3**
- **Spring Security**
- **Spring Data JPA**
- **Hibernate**
- **Maven**

### Frontend

- **Vaadin Flow**
- **HTML / CSS**

### Database

- **MySQL**
- **JPA / Hibernate ORM**

### Development Tools

- **IntelliJ IDEA**
- **MySQL Workbench**
- **Git & GitHub**
- **Maven**

---

## 🏗️ Application Architecture

Web Tasks follows a layered application architecture:

```text
┌─────────────────────────────┐
│       Vaadin UI Layer       │
│     Views / Components      │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│        Service Layer        │
│ Business Logic & Security   │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│      Repository Layer       │
│      Spring Data JPA        │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│        MySQL Database       │
└─────────────────────────────┘
```

The application uses **Vaadin Flow's server-side UI model**, with application logic handled by Spring services and persistent data managed through Spring Data JPA and Hibernate.

---

## 📸 Screenshots

### Dashboard

Overview of boards, task statistics, and important project information.

![Dashboard](docs/screenshots/dashboard.png)

---

### Boards Overview

Overview of available boards and quick access to project workspaces.

![Boards View](docs/screenshots/boards-view.png)

---

### Kanban Board

Tasks are organized into lists and managed through an interactive Kanban-style interface.

![Board View](docs/screenshots/board-view.png)

---

### Task Details

Tasks support detailed descriptions, priorities, assignees, due dates, comments, attachments, and activity tracking.

![Task Details](docs/screenshots/task-details.png)

---

### My Tasks

Personal task overview showing tasks assigned to the currently logged-in user.

![My Tasks](docs/screenshots/my-tasks.png)

---

### Board Members & Roles

Board owners and administrators can manage members and board-level permissions.

![Board Members](docs/screenshots/board-members.png)

---

### Administration

Administrative functionality for managing application users.

![Admin View](docs/screenshots/admin-view.png)

---

## ⚙️ Installation & Setup

### Prerequisites

Before running the application, make sure you have the following installed:

- **Java 21**
- **Maven**
- **MySQL 8**
- **Git**

### 1. Clone the Repository

```bash
git clone https://github.com/Alexic11/Web_Tasks.git
cd Web_Tasks
```

### 2. Initialize the Database

The project includes SQL scripts for creating the database structure and optional demo data.

Run the scripts in the following order:

```text
database/schema.sql
database/demo-data.sql
```

The first script creates the `task_app` database and all required tables.

The second script inserts demo users, a sample board, Kanban lists, tasks, labels, comments, checklist items, activity history, and notifications.

Demo credentials:

```text
Administrator
Email: admin@local
Password: admin123
```

```text
Board Owner
Email: owner@example.com
Password: demo123
```

The demo data is intended only for local development and portfolio demonstration.

### 3. Configure Database Connection

The application uses environment variables for database credentials.

The following variables are supported:

| Variable | Description | Default |
| --- | --- | --- |
| `DB_URL` | MySQL connection URL | `jdbc:mysql://localhost:3306/task_app` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | No default password |

For example:

```text
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
```

When running the application from IntelliJ IDEA, these values can be configured under:

```text
Run → Edit Configurations → Environment variables
```

Example:

```text
DB_USERNAME=root;DB_PASSWORD=your_mysql_password
```

> Database passwords and other sensitive credentials should never be committed to the repository.

### 4. Build the Application

Using the Maven Wrapper:

```bash
./mvnw clean install
```

On Windows Command Prompt:

```cmd
mvnw.cmd clean install
```

### 5. Run the Application

Using Maven:

```bash
./mvnw spring-boot:run
```

Alternatively, run the following class directly from IntelliJ IDEA:

```text
carobnifrulas.web_tasks.WebTasksApplication
```

### 6. Open the Application

Once the application has started successfully, open:

```text
http://localhost:9800
```

in your web browser.

---

## 🔐 Security Configuration

Database credentials are configured through environment variables instead of being hardcoded in the source code.

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/task_app?useUnicode=true&characterEncoding=utf8&serverTimezone=Europe/Sarajevo}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
```

This keeps environment-specific credentials outside the Git repository.

---

## 🧪 Testing

The project includes unit tests for core business logic using **JUnit 5** and **Mockito**.

Currently covered areas include:

- User creation with temporary passwords
- Duplicate user validation
- Board creation
- Automatic OWNER assignment
- Default board list initialization

Run the test suite with:

```bash
./mvnw test
```

The project also uses **GitHub Actions** to automatically build and test the application on every push and pull request to the `main` branch.

The current CI workflow runs the test suite using **Java 21** and Maven.

---

## 📁 Project Structure

```text
Web_Tasks/
├── .github/
│   └── workflows/
│       └── build.yml
│
├── database/
│   ├── schema.sql
│   └── demo-data.sql
│
├── docs/
│   └── screenshots/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── carobnifrulas/
│   │   │       └── web_tasks/
│   │   └── resources/
│   │
│   └── test/
│       └── java/
│           └── carobnifrulas/
│               └── web_tasks/
│
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

The application code is organized into domain-focused packages for boards, cards, lists, users, security, notifications, and UI views.

The project follows a layered architecture where UI components delegate business operations to services, while persistence is handled through Spring Data JPA repositories.

---

## 🔄 Continuous Integration

The repository includes a **GitHub Actions CI pipeline**.

For every push or pull request targeting the `main` branch, GitHub Actions automatically:

1. Checks out the repository
2. Configures Java 21
3. Configures the Maven environment
4. Builds the project
5. Executes the automated test suite

The current build status is displayed at the top of this README.

---

## 🔭 Future Improvements

Potential future improvements include:

- Additional unit and integration test coverage
- Docker-based local development environment
- Database migrations with Flyway
- REST API support for external integrations
- Expanded notification features
- Additional automated UI testing
- Cloud deployment and production configuration

---

## 📄 Project Purpose

Web Tasks was developed as a full-stack application demonstrating the implementation of a modern task management system using the Java ecosystem.

The project demonstrates practical experience with:

- Java and object-oriented application design
- Spring Boot application architecture
- Spring Security and authorization
- Vaadin Flow server-side UI development
- Relational database design with MySQL
- JPA and Hibernate persistence
- Role-based business rules
- Unit testing with JUnit and Mockito
- Git-based development workflow
- Continuous integration with GitHub Actions

---
