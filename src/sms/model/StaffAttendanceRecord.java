package sms.model;

import java.time.LocalDate;

public record StaffAttendanceRecord(int teacherId, LocalDate date, StaffAttendanceStatus status) {
  public StaffAttendanceRecord {
    if (teacherId <= 0 || date == null || status == null) {
      throw new IllegalArgumentException("Teacher ID, date, and status are required.");
    }
  }
}
