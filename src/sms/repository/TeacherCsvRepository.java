package sms.repository;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import sms.exception.PersistenceException;
import sms.model.Teacher;

public class TeacherCsvRepository implements Repository<Teacher> {
  private final Path file;

  public TeacherCsvRepository(Path file) {
    this.file = Objects.requireNonNull(file);
  }

  @Override
  public void saveAll(List<Teacher> items) {
    write(
        items.stream()
            .map(
                t ->
                    String.join(
                        ",",
                        String.valueOf(t.getId()),
                        clean(t.getName()),
                        clean(t.getEmail()),
                        clean(t.getPhone()),
                        clean(t.getDepartment()),
                        t.getMonthlySalary().toPlainString(),
                        String.valueOf(t.isActive()),
                        t.getSubjectIds().stream()
                            .map(String::valueOf)
                            .collect(java.util.stream.Collectors.joining(";"))))
            .toList());
  }

  @Override
  public List<Teacher> findAll() {
    List<Teacher> found = new ArrayList<>();
    for (String line : read()) {
      try {
        String[] p = line.split(",", -1);
        if (p.length != 8) continue;
        Teacher t =
            new Teacher(
                Integer.parseInt(p[0]),
                p[1],
                p[2],
                p[3],
                "",
                p[4],
                new BigDecimal(p[5]),
                Boolean.parseBoolean(p[6]));
        if (!p[7].isBlank())
          for (String id : p[7].split(";")) t.assignSubject(Integer.parseInt(id));
        found.add(t);
      } catch (RuntimeException ignored) {
      }
    }
    return found;
  }

  private List<String> read() {
    if (!Files.exists(file)) return List.of();
    try {
      return Files.readAllLines(file);
    } catch (IOException e) {
      throw new PersistenceException("Could not read teachers CSV.", e);
    }
  }

  private void write(List<String> lines) {
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      Files.write(file, lines);
    } catch (IOException e) {
      throw new PersistenceException("Could not write teachers CSV.", e);
    }
  }

  private static String clean(String s) {
    return s.replace(",", " ").replace("\n", " ");
  }
}
