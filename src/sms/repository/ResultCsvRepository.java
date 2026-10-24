package sms.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import sms.exception.PersistenceException;
import sms.model.Result;

public class ResultCsvRepository {
  private final Path file;

  public ResultCsvRepository(Path file) { this.file = Objects.requireNonNull(file); }

  public List<Result> findAll() {
    if (!Files.exists(file)) return List.of();
    List<Result> items = new ArrayList<>();
    try {
      for (String line : Files.readAllLines(file)) {
        String[] fields = line.split(",", -1);
        if (fields.length != 5) continue;
        try {
          items.add(new Result(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]),
              Integer.parseInt(fields[2]), Integer.parseInt(fields[3]), Integer.parseInt(fields[4])));
        } catch (RuntimeException ignored) { }
      }
      return items;
    } catch (IOException e) { throw new PersistenceException("Could not read results CSV.", e); }
  }

  public void saveAll(List<Result> items) {
    List<String> lines = items.stream().map(r -> r.getStudentId() + "," + r.getExamId() + ","
        + r.getSubjectId() + "," + r.getMarksObtained() + "," + r.getMaxMarks()).toList();
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      Files.write(file, lines);
    } catch (IOException e) { throw new PersistenceException("Could not write results CSV.", e); }
  }
}
