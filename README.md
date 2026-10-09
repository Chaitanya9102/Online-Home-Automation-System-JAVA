# Online Home Automation Control Platform

**Haven** is a Java Swing application for managing a simulated smart home. Homeowners can control devices, create automation routines, and check home conditions. Administrators can review devices, manage accounts, and configure system settings.

The app simulates device commands and environment readings. It does not connect to physical home devices.

## Features

- Homeowner and administrator sign-in
- Homeowner account creation
- Profile editing and password updates
- Device status, room filters, security status, and temperature readings
- Simulated device controls, including on/off, light brightness, and thermostat adjustments
- Automation routines with conditions, actions, and enable/disable controls
- Administrator tools for account and role management, device compatibility, and system settings
- Background environment monitoring
- SQLite database support through JDBC

Passwords are stored as salted PBKDF2 hashes.

## Requirements

- JDK 21 or later
- For SQLite storage and the packaged JAR: Maven 3.9 or later
- Internet access the first time Maven downloads the project dependencies

## Run the demo

Open PowerShell in the project folder and run:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\run-demo.ps1
```

The script compiles and starts the app with sample data. Changes made in demo mode are cleared when the app closes. The execution-policy change applies only to the current PowerShell window.

### Demo accounts

| Role | Email | Password |
|---|---|---|
| Homeowner | `alex@example.com` | `Home123!` |
| Administrator | `admin@haven.local` | `Admin123!` |

You can also create a homeowner account from the sign-in screen. New accounts cannot be assigned the administrator role through public registration.

## Using the application

1. Sign in with a demo account or create a homeowner account.
2. On the homeowner **Overview** page, check the temperature, security status, and connected devices. Use the device controls to switch supported devices on or off, or adjust lights and thermostats.
3. Open **Automations** to create routines by choosing conditions and actions. You can enable or disable each routine.
4. Open **My profile** to edit your account details or update your password.
5. Sign in as the administrator and open **Admin console** to manage accounts, review device compatibility, update system settings, and monitor the platform.

The app displays messages when an action succeeds or needs attention.

## Run with SQLite storage

From the project folder, build the runnable JAR:

```powershell
mvn clean package
```

Set the database location and run the JAR:

```powershell
$env:HOME_AUTOMATION_DB = 'jdbc:sqlite:home-automation.db'
java -jar target/home-control-platform-1.0.0.jar
```

On its first launch, the app creates the database tables and sample records. User accounts, devices, automation routines, and system settings are saved in SQLite. Environment readings remain simulated for the current session.

To return to demo mode, close the app and open a new PowerShell window without setting `HOME_AUTOMATION_DB`.

## Java concepts demonstrated

- **Object-oriented programming:** `SmartDevice` inheritance, concrete device classes, the `Switchable` interface, polymorphism, and exception handling
- **Collections and generics:** typed lists, repository collections, and a thread-safe listener collection
- **Multithreading and synchronization:** a background environment monitor and synchronized device and repository operations
- **Database operations and JDBC:** `HomeRepository`, `InMemoryHomeRepository`, and `JdbcHomeRepository`

This project is a desktop application. It does not use Servlets or provide a web interface.

## Screenshots

The screenshots show the app using its sample data. The README copies are in `screenshots/readme/`; some have been cropped to remove a strip above the app window and given a narrow border. The original screenshots are also in `screenshots/`.

### Sign-in

<img src="screenshots/readme/sign-in.jpg" alt="Haven sign-in screen with homeowner account creation option" width="900">

### Homeowner dashboard

**Overview and device controls**

<img src="screenshots/readme/homeowner-overview.jpg" alt="Homeowner overview showing environment status and device controls" width="900">

**Automation routines**

<img src="screenshots/readme/automations.jpg" alt="Homeowner automation routines with conditions and actions" width="900">

**Profile and password update**

<img src="screenshots/readme/homeowner-profile.jpg" alt="Homeowner profile and password update fields" width="900">

### Administrator dashboard

**System overview**

<img src="screenshots/readme/admin-overview.jpg" alt="Administrator overview showing home environment and connected devices" width="900">

**Device compatibility review**

<img src="screenshots/readme/admin-device-compatibility.jpg" alt="Administrator device compatibility approval screen" width="900">

**Administrator profile**

<img src="screenshots/readme/admin-profile.jpg" alt="Administrator profile and password update fields" width="900">

The screenshots do not include the administrator user-management or system-settings panels.

## Project structure

```text
src/main/java/edu/homeautomation/
  Main.java
  model/                  Devices, users, routines, and environment readings
  persistence/            Repository interface and demo/JDBC implementations
  service/                 Device control, environment monitor, password hashing
  ui/                      Sign-in, registration, and role dashboards
src/main/resources/schema.sql
```
