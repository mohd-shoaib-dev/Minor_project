# Hospital Management System (BCA Minor Project)

A Java Swing + JDBC + MySQL desktop application for outpatient department (OPD) administration. Features are based on the supplied project report: role-based login, patient registration and viewing, doctor directory, appointment booking with doctor/day conflict prevention, patient appointment history, and admin-only patient deletion with linked appointment cleanup.

## Requirements
- JDK 17 or later
- Maven 3.8+
- MySQL 8.0+
- IntelliJ IDEA, Eclipse, or VS Code with Java extensions

## 1. Create the database
1. Start MySQL Server.
2. Open MySQL Workbench or a terminal connected to MySQL.
3. Run the complete script `database/schema.sql`.
4. The script creates the `hospital` database, tables, demo users, and sample doctors.

Demo login accounts:
| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Receptionist | `reception` | `reception123` |
| Viewer | `viewer` | `viewer123` |

These are classroom demo credentials only. Change them before any real use.

## 2. Configure database connection
The defaults are:
- URL: `jdbc:mysql://localhost:3306/hospital`
- User: `root`
- Password: empty

Set environment variables if your MySQL credentials differ:
- `HMS_DB_URL`
- `HMS_DB_USER`
- `HMS_DB_PASSWORD`

Windows PowerShell example for the current terminal:
```powershell
$env:HMS_DB_USER="root"
$env:HMS_DB_PASSWORD="your_mysql_password"
```

## 3. Run
From the project root:
```bash
mvn clean compile
mvn exec:java
```
Or open the folder in IntelliJ IDEA as a Maven project and run `com.ims.hms.HospitalManagementSystemGUI`.

## Role permissions
- **Admin:** add/view patients, view doctors, book appointments, view history, remove patients.
- **Reception:** add/view patients, view doctors, book appointments, view history; cannot remove patients.
- **Viewer:** read-only patient/doctor lists and appointment history.

## Important notes
- The unique constraint on `(doctor_id, appointment_date)` prevents booking the same doctor twice on one day, matching the report's stated scheduling rule.
- The demo uses SHA-256 password hashes for a simple classroom setup. For production, use BCrypt/Argon2, secure configuration, audit logging, and stronger access controls.
- This is an educational project and must not be used to store real patient information without professional security, privacy, and compliance review.
