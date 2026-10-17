package sms.repository;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import sms.exception.PersistenceException;
import sms.model.*;

public class StudentCsvRepository implements Repository<Student> {
  private final Path file;

  public StudentCsvRepository(Path file) {
    this.file = Objects.requireNonNull(file);
  }

  @Override
  public void saveAll(List<Student> items) {
    write(
        items.stream()
            .map(
                s ->
                    String.join(
                        ",",
                        String.valueOf(s.getId()),
                        clean(s.getAdmissionNumber()),
                        clean(s.getName()),
                        clean(s.getEmail()),
                        clean(s.getPhone()),
                        clean(s.getClassSection().grade()),
                        clean(s.getClassSection().section()),
                        s.getAdmissionDate().toString(),
                        s.getTotalFees().toPlainString(),
                        s.getFeesPaid().toPlainString(),
                        String.valueOf(s.isActive()),
                        clean(s.getCurrentAcademicClass().academicYear().name()),
                        s.getCurrentAcademicClass().academicYear().startDate().toString(),
                        s.getCurrentAcademicClass().academicYear().endDate().toString(),
                        s.getStatus().name()))
            .toList());
  }

  @Override
  public List<Student> findAll() {
    List<Student> found = new ArrayList<>();
    for (String line : read()) {
      try {
        String[] p = line.split(",", -1);
        if (p.length != 11 && p.length != 15) continue;
        Student student =
            new Student(
                Integer.parseInt(p[0]),
                p[1],
                p[2],
                p[3],
                p[4],
                new ClassSection(p[5], p[6]),
                LocalDate.parse(p[7]),
                new BigDecimal(p[8]),
                new BigDecimal(p[9]),
                Boolean.parseBoolean(p[10]));
        if (p.length == 15) {
          student.promoteTo(
              new AcademicYear(p[11], LocalDate.parse(p[12]), LocalDate.parse(p[13])),
              new ClassSection(p[5], p[6]));
          student.restoreStatus(StudentStatus.valueOf(p[14]));
        }
        found.add(student);
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
      throw new PersistenceException("Could not read students CSV.", e);
    }
  }

  private void write(List<String> lines) {
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      Files.write(file, lines);
    } catch (IOException e) {
      throw new PersistenceException("Could not write students CSV.", e);
    }
  }

  private static String clean(String s) {
    return s.replace(",", " ").replace("\n", " ");
  }
}
