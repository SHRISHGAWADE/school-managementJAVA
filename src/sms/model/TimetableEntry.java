package sms.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record TimetableEntry(
    ClassSection classSection,
    DayOfWeek day,
    int period,
    LocalTime startTime,
    LocalTime endTime,
    int subjectId,
    int teacherId,
    String room) {
  public TimetableEntry {
    if (classSection == null || day == null || period <= 0 || startTime == null || endTime == null
        || !endTime.isAfter(startTime) || subjectId <= 0 || teacherId <= 0)
      throw new IllegalArgumentException(
          "A timetable entry requires class, day, valid times, subject, and teacher.");
    room = room == null ? "" : room.trim();
  }

  public TimetableEntry(ClassSection classSection, DayOfWeek day, int period,
      int subjectId, int teacherId, String room) {
    this(classSection, day, period, LocalTime.of(8, 0).plusMinutes((long) (period - 1) * 50),
        LocalTime.of(8, 0).plusMinutes((long) period * 50), subjectId, teacherId, room);
  }

  public DayPeriod slot() {
    return new DayPeriod(day, period);
  }
}
