package sms.model;

import java.time.DayOfWeek;

public record TimetableEntry(
    ClassSection classSection,
    DayOfWeek day,
    int period,
    int subjectId,
    int teacherId,
    String room) {
  public TimetableEntry {
    if (classSection == null || day == null || period <= 0 || subjectId <= 0 || teacherId <= 0)
      throw new IllegalArgumentException(
          "A timetable entry requires class, day, positive period, subject, and teacher.");
    room = room == null ? "" : room.trim();
  }

  public DayPeriod slot() {
    return new DayPeriod(day, period);
  }
}
