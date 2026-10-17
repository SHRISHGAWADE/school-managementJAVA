package sms.model;

import java.time.LocalDate;

public record AttendanceRecord(int studentId, LocalDate date, AttendanceStatus status) {
  public AttendanceRecord {
    if (studentId <= 0 || date == null || status == null)
      throw new IllegalArgumentException("Student ID, date, and status are required.");
  }
}
