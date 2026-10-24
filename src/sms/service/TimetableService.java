package sms.service;

import java.util.*;
import sms.model.*;

public class TimetableService {
  private final List<TimetableEntry> entries = new ArrayList<>();

  public void addEntry(TimetableEntry entry) {
    Objects.requireNonNull(entry);
    validateNoConflict(entry, null);
    entries.add(entry);
  }

  public boolean updateEntry(int index, TimetableEntry replacement) {
    if (index < 0 || index >= entries.size()) throw new IllegalArgumentException("Invalid timetable entry.");
    validateNoConflict(replacement, entries.get(index));
    entries.set(index, replacement);
    return true;
  }

  public boolean removeEntry(int index) {
    if (index < 0 || index >= entries.size()) return false;
    entries.remove(index);
    return true;
  }

  public boolean removeEntry(TimetableEntry entry) {
    return entries.remove(entry);
  }

  public List<TimetableEntry> getEntries() {
    return List.copyOf(entries);
  }

  public void loadEntries(List<TimetableEntry> saved) {
    entries.clear();
    for (TimetableEntry entry : saved) addEntry(entry);
  }

  private void validateNoConflict(TimetableEntry entry, TimetableEntry ignored) {
    for (TimetableEntry existing : entries) {
      if (existing == ignored || !existing.day().equals(entry.day())) continue;
      boolean overlaps = entry.startTime().isBefore(existing.endTime())
          && existing.startTime().isBefore(entry.endTime());
      boolean sharedResource = existing.teacherId() == entry.teacherId()
          || existing.classSection().equals(entry.classSection())
          || (!entry.room().isBlank() && entry.room().equalsIgnoreCase(existing.room()));
      if (overlaps && sharedResource)
        throw new IllegalStateException("Timetable conflict: teacher, class, or room is already scheduled during this time.");
    }
  }

  public List<TimetableEntry> getForClass(ClassSection section) {
    return entries.stream().filter(e -> e.classSection().equals(section)).toList();
  }

  public List<TimetableEntry> getForTeacher(int teacherId) {
    return entries.stream().filter(e -> e.teacherId() == teacherId).toList();
  }
}
