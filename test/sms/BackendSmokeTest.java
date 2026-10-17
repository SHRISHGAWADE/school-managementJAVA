package sms;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import sms.exception.InsufficientFundsException;
import sms.model.*;
import sms.repository.StudentCsvRepository;
import sms.service.*;

/** Dependency-free behavioral checks runnable with java -ea. */
public final class BackendSmokeTest {
  public static void main(String[] args) throws Exception {
    Student student =
        new Student(
            1,
            "ADM1",
            "Test Student",
            "",
            "",
            new ClassSection("10", "A"),
            LocalDate.now(),
            new BigDecimal("100"),
            BigDecimal.ZERO,
            true);
    Teacher teacher =
        new Teacher(2, "Test Teacher", "", "", "", "Math", new BigDecimal("50"), true);
    FinanceService finance = new FinanceService();
    expect(IllegalArgumentException.class, () -> finance.collectFee(student, BigDecimal.ZERO));
    finance.receiveDonation("Donor", new BigDecimal("100"));
    finance.collectFee(student, new BigDecimal("60"));
    assert student.getRemainingFees().equals(new BigDecimal("40"));
    student.setFeeAmount(FeeCategory.LIBRARY, new BigDecimal("10"));
    assert student.getTotalFees().equals(new BigDecimal("110"));
    expect(IllegalArgumentException.class, () -> finance.collectFee(student, new BigDecimal("51")));
    finance.paySalary(teacher);
    assert teacher.getSalaryPayments().size() == 1;
    expect(
        InsufficientFundsException.class,
        () -> finance.addExpense(ExpenseCategory.OTHER, "Too much", new BigDecimal("111")));
    AttendanceService attendance = new AttendanceService();
    attendance.markAttendance(student, LocalDate.now(), AttendanceStatus.PRESENT);
    expect(
        IllegalStateException.class,
        () -> attendance.markAttendance(student, LocalDate.now(), AttendanceStatus.ABSENT));
    ResultService results = new ResultService();
    Exam exam = new Exam(1, "Test", LocalDate.now());
    Subject subject = new Subject(1, "Math", 100);
    expect(IllegalArgumentException.class, () -> results.recordMarks(student, exam, subject, 101));
    TimetableService timetable = new TimetableService();
    TimetableEntry first =
        new TimetableEntry(student.getClassSection(), DayOfWeek.MONDAY, 1, 1, teacher.getId(), "1");
    timetable.addEntry(first);
    expect(
        IllegalStateException.class,
        () ->
            timetable.addEntry(
                new TimetableEntry(
                    new ClassSection("10", "B"), DayOfWeek.MONDAY, 1, 2, teacher.getId(), "2")));
    SchoolService schoolService = new SchoolService(new School("Test School"));
    AcademicYear year =
        new AcademicYear("2026-27", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
    AcademicClass class10A = new AcademicClass(year, new ClassSection("10", "A"));
    schoolService.createAcademicYear(year);
    schoolService.createClass(class10A);
    schoolService.enrollStudent(student);
    expect(IllegalArgumentException.class, () -> schoolService.enrollStudent(student));
    schoolService.hireTeacher(teacher);
    schoolService.addSubject(subject);
    schoolService.assignTeacherToSubjectForClass(teacher.getId(), subject.getId(), class10A);
    assert schoolService
        .getTeacherForSubjectAndClass(subject.getId(), class10A)
        .orElseThrow()
        .equals(teacher);
    schoolService.promoteStudent(student.getId(), class10A);
    assert student.getClassHistory().containsKey(year);
    StaffAttendanceService staffAttendance = new StaffAttendanceService();
    staffAttendance.markAttendance(teacher, LocalDate.now(), StaffAttendanceStatus.PRESENT);
    expect(
        IllegalStateException.class,
        () ->
            staffAttendance.markAttendance(teacher, LocalDate.now(), StaffAttendanceStatus.ABSENT));
    Path csv = Files.createTempDirectory("sms-csv-test").resolve("students.csv");
    StudentCsvRepository repository = new StudentCsvRepository(csv);
    repository.saveAll(List.of(student));
    assert repository.findAll().size() == 1;
    assert repository.findById(student.getId()).orElseThrow().getStatus() == StudentStatus.ACTIVE;
    System.out.println("Backend smoke tests passed.");
  }

  private static void expect(Class<? extends RuntimeException> type, Runnable action) {
    try {
      action.run();
      throw new AssertionError("Expected " + type.getSimpleName());
    } catch (RuntimeException actual) {
      if (!type.isInstance(actual)) throw actual;
    }
  }
}
