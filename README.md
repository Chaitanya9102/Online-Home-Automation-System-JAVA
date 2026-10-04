# Online Home Automation Control Platform

A Java Swing project for controlling and monitoring a smart home. It demonstrates object-oriented design, collections and generics, synchronized device state, a background environment monitor, and JDBC database classes.

## Features

- Homeowner dashboard with room filters, device controls, temperature and security indicators.
- Automation rules that can be added, enabled, and disabled.
- Admin dashboard for reviewing compatible devices and managing demo users.
- Environment readings updated by a background thread.
- A repository interface with both an in-memory demo implementation and a JDBC SQLite implementation.
- Custom validation exception and polymorphic smart-device hierarchy.

## Requirements

- JDK 21 or later.
- Maven 3.9 or later for the packaged application and SQLite JDBC driver.

## Run

For a quick demo on a computer with only the JDK installed, open PowerShell in this folder and run:

```powershell
.\run-demo.ps1
```

The script compiles the app and starts it with in-memory demo data.

To build a packaged app with SQLite support, use Maven:

From this folder:

```text
mvn clean package
java -jar target/home-control-platform-1.0.0.jar
```

The app opens in demo mode by default. To use SQLite persistence, set the environment variable `HOME_AUTOMATION_DB` to a SQLite JDBC URL before starting. For example:

```text
HOME_AUTOMATION_DB=jdbc:sqlite:home-automation.db
```

On Windows PowerShell:

```powershell
$env:HOME_AUTOMATION_DB = 'jdbc:sqlite:home-automation.db'
java -jar target/home-control-platform-1.0.0.jar
```

If SQLite is configured but the driver cannot be loaded, the app reports the database error and continues in demo mode. Database tables are initialized automatically from `src/main/resources/schema.sql`.

## Demo access

Choose **Homeowner** or **Administrator** from the role selector on the sign-in screen. No password is required for this classroom demo. This is a user-interface role switch, not production authentication.

## Project structure

```text
src/main/java/edu/homeautomation/
  Main.java
  model/                  Device inheritance, rules, users and readings
  persistence/            Repository interface, demo store and JDBC DAO
  service/                Device and environment services
  ui/                     Swing sign-in screen and dashboards
src/main/resources/schema.sql
```

## Extending the project

- Replace the role selector with password-based authentication and hashed passwords.
- Add CRUD forms for editing users and automation conditions.
- Connect real hardware through a device adapter instead of the simulated command service.
- Add logging, validation, and role-based authorization before deploying beyond a classroom demo.
