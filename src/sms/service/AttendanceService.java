package sms.service;

import java.time.*;
import java.util.*;
import sms.model.*;

public class AttendanceService {
  private final Map<String, AttendanceRecord> records = new LinkedHashMap<>();

  public void markAttendance(Student student, LocalDate date, AttendanceStatus status) {
    if (student == null || date == null || status == null)
      throw new IllegalArgumentException("Student, date, and status are required.");
    if (date.isAfter(LocalDate.now()))
      throw new IllegalArgumentException("Attendance cannot be marked in the future.");
    String key = key(student.getId(), date);
    if (records.containsKey(key))
      throw new IllegalStateException("Attendance is already recorded for this student and date.");
    records.put(key, new AttendanceRecord(student.getId(), date, status));
  }

  public double getAttendancePercentage(int studentId) {
    List<AttendanceRecord> items = getAttendanceByStudent(studentId);
    if (items.isEmpty()) return 0;
    return items.stream()
            .filter(
                r -> r.status() == AttendanceStatus.PRESENT || r.status() == AttendanceStatus.LATE)
            .count()
        * 100.0
        / items.size();
  }

  public List<AttendanceRecord> getAttendanceByStudent(int id) {
    return records.values().stream().filter(r -> r.studentId() == id).toList();
  }

  public List<AttendanceRecord> getAttendanceByDate(LocalDate date) {
    return records.values().stream().filter(r -> r.date().equals(date)).toList();
  }

  public List<AttendanceRecord> getMonthlyReport(int id, YearMonth month) {
    return getAttendanceByStudent(id).stream()
        .filter(r -> YearMonth.from(r.date()).equals(month))
        .toList();
  }

  private static String key(int id, LocalDate d) {
    return id + ":" + d;
  }
}
