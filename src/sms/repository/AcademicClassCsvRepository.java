package sms.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import sms.exception.PersistenceException;
import sms.model.AcademicClass;
import sms.model.AcademicYear;
import sms.model.ClassSection;

public class AcademicClassCsvRepository {
  private final Path file;
  public AcademicClassCsvRepository(Path file) { this.file = Objects.requireNonNull(file); }

  public List<AcademicClass> findAll(List<AcademicYear> years) {
    if (!Files.exists(file)) return List.of();
    List<AcademicClass> items = new ArrayList<>();
    try {
      for (String line : Files.readAllLines(file)) {
        String[] f = line.split(",", -1);
        if (f.length != 3) continue;
        AcademicYear year = years.stream().filter(y -> y.name().equals(f[0])).findFirst().orElse(null);
        if (year == null) continue;
        try { items.add(new AcademicClass(year, new ClassSection(f[1], f[2]))); }
        catch (RuntimeException ignored) { }
      }
      return items;
    } catch (IOException e) { throw new PersistenceException("Could not read academic classes CSV.", e); }
  }

  public void saveAll(List<AcademicClass> items) {
    List<String> lines = items.stream().map(c -> String.join(",", clean(c.academicYear().name()),
        clean(c.classSection().grade()), clean(c.classSection().section()))).toList();
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      Files.write(file, lines);
    } catch (IOException e) { throw new PersistenceException("Could not write academic classes CSV.", e); }
  }

  private static String clean(String value) { return value.replace(",", " ").replace("\n", " "); }
}
