package sms.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sms.model.StaffAttendanceRecord;
import sms.model.StaffAttendanceStatus;
import sms.model.Teacher;

public class StaffAttendanceService {
  private final Map<String, StaffAttendanceRecord> records = new LinkedHashMap<>();

  public void markAttendance(Teacher teacher, LocalDate date, StaffAttendanceStatus status) {
    if (teacher == null || date == null || status == null || date.isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("Teacher, non-future date, and status are required.");
    }
    String key = teacher.getId() + ":" + date;
    if (records.containsKey(key))
      throw new IllegalStateException("Staff attendance is already recorded.");
    records.put(key, new StaffAttendanceRecord(teacher.getId(), date, status));
  }

  public List<StaffAttendanceRecord> getAttendanceForTeacher(int teacherId) {
    return records.values().stream().filter(record -> record.teacherId() == teacherId).toList();
  }
}
