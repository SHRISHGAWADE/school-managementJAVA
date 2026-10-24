# School Management System

## Project Overview

A Java school management application with a browser interface served by the project's built-in Java HTTP server. Java contains the domain models, business rules, services, HTTP API, and CSV persistence; HTML, CSS, and JavaScript provide the browser UI and call that API. A separate interactive console entry point is also available.

## Implemented Modules

- Dashboard with current student, teacher, and finance metrics
- Student directory: enrollment, search, updates, activation, and CSV persistence
- Teacher directory: hiring, search, profile updates, activation, and CSV persistence
- Finance and fees: fee collection, salary payments, expenses, donations, summary metrics, and transaction ledger
- Student attendance: daily recording, date-based review, and attendance rates
- Exam results: exam and subject setup, marks entry and updates, percentages, and grades
- Academics: academic year date ranges and grade/section structure
- Timetable: weekly schedule, entry editing and deletion, overlap checks, and CSV persistence

## Package Structure

```text
src/sms
  Main.java
  ConsoleMenu.java
  exception/
    EntityNotFoundException.java
    InsufficientFundsException.java
    PersistenceException.java
  model/
    Person.java
    Student.java
    Teacher.java
    School.java
    AttendanceRecord.java
    AttendanceStatus.java
    Exam.java
    Subject.java
    Result.java
    ClassSection.java
    TimetableEntry.java
    AcademicYear.java
    AcademicClass.java
    ExpenseCategory.java
    finance/
      Transaction.java
      TransactionType.java
  repository/
    Repository.java
    StudentCsvRepository.java
    TeacherCsvRepository.java
    TransactionCsvRepository.java
    AttendanceCsvRepository.java
    ExamCsvRepository.java
    SubjectCsvRepository.java
    ResultCsvRepository.java
    TimetableCsvRepository.java
  service/
    SchoolService.java
    FinanceService.java
    AttendanceService.java
    ResultService.java
    TimetableService.java
  web/
    HttpApiServer.java
    JsonUtil.java
```

## Better Class Design

`Person` is an abstract parent class for common fields such as id, name, email, and phone. `Student` and `Teacher` extend it because both are people in the school system, but each has its own specific fields.

`School` stores students and teachers, but it does not handle fee collection, salary payment, attendance, or exam logic. Those responsibilities are moved to services:

- `SchoolService`: enrollment, hiring, lookup, class/subject assignment, and soft removal
- `FinanceService`: fees, salaries, expenses, donations, balance
- `AttendanceService`: daily attendance and attendance percentage
- `ResultService`: marks entry, percentage, grade calculation
- `TimetableService`: class/teacher schedules and conflict detection

This applies the Single Responsibility Principle because each class has one clear job.

## Relationships Between Classes

- `Student` is a `Person`
- `Teacher` is a `Person`
- `School` has many `Student` objects
- `School` has many `Teacher` objects
- `School` has many `Subject` and `Exam` objects
- `FinanceService` creates many `Transaction` objects
- `AttendanceService` creates many `AttendanceRecord` objects
- `ResultService` connects `Student`, `Exam`, and `Subject` through `Result`
- `TimetableEntry` connects a class/section, teacher, subject, day, time range, and room
- Repositories save and load data from CSV files

## New Classes Added

- `Person`: shared parent for students and teachers
- `Identifiable`: interface for classes with an id
- `Transaction`: stores every financial event
- `TransactionType`: fee, salary, expense, donation, refund
- `AttendanceRecord`: one attendance entry for one student on one date
- `AttendanceStatus`: present, absent, late, excused
- `Exam`: exam metadata
- `Subject`: subject metadata and maximum marks
- `Result`: marks scored by a student in a subject for an exam
- Repository classes: CSV persistence
- Custom exceptions: clearer error handling

## Realistic Finance Management

The finance system uses `BigDecimal` instead of `int` or `double`, which is better for money. Every fee payment, salary payment, expense, and donation becomes a `Transaction`.

Useful real-world features:

- Tracks total income
- Tracks total expenses
- Calculates current balance
- Prevents paying salaries or expenses when balance is insufficient
- Keeps full transaction history for auditing

## Attendance System

Attendance is stored as a list of `AttendanceRecord` objects. Each record contains:

- student id
- date
- status

This makes it possible to calculate attendance percentage, identify absent students, generate monthly reports, and support future parent notifications.

## Result And Exam Management

`ResultService` records marks for a student in a subject for an exam. It calculates:

- student result list
- overall percentage
- grade

This can later be expanded into report cards, class toppers, subject averages, and promotion eligibility.

## Transaction History

Transactions are stored in memory and saved to `data/transactions.csv`. This is important because real school ERPs must be able to answer questions such as:

- Who paid fees?
- When was salary paid?
- What expenses were recorded?
- What is the current balance?

## File Handling Persistence

The browser application loads CSV files from `data/` when its Java server starts and saves changes through the existing repositories. Files are created on first save where they do not yet exist:

- `data/students.csv`
- `data/teachers.csv`
- `data/transactions.csv`
- `data/attendance.csv`
- `data/exams.csv`
- `data/subjects.csv`
- `data/results.csv`
- `data/timetable.csv`
- `data/academic-years.csv`
- `data/academic-classes.csv`

The console application saves student, teacher, and transaction data when exiting through its menu. Keep the CSV files in place between runs to retain application data.

## Encapsulation And Abstraction

Fields are private and accessed through methods. Business rules are enforced inside methods, for example:

- student name cannot be empty
- fee and salary cannot be negative
- marks cannot exceed max marks
- expenses cannot be paid if balance is too low
- fee payments cannot exceed a student's remaining balance
- a teacher, class/section, or room cannot occupy overlapping timetable times
- only one attendance record may exist for a student/date pair

Abstraction is used through:

- abstract class `Person`
- interface `Identifiable`
- interface `Repository<T>`

## Interfaces, Inheritance, Exceptions

Inheritance:

- `Student extends Person`
- `Teacher extends Person`

Interfaces:

- `Identifiable`
- `Repository<T>`

Exception handling:

- `EntityNotFoundException`
- `InsufficientFundsException`
- `PersistenceException`

This makes errors more meaningful than generic crashes.

## Best Data Structures

- `Map<Integer, Student>` and `Map<Integer, Teacher>` inside `School`: fast lookup by id
- `List<Transaction>`: keeps transaction history in order
- `List<AttendanceRecord>`: simple daily attendance storage
- `List<Result>`: easy filtering by student, exam, and subject
- `Optional<T>`: safer lookup results

## Real ERP Features You Can Add Next

- parent/guardian profile
- class and section management
- library module
- transport module
- hostel module
- staff attendance
- fee invoice generation
- exam report card export
- user login and roles
- notifications
- database persistence using JDBC or Hibernate

## How To Run

### Prerequisites

- JDK 17 or newer (records and modern Java language features are used)
- PowerShell on Windows, or a Bash-compatible shell on Linux/macOS
- A modern web browser for the browser interface

### Browser Application

From the project folder in PowerShell, compile the Java sources and start the HTTP server:

```powershell
javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp out sms.web.HttpApiServer 8081
```

Open `http://localhost:8081` in your browser. Keep the server running while using the browser application. Press **Ctrl+C** to stop it.

On Linux/macOS, use:

```bash
mkdir -p out
find src -name '*.java' -print0 | xargs -0 javac -d out
java -cp out sms.web.HttpApiServer 8081
```

### Console Application

To start the interactive terminal menu instead, run the matching command after compiling:

```powershell
java -cp out sms.Main
```

```bash
java -cp out sms.Main
```

## OOP Principles Applied

- Encapsulation: private fields and controlled methods
- Inheritance: shared person data in `Person`
- Abstraction: services and repository interfaces hide implementation details
- Polymorphism: `Student` and `Teacher` can both be treated as `Person`
- Single Responsibility: finance, attendance, result, and school management are separate
- Open/Closed Principle: CSV repositories can be replaced with database repositories later
- Composition: services use models and repositories to perform application operations
- Java standard-library HTTP server: serves the browser files and JSON endpoints without an external framework
