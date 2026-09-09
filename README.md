# BloodLink - Professional Blood Bank Management System

**BloodLink** is an Object-Oriented Programming project built in **Java 21 + Swing**, with an optional **MySQL/JDBC** persistence layer. It is designed as a real operations dashboard rather than a basic CRUD assignment.

**Version 2.0** adds the three features that move the project from "CRUD with a nice dashboard" to something that models how a blood bank actually works: bag level inventory with expiry, TTI screening, and a full audit trail.

## What is new in v2.0

### 1. Bag level inventory with expiry and FEFO issuing

Previously, inventory was one integer per blood group. Now every physical unit is a `BloodBag` with its own barcode, donor, collection date, component and expiry date.

- **Component driven shelf life.** Whole Blood 35 days, Packed Red Cells 42, Platelets 5, Plasma 365. Expiry is derived at collection, never typed in.
- **FEFO issuing.** First expired, first out. The bag closest to expiry is always chosen first, which is what actually reduces wastage.
- **Reservation on approval.** Approving a request now holds real bags. Two approved requests can no longer claim the same unit, and rejecting an approved request returns its bags to the shelf.
- **Expiry sweep.** Runs at startup and on demand, moving anything past its date to `EXPIRED`.
- **Wastage reporting.** Expired plus discarded, as a percentage of all resolved bags.

Bag lifecycle:

```text
QUARANTINED ──all markers non-reactive──> AVAILABLE ──approve──> RESERVED ──complete──> ISSUED
     │                                        │                      │
     └──any marker reactive──> DISCARDED      └──shelf life ends──> EXPIRED
                                                                     │
                                              RESERVED ──reject──> AVAILABLE
```

### 2. TTI screening workflow

Nothing donated is issuable until it has been tested. A new collection enters `QUARANTINED` and the screening panel is the only route out.

- Five markers per bag: **HIV, HBV, HCV, Syphilis, Malaria**.
- All five non-reactive, the bag is released to issuable stock.
- Any marker reactive, the bag is discarded and the donor is automatically deferred and flagged unavailable.
- The rule is one way. Once discarded for a reactive result, a bag can never be released, no matter what is recorded afterwards.

### 3. Audit trail

An append only record of who did what, to which record, and when.

- Covers sign in (successful and failed), donor changes, collections, screening results, quarantine release, reservations, issues, discards, expiries, request decisions and appointments.
- `AuditRepository` deliberately has no `update` or `delete` method. An audit log that can be edited is not an audit log.
- Filterable by category and searchable by actor, action, record or detail.
- Donor writes are audited through `AuditedDonorRepository`, a **decorator** that wraps any `DonorRepository` without the storage class or the service knowing auditing exists.

## Core modules

- Secure role-based login with PBKDF2 password hashing
- Professional operations dashboard with live KPIs
- Donor registration, search and deletion
- Bag level blood inventory across all 8 blood groups and 4 components
- TTI screening and quarantine management
- Emergency-first blood request workflow with real reservations
- Blood compatibility intelligence
- Donation appointment scheduling
- Operational reports including shelf life and wastage
- Immutable audit trail
- MySQL repository layer with automatic schema initialization
- Demo/in-memory repositories so the application runs without a database

## OOP concepts demonstrated

- **Abstraction:** `User` is an abstract base class.
- **Inheritance:** `Admin`, `Staff`, `Donor`, and `Patient` derive from `User`.
- **Polymorphism:** each user type implements `dashboardTitle()`.
- **Encapsulation:** model fields are private and exposed through controlled methods.
- **Interfaces:** repository contracts (`DonorRepository`, `BloodBagRepository`, `AuditRepository`, etc.).
- **Dependency inversion:** services depend on interfaces, not concrete storage classes.
- **Decorator pattern:** `AuditedDonorRepository` adds auditing around any `DonorRepository`.
- **Composition:** `RequestService` coordinates request and inventory services; `ScreeningService` coordinates bags and donors.
- **Enums with behaviour:** `BloodComponent` carries shelf life, `BagStatus` answers `isOnShelf()` and `isWasted()`, `BloodGroup` computes compatibility.
- **State machine:** bag lifecycle and request workflow are both enforced at the service layer.
- **Exception handling and validation:** invalid transitions are blocked and reported to the user.

## Architecture

```text
UI (Swing)
   ↓
Service Layer        AuthService, DonorService, InventoryService,
   ↓                 ScreeningService, RequestService, AppointmentService, AuditService
Repository Interfaces
   ↓
┌───────────────────┬────────────────────┐
│ In-memory storage │ MySQL/JDBC storage │
└───────────────────┴────────────────────┘
```

## Folder structure

```text
src/main/java/com/bloodlink
├── Main.java
├── AppContext.java          composition root, all wiring lives here
├── db/
├── model/                   User hierarchy, BloodBag, AuditEvent, enums
├── repository/
│   ├── memory/              demo storage
│   ├── jdbc/                MySQL storage
│   └── audit/               AuditedDonorRepository decorator
├── service/
├── ui/
└── util/                    Theme, PasswordUtil
```

## Run immediately - no database required

From the project folder:

```bash
javac -d out $(find src/main/java -name "*.java")
java -cp out com.bloodlink.Main
```

### Windows PowerShell

```powershell
New-Item -ItemType Directory -Force out | Out-Null
$files = Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $files
java -cp out com.bloodlink.Main
```

### Demo login

```text
Email:    admin@bloodlink.local
Password: Admin@123
```

Staff demo account:

```text
Email:    staff@bloodlink.local
Password: Staff@123
```

## Suggested demo script

This walks all three new features in about two minutes.

1. **Sign in.** Open the Audit trail. The sign in is already logged, along with any bags the startup expiry sweep caught.
2. **Dashboard.** Point out Issuable Units versus In Quarantine: they are different numbers because unscreened blood is not stock. Note the Expiring Soon and Wastage tiles.
3. **Inventory.** Show the bag register. Filter to "Expiring soon". Click "+ Record collection" and add 2 bags. Stock does not move, quarantine goes up by 2.
4. **Screening.** The new bags are on the worklist. Select one, "Mark panel non-reactive" and it is released. Select the other, "Record one marker", choose HIV and Reactive: the bag is discarded and the donor is deferred.
5. **Donors.** The deferred donor now shows Available = No.
6. **Requests.** Approve a request, then look at Inventory filtered to "Reserved" and see exactly which bag codes were held, nearest expiry first. Reject the request and watch them return to the shelf.
7. **Audit trail.** Every step above is there, in order, with bag codes and reasons.

## MySQL mode

1. Create a MySQL database named `bloodlink` or run `sql/schema.sql`.
2. Add MySQL Connector/J to the runtime classpath, or use Maven.
3. Set these environment variables:

```text
BLOODLINK_DB_URL=jdbc:mysql://localhost:3306/bloodlink
BLOODLINK_DB_USER=root
BLOODLINK_DB_PASSWORD=your_password
```

4. Run the application. BloodLink detects the database variables and switches from demo repositories to JDBC repositories.
5. The application automatically creates missing tables and creates the default admin account on first launch.

With Maven installed:

```bash
mvn compile exec:java
```

### Upgrading a v1.0 database

The `inventory` table is no longer used. Run `sql/schema.sql` to create `blood_bags`, `bag_screening` and `audit_log`. The old table can be dropped once you have migrated any stock you care about into individual bag rows.

## Default MySQL admin

```text
Email:    admin@bloodlink.local
Password: Admin@123
```

The password is not stored in plaintext. BloodLink uses **PBKDF2-HMAC-SHA256** with a random salt and 120,000 iterations.

## Theme

The interface uses a maroon, red and white palette defined in one place, `util/Theme.java`. Change the constants at the top of that file and the whole application follows.

```text
MAROON_DEEP  #4A0C14   sidebar, table headers
MAROON       #6D111C   gradients, hover
RED          #B21B28   primary accent, active navigation
RED_SOFT     #FAECED   selection and tinted fills
BG           #FAF6F6   application background
CARD         #FFFFFF   surfaces
```

## Remaining extension points

The architecture is ready for these without a rewrite:

- donor health/eligibility questionnaire and deferral rules (90 or 120 day intervals)
- barcode scanning for bag codes
- component separation, one collection splitting into PRBC, plasma and platelets
- hospital accounts and patient portal
- email/SMS/WhatsApp notification adapters via a `NotificationChannel` interface
- PDF donation certificates and CSV exports
- charts on the dashboard
- multiple blood-bank branches with inter-branch transfers
- camp management
- role permissions enforced at the service layer
- donor reward badges and leaderboard
- JUnit tests over the service layer, which needs no mocking framework because the in-memory repositories already exist

## Safety note

The compatibility screen and the screening workflow are educational and operational-support functionality. Real transfusion decisions must always follow hospital blood-bank protocols, clinical judgment, blood typing, cross-matching, licensed assay procedures and applicable regulations.

## Suggested project presentation flow

1. Problem statement and objectives
2. OOP design and class hierarchy
3. Architecture diagram
4. Login and role management
5. Donor management
6. Bag level inventory, shelf life and FEFO
7. TTI screening and quarantine
8. Emergency blood request workflow with reservations
9. Compatibility checker
10. Appointment module
11. Reports, wastage and the audit trail
12. MySQL persistence
13. Security and validation
14. Future scope

---

**BloodLink v2.0** - Java OOP Blood Bank Management System
