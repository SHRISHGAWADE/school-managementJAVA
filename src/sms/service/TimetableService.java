package sms.service;

import java.util.*;
import sms.model.*;

public class TimetableService {
  private final List<TimetableEntry> entries = new ArrayList<>();

  public void addEntry(TimetableEntry entry) {
    Objects.requireNonNull(entry);
    for (TimetableEntry existing : entries) {
      if (existing.slot().equals(entry.slot())
          && (existing.teacherId() == entry.teacherId()
              || existing.classSection().equals(entry.classSection())))
        throw new IllegalStateException(
            "Timetable conflict: teacher or class is already scheduled in this period.");
    }
    entries.add(entry);
  }

  public boolean removeEntry(TimetableEntry entry) {
    return entries.remove(entry);
  }

  public List<TimetableEntry> getEntries() {
    return List.copyOf(entries);
  }

  public List<TimetableEntry> getForClass(ClassSection section) {
    return entries.stream().filter(e -> e.classSection().equals(section)).toList();
  }

  public List<TimetableEntry> getForTeacher(int teacherId) {
    return entries.stream().filter(e -> e.teacherId() == teacherId).toList();
  }
}
