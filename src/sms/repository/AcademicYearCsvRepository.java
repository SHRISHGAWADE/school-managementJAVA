package sms.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import sms.exception.PersistenceException;
import sms.model.AcademicYear;

public class AcademicYearCsvRepository {
  private final Path file;
  public AcademicYearCsvRepository(Path file) { this.file = Objects.requireNonNull(file); }

  public List<AcademicYear> findAll() {
    if (!Files.exists(file)) return List.of();
    List<AcademicYear> items = new ArrayList<>();
    try {
      for (String line : Files.readAllLines(file)) {
        String[] f = line.split(",", -1);
        if (f.length != 3) continue;
        try { items.add(new AcademicYear(f[0], LocalDate.parse(f[1]), LocalDate.parse(f[2]))); }
        catch (RuntimeException ignored) { }
      }
      return items;
    } catch (IOException e) { throw new PersistenceException("Could not read academic years CSV.", e); }
  }

  public void saveAll(List<AcademicYear> items) {
    List<String> lines = items.stream().map(y -> clean(y.name()) + "," + y.startDate() + "," + y.endDate()).toList();
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      Files.write(file, lines);
    } catch (IOException e) { throw new PersistenceException("Could not write academic years CSV.", e); }
  }

  private static String clean(String value) { return value.replace(",", " ").replace("\n", " "); }
}
