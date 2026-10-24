package sms.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import sms.exception.PersistenceException;
import sms.model.Exam;

public class ExamCsvRepository {
  private final Path file;

  public ExamCsvRepository(Path file) { this.file = Objects.requireNonNull(file); }

  public List<Exam> findAll() {
    if (!Files.exists(file)) return List.of();
    List<Exam> items = new ArrayList<>();
    try {
      for (String line : Files.readAllLines(file)) {
        String[] fields = line.split(",", -1);
        if (fields.length != 3) continue;
        try { items.add(new Exam(Integer.parseInt(fields[0]), fields[1], LocalDate.parse(fields[2]))); }
        catch (RuntimeException ignored) { }
      }
      return items;
    } catch (IOException e) { throw new PersistenceException("Could not read exams CSV.", e); }
  }

  public void saveAll(List<Exam> items) {
    write(items.stream().map(e -> e.getId() + "," + clean(e.getName()) + "," + e.getExamDate()).toList());
  }

  private void write(List<String> lines) {
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      Files.write(file, lines);
    } catch (IOException e) { throw new PersistenceException("Could not write exams CSV.", e); }
  }

  private static String clean(String value) { return value.replace(",", " ").replace("\n", " "); }
}
