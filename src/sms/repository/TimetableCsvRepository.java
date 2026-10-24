package sms.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import sms.exception.PersistenceException;
import sms.model.ClassSection;
import sms.model.TimetableEntry;

public class TimetableCsvRepository {
  private final Path file;

  public TimetableCsvRepository(Path file) { this.file = Objects.requireNonNull(file); }

  public List<TimetableEntry> findAll() {
    if (!Files.exists(file)) return List.of();
    List<TimetableEntry> entries = new ArrayList<>();
    try {
      for (String line : Files.readAllLines(file)) {
        String[] f = line.split(",", -1);
        if (f.length != 8) continue;
        try {
          LocalTime start = LocalTime.parse(f[1]);
          int period = start.toSecondOfDay() / 60 / 50 + 1;
          entries.add(new TimetableEntry(new ClassSection(f[3], f[4]), DayOfWeek.valueOf(f[0]),
              period, start, LocalTime.parse(f[2]), Integer.parseInt(f[5]),
              Integer.parseInt(f[6]), f[7]));
        } catch (RuntimeException ignored) { }
      }
      return entries;
    } catch (IOException e) { throw new PersistenceException("Could not read timetable CSV.", e); }
  }

  public void saveAll(List<TimetableEntry> entries) {
    List<String> lines = entries.stream().map(e -> String.join(",", e.day().name(),
        e.startTime().toString(), e.endTime().toString(), clean(e.classSection().grade()),
        clean(e.classSection().section()), String.valueOf(e.subjectId()),
        String.valueOf(e.teacherId()), clean(e.room()))).toList();
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      Files.write(file, lines);
    } catch (IOException e) { throw new PersistenceException("Could not write timetable CSV.", e); }
  }

  private static String clean(String value) { return value.replace(",", " ").replace("\n", " "); }
}
