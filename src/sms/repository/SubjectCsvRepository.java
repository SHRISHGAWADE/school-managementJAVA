package sms.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import sms.exception.PersistenceException;
import sms.model.Subject;

public class SubjectCsvRepository {
  private final Path file;

  public SubjectCsvRepository(Path file) { this.file = Objects.requireNonNull(file); }

  public List<Subject> findAll() {
    if (!Files.exists(file)) return List.of();
    List<Subject> items = new ArrayList<>();
    try {
      for (String line : Files.readAllLines(file)) {
        String[] fields = line.split(",", -1);
        if (fields.length != 3) continue;
        try { items.add(new Subject(Integer.parseInt(fields[0]), fields[1], Integer.parseInt(fields[2]))); }
        catch (RuntimeException ignored) { }
      }
      return items;
    } catch (IOException e) { throw new PersistenceException("Could not read subjects CSV.", e); }
  }

  public void saveAll(List<Subject> items) {
    write(items.stream().map(s -> s.getId() + "," + clean(s.getName()) + "," + s.getMaxMarks()).toList());
  }

  private void write(List<String> lines) {
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      Files.write(file, lines);
    } catch (IOException e) { throw new PersistenceException("Could not write subjects CSV.", e); }
  }

  private static String clean(String value) { return value.replace(",", " ").replace("\n", " "); }
}
