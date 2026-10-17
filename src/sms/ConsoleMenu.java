package sms;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import sms.exception.EntityNotFoundException;
import sms.exception.InsufficientFundsException;
import sms.model.*;
import sms.model.finance.TransactionType;
import sms.repository.StudentCsvRepository;
import sms.repository.TeacherCsvRepository;
import sms.repository.TransactionCsvRepository;
import sms.service.*;

/** Input/output controller. Business rules remain in services and models. */
public final class ConsoleMenu {
  private final Scanner scanner = new Scanner(System.in);
  private final SchoolService schools;
  private final FinanceService finance;
  private final AttendanceService attendance;
  private final StaffAttendanceService staffAttendance;
  private final ResultService results;
  private final TimetableService timetable;
  private final StudentCsvRepository studentRepository;
  private final TeacherCsvRepository teacherRepository;
  private final TransactionCsvRepository transactionRepository;

  public ConsoleMenu(
      SchoolService schools,
      FinanceService finance,
      AttendanceService attendance,
      StaffAttendanceService staffAttendance,
      ResultService results,
      TimetableService timetable,
      StudentCsvRepository studentRepository,
      TeacherCsvRepository teacherRepository,
      TransactionCsvRepository transactionRepository) {
    this.schools = schools;
    this.finance = finance;
    this.attendance = attendance;
    this.staffAttendance = staffAttendance;
    this.results = results;
    this.timetable = timetable;
    this.studentRepository = studentRepository;
    this.teacherRepository = teacherRepository;
    this.transactionRepository = transactionRepository;
  }

  public void start() {
    boolean running = true;
    while (running) {
      header(
          "SCHOOL MANAGEMENT SYSTEM",
          "1. Student Management",
          "2. Teacher Management",
          "3. Class & Subject Management",
          "4. Fee Management",
          "5. Salary Management",
          "6. Attendance Management",
          "7. Exam & Result Management",
          "8. Timetable Management",
          "9. Finance & Expenses",
          "10. Reports",
          "0. Exit");
      switch (integer("Enter your choice: ")) {
        case 1 -> studentMenu();
        case 2 -> teacherMenu();
        case 3 -> classMenu();
        case 4 -> feeMenu();
        case 5 -> salaryMenu();
        case 6 -> attendanceMenu();
        case 7 -> examMenu();
        case 8 -> timetableMenu();
        case 9 -> financeMenu();
        case 10 -> reportsMenu();
        case 0 -> {
          save();
          running = false;
        }
        default -> say("Invalid menu choice.");
      }
    }
    say("Data saved. Goodbye.");
  }

  private void studentMenu() {
    boolean back = false;
    while (!back) {
      header(
          "STUDENT MANAGEMENT",
          "1. Add Student",
          "2. View All Students",
          "3. Search Student By ID",
          "4. Search Student By Name",
          "5. Update Student",
          "6. Deactivate/Remove Student",
          "7. Promote Student",
          "8. Pay Student Fees",
          "9. View Student Fee Details",
          "10. View Student Attendance",
          "11. View Student Results",
          "0. Back");
      try {
        switch (integer("Choice: ")) {
          case 1 -> addStudent();
          case 2 -> schools.getStudents().forEach(System.out::println);
          case 3 -> System.out.println(student());
          case 4 -> schools.searchStudentsByName(text("Name: ")).forEach(System.out::println);
          case 5 -> updateStudent();
          case 6 -> schools.removeStudent(integer("Student ID: "));
          case 7 -> promote();
          case 8 -> payFee();
          case 9 -> feeDetails();
          case 10 -> showStudentAttendance();
          case 11 -> showResults();
          case 0 -> back = true;
          default -> say("Invalid menu choice.");
        }
      } catch (RuntimeException e) {
        handle(e);
      }
    }
  }

  private void teacherMenu() {
    boolean back = false;
    while (!back) {
      header(
          "TEACHER MANAGEMENT",
          "1. Add Teacher",
          "2. View All Teachers",
          "3. Search Teacher By ID",
          "4. Search Teacher By Name",
          "5. Update Teacher",
          "6. Deactivate/Remove Teacher",
          "7. Assign Teacher To Subject/Class",
          "8. Pay Teacher Salary",
          "9. View Salary History",
          "10. View Teacher Attendance",
          "0. Back");
      try {
        switch (integer("Choice: ")) {
          case 1 -> addTeacher();
          case 2 -> schools.getTeachers().forEach(System.out::println);
          case 3 -> System.out.println(teacher());
          case 4 -> schools.searchTeachersByName(text("Name: ")).forEach(System.out::println);
          case 5 -> updateTeacher();
          case 6 -> schools.removeTeacher(integer("Teacher ID: "));
          case 7 -> assign();
          case 8 -> paySalary();
          case 9 -> teacher().getSalaryPayments().forEach(System.out::println);
          case 10 -> showStaffAttendance();
          case 0 -> back = true;
          default -> say("Invalid menu choice.");
        }
      } catch (RuntimeException e) {
        handle(e);
      }
    }
  }

  private void classMenu() {
    boolean back = false;
    while (!back) {
      header(
          "CLASS & SUBJECT MANAGEMENT",
          "1. Create Academic Year",
          "2. Create Class/Section",
          "3. View Classes",
          "4. Add Subject",
          "5. View Subjects",
          "6. Assign Subject/Teacher/Class",
          "7. View Teaching Assignments",
          "8. Promote Student",
          "0. Back");
      try {
        switch (integer("Choice: ")) {
          case 1 -> createYear();
          case 2 -> createClass();
          case 3 -> schools.getAcademicClasses().forEach(System.out::println);
          case 4 -> addSubject();
          case 5 -> schools.getSubjects().forEach(System.out::println);
          case 6 -> assign();
          case 7 -> schools.getTeachingAssignments().forEach(System.out::println);
          case 8 -> promote();
          case 0 -> back = true;
          default -> say("Invalid menu choice.");
        }
      } catch (RuntimeException e) {
        handle(e);
      }
    }
  }

  private void feeMenu() {
    menu(
        "FEE MANAGEMENT",
        new String[] {
          "1. Configure/View Fee Structure",
          "2. Pay Student Fees",
          "3. View Student Remaining Fees",
          "4. View Total Fees Collected",
          "5. View Pending Fees",
          "6. View Fee Transactions",
          "0. Back"
        },
        choice -> {
          switch (choice) {
            case 1 -> configureFees();
            case 2 -> payFee();
            case 3 -> feeDetails();
            case 4 -> System.out.println(finance.getTotalFeesCollected());
            case 5 -> System.out.println(finance.getPendingFees(schools.getStudents()));
            case 6 ->
                finance
                    .getTransactionsByType(TransactionType.FEE_PAYMENT)
                    .forEach(System.out::println);
            default -> invalid(choice);
          }
        });
  }

  private void salaryMenu() {
    menu(
        "SALARY MANAGEMENT",
        new String[] {
          "1. View Teachers",
          "2. Pay Teacher Salary",
          "3. View Salary History",
          "4. View Total Salary Paid",
          "0. Back"
        },
        choice -> {
          switch (choice) {
            case 1 -> schools.getTeachers().forEach(System.out::println);
            case 2 -> paySalary();
            case 3 -> teacher().getSalaryPayments().forEach(System.out::println);
            case 4 -> System.out.println(finance.getTotalSalaryPaid());
            default -> invalid(choice);
          }
        });
  }

  private void attendanceMenu() {
    menu(
        "ATTENDANCE",
        new String[] {
          "1. Mark Student Attendance",
          "2. View Student Attendance",
          "3. View Student Attendance Percentage",
          "4. Mark Staff Attendance",
          "5. View Staff Attendance",
          "0. Back"
        },
        choice -> {
          switch (choice) {
            case 1 ->
                attendance.markAttendance(
                    student(),
                    date("Date (YYYY-MM-DD): "),
                    enumValue(AttendanceStatus.class, "Status"));
            case 2 -> showStudentAttendance();
            case 3 ->
                System.out.println(attendance.getAttendancePercentage(student().getId()) + "%");
            case 4 ->
                staffAttendance.markAttendance(
                    teacher(),
                    date("Date (YYYY-MM-DD): "),
                    enumValue(StaffAttendanceStatus.class, "Status"));
            case 5 -> showStaffAttendance();
            default -> invalid(choice);
          }
        });
  }

  private void examMenu() {
    menu(
        "EXAMS & RESULTS",
        new String[] {
          "1. Create Exam",
          "2. Record Student Marks",
          "3. View Student Results",
          "4. View Percentage",
          "5. View Grade",
          "0. Back"
        },
        choice -> {
          switch (choice) {
            case 1 ->
                schools.createExam(new Exam(integer("Exam ID: "), text("Name: "), date("Date: ")));
            case 2 ->
                results.recordMarks(
                    student(),
                    schools.getExam(integer("Exam ID: ")),
                    schools.getSubject(integer("Subject ID: ")),
                    integer("Marks: "));
            case 3 -> showResults();
            case 4 -> System.out.println(results.getOverallPercentage(student().getId()));
            case 5 -> {
              double percentage = results.getOverallPercentage(student().getId());
              System.out.println(results.getGrade(percentage));
            }
            default -> invalid(choice);
          }
        });
  }

  private void timetableMenu() {
    menu(
        "TIMETABLE",
        new String[] {
          "1. Add Timetable Entry",
          "2. View Class Timetable",
          "3. View Teacher Timetable",
          "4. Remove Timetable Entry",
          "0. Back"
        },
        choice -> {
          switch (choice) {
            case 1 -> addTimetable();
            case 2 ->
                timetable.getForClass(academicClass().classSection()).forEach(System.out::println);
            case 3 -> timetable.getForTeacher(teacher().getId()).forEach(System.out::println);
            case 4 -> removeTimetable();
            default -> invalid(choice);
          }
        });
  }

  private void financeMenu() {
    menu(
        "FINANCE & EXPENSES",
        new String[] {
          "1. Add School Expense",
          "2. Record Donation",
          "3. View Total Income",
          "4. View Total Expenses",
          "5. View Current Balance",
          "6. View Transactions",
          "7. Filter Transactions By Type",
          "0. Back"
        },
        choice -> {
          switch (choice) {
            case 1 ->
                finance.addExpense(
                    enumValue(ExpenseCategory.class, "Category"),
                    text("Purpose: "),
                    money("Amount: "));
            case 2 -> finance.receiveDonation(text("Donor: "), money("Amount: "));
            case 3 -> System.out.println(finance.getTotalIncome());
            case 4 -> System.out.println(finance.getTotalExpenses());
            case 5 -> System.out.println(finance.getBalance());
            case 6 -> finance.getTransactions().forEach(System.out::println);
            case 7 ->
                finance
                    .getTransactionsByType(enumValue(TransactionType.class, "Type"))
                    .forEach(System.out::println);
            default -> invalid(choice);
          }
        });
  }

  private void reportsMenu() {
    menu(
        "REPORTS",
        new String[] {
          "1. Student Summary",
          "2. Teacher Summary",
          "3. Fee Summary",
          "4. Salary Summary",
          "5. Expense Summary",
          "6. Attendance Summary",
          "7. Result Summary",
          "8. Financial Summary",
          "0. Back"
        },
        choice -> {
          switch (choice) {
            case 1 -> System.out.println("Students: " + schools.getStudents().size());
            case 2 -> System.out.println("Teachers: " + schools.getTeachers().size());
            case 3 ->
                System.out.println(
                    "Collected: "
                        + finance.getTotalFeesCollected()
                        + ", Pending: "
                        + finance.getPendingFees(schools.getStudents()));
            case 4 -> System.out.println("Salaries paid: " + finance.getTotalSalaryPaid());
            case 5 -> System.out.println("Other expenses: " + finance.getTotalOtherExpenses());
            case 6 -> showStudentAttendance();
            case 7 -> showResults();
            case 8 -> financialSummary();
            default -> invalid(choice);
          }
        });
  }

  private void addStudent() {
    schools.enrollStudent(
        new Student(
            integer("Student ID: "),
            text("Admission number: "),
            text("Name: "),
            text("Email (blank allowed): "),
            text("Phone (blank allowed): "),
            new ClassSection(text("Grade: "), text("Section: ")),
            date("Admission date: "),
            money("Initial total fee: "),
            BigDecimal.ZERO,
            true));
  }

  private void addTeacher() {
    schools.hireTeacher(
        new Teacher(
            integer("Teacher ID: "),
            text("Name: "),
            text("Email (blank allowed): "),
            text("Phone (blank allowed): "),
            "",
            text("Department: "),
            money("Monthly salary: "),
            true));
  }

  private void addSubject() {
    schools.addSubject(
        new Subject(integer("Subject ID: "), text("Subject name: "), integer("Maximum marks: ")));
  }

  private void updateStudent() {
    Student value = student();
    value.setName(text("New name: "));
    value.setEmail(text("New email: "));
    value.setPhone(text("New phone: "));
  }

  private void updateTeacher() {
    Teacher value = teacher();
    value.setName(text("New name: "));
    value.setEmail(text("New email: "));
    value.setPhone(text("New phone: "));
    value.setDepartment(text("New department: "));
    value.setMonthlySalary(money("New salary: "));
  }

  private void createYear() {
    schools.createAcademicYear(
        new AcademicYear(text("Year name: "), date("Start date: "), date("End date: ")));
  }

  private void createClass() {
    schools.createClass(
        new AcademicClass(academicYear(), new ClassSection(text("Grade: "), text("Section: "))));
  }

  private void assign() {
    Teacher value = teacher();
    Subject subject = schools.getSubject(integer("Subject ID: "));
    schools.assignTeacherToSubjectForClass(value.getId(), subject.getId(), academicClass());
  }

  private void promote() {
    schools.promoteStudent(student().getId(), academicClass());
  }

  private void payFee() {
    Student value = student();
    finance.collectFee(value, money("Payment amount: "));
    say("Remaining fees: " + value.getRemainingFees());
  }

  private void paySalary() {
    finance.paySalary(teacher());
    say("Salary payment recorded.");
  }

  private void configureFees() {
    Student value = student();
    System.out.println(value.getFeeStructure().getAmounts());
    if (yesNo("Change a category? (y/n): "))
      value.setFeeAmount(enumValue(FeeCategory.class, "Category"), money("Amount: "));
  }

  private void feeDetails() {
    Student value = student();
    System.out.println("Fee structure: " + value.getFeeStructure().getAmounts());
    System.out.println("Paid: " + value.getFeesPaid() + ", Remaining: " + value.getRemainingFees());
  }

  private void showStudentAttendance() {
    attendance.getAttendanceByStudent(student().getId()).forEach(System.out::println);
  }

  private void showStaffAttendance() {
    staffAttendance.getAttendanceForTeacher(teacher().getId()).forEach(System.out::println);
  }

  private void showResults() {
    results.getStudentResults(student().getId()).forEach(System.out::println);
  }

  private void addTimetable() {
    AcademicClass value = academicClass();
    int teacherId = integer("Teacher ID: ");
    int subjectId = integer("Subject ID: ");
    if (!schools.hasTeachingAssignment(teacherId, subjectId, value))
      throw new IllegalArgumentException("Create the teacher-subject-class assignment first.");
    timetable.addEntry(
        new TimetableEntry(
            value.classSection(),
            enumValue(DayOfWeek.class, "Day"),
            integer("Period: "),
            subjectId,
            teacherId,
            text("Room: ")));
  }

  private void removeTimetable() {
    AcademicClass value = academicClass();
    TimetableEntry entry =
        new TimetableEntry(
            value.classSection(),
            enumValue(DayOfWeek.class, "Day"),
            integer("Period: "),
            integer("Subject ID: "),
            integer("Teacher ID: "),
            text("Room: "));
    if (!timetable.removeEntry(entry)) say("No matching entry found.");
  }

  private void financialSummary() {
    System.out.println("Students: " + schools.getStudents().size());
    System.out.println("Teachers: " + schools.getTeachers().size());
    System.out.println("Fees: " + finance.getTotalFeesCollected());
    System.out.println("Pending: " + finance.getPendingFees(schools.getStudents()));
    System.out.println("Salaries: " + finance.getTotalSalaryPaid());
    System.out.println("Other expenses: " + finance.getTotalOtherExpenses());
    System.out.println("Donations: " + finance.getTotalDonations());
    System.out.println("Income: " + finance.getTotalIncome());
    System.out.println("Expenses: " + finance.getTotalExpenses());
    System.out.println("Balance: " + finance.getBalance());
    System.out.println("Transactions: " + finance.getTransactions().size());
  }

  private Student student() {
    return schools.getStudent(integer("Student ID: "));
  }

  private Teacher teacher() {
    return schools.getTeacher(integer("Teacher ID: "));
  }

  private AcademicYear academicYear() {
    String name = text("Academic year name: ");
    return schools.getAcademicYears().stream()
        .filter(year -> year.name().equalsIgnoreCase(name))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + name));
  }

  private AcademicClass academicClass() {
    AcademicYear year = academicYear();
    ClassSection section = new ClassSection(text("Grade: "), text("Section: "));
    return schools.getAcademicClasses().stream()
        .filter(value -> value.academicYear().equals(year) && value.classSection().equals(section))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Academic class not found."));
  }

  private void menu(String title, String[] options, java.util.function.IntConsumer action) {
    boolean back = false;
    while (!back) {
      header(title, options);
      try {
        int choice = integer("Choice: ");
        if (choice == 0) back = true;
        else action.accept(choice);
      } catch (RuntimeException e) {
        handle(e);
      }
    }
  }

  private void invalid(int choice) {
    if (choice != 0) say("Invalid menu choice.");
  }

  private int integer(String prompt) {
    while (true) {
      System.out.print(prompt);
      try {
        return Integer.parseInt(scanner.nextLine().trim());
      } catch (NumberFormatException e) {
        say("Invalid input. Please enter a numeric value.");
      }
    }
  }

  private BigDecimal money(String prompt) {
    while (true) {
      System.out.print(prompt);
      try {
        BigDecimal value = new BigDecimal(scanner.nextLine().trim());
        if (value.signum() < 0) throw new NumberFormatException();
        return value;
      } catch (NumberFormatException e) {
        say("Invalid input. Please enter a non-negative decimal amount.");
      }
    }
  }

  private LocalDate date(String prompt) {
    while (true) {
      System.out.print(prompt);
      try {
        return LocalDate.parse(scanner.nextLine().trim());
      } catch (java.time.format.DateTimeParseException e) {
        say("Invalid date. Use YYYY-MM-DD.");
      }
    }
  }

  private <E extends Enum<E>> E enumValue(Class<E> type, String prompt) {
    while (true) {
      System.out.print(prompt + " " + Arrays.toString(type.getEnumConstants()) + ": ");
      try {
        return Enum.valueOf(type, scanner.nextLine().trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        say("Invalid choice.");
      }
    }
  }

  private String text(String prompt) {
    System.out.print(prompt);
    return scanner.nextLine().trim();
  }

  private boolean yesNo(String prompt) {
    return text(prompt).equalsIgnoreCase("y");
  }

  private void save() {
    studentRepository.saveAll(new ArrayList<>(schools.getStudents()));
    teacherRepository.saveAll(new ArrayList<>(schools.getTeachers()));
    transactionRepository.saveAll(finance.getTransactions());
  }

  private static void header(String title, String... options) {
    System.out.println("\n========================================");
    System.out.println(title);
    System.out.println("========================================");
    for (String option : options) System.out.println(option);
  }

  private static void say(String value) {
    System.out.println(value);
  }

  private static void handle(RuntimeException e) {
    if (e instanceof EntityNotFoundException
        || e instanceof InsufficientFundsException
        || e instanceof IllegalArgumentException
        || e instanceof IllegalStateException) say("Error: " + e.getMessage());
    else throw e;
  }
}
