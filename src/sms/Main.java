package sms;

import java.nio.file.Path;
import sms.model.School;
import sms.repository.StudentCsvRepository;
import sms.repository.TeacherCsvRepository;
import sms.repository.TransactionCsvRepository;
import sms.service.AttendanceService;
import sms.service.FinanceService;
import sms.service.ResultService;
import sms.service.SchoolService;
import sms.service.StaffAttendanceService;
import sms.service.TimetableService;

/** Initializes the backend and starts the interactive command-line application. */
public class Main {
  public static void main(String[] args) {
    SchoolService schools = new SchoolService(new School("Green Valley High School"));
    FinanceService finance = new FinanceService();
    StudentCsvRepository students = new StudentCsvRepository(Path.of("data", "students.csv"));
    TeacherCsvRepository teachers = new TeacherCsvRepository(Path.of("data", "teachers.csv"));
    TransactionCsvRepository transactions =
        new TransactionCsvRepository(Path.of("data", "transactions.csv"));

    students.findAll().forEach(schools::enrollStudent);
    teachers.findAll().forEach(schools::hireTeacher);
    finance.loadTransactions(transactions.findAll());

    new ConsoleMenu(
            schools,
            finance,
            new AttendanceService(),
            new StaffAttendanceService(),
            new ResultService(),
            new TimetableService(),
            students,
            teachers,
            transactions)
        .start();
  }
}
