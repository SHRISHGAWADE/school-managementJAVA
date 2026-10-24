package sms.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import sms.exception.PersistenceException;
import sms.model.AttendanceRecord;
import sms.model.AttendanceStatus;

public class AttendanceCsvRepository {
  private final Path file;

  public AttendanceCsvRepository(Path file) {
    this.file = Objects.requireNonNull(file);
  }

  public List<AttendanceRecord> findAll() {
    if (!Files.exists(file)) return List.of();
    List<AttendanceRecord> records = new ArrayList<>();
    try {
      for (String line : Files.readAllLines(file)) {
        String[] fields = line.split(",", -1);
        if (fields.length != 3) continue;
        try {
          records.add(new AttendanceRecord(
              Integer.parseInt(fields[0]),
              java.time.LocalDate.parse(fields[1]),
              AttendanceStatus.valueOf(fields[2])));
        } catch (RuntimeException ignored) {
          // Ignore malformed rows while preserving valid attendance records.
        }
      }
      return records;
    } catch (IOException e) {
      throw new PersistenceException("Could not read attendance CSV.", e);
    }
  }

  public void saveAll(List<AttendanceRecord> records) {
    List<String> lines = records.stream()
        .map(record -> record.studentId() + "," + record.date() + "," + record.status().name())
        .toList();
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      Files.write(file, lines);
    } catch (IOException e) {
      throw new PersistenceException("Could not write attendance CSV.", e);
    }
  }
}
