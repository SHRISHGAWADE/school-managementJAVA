package sms.repository;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import sms.exception.PersistenceException;
import sms.model.finance.*;

public class TransactionCsvRepository {
  private final Path file;

  public TransactionCsvRepository(Path file) {
    this.file = Objects.requireNonNull(file);
  }

  public void saveAll(List<Transaction> items) {
    write(
        items.stream()
            .map(
                t ->
                    String.join(
                        ",",
                        String.valueOf(t.getId()),
                        t.getType().name(),
                        t.getAmount().toPlainString(),
                        t.getTimestamp().toString(),
                        clean(t.getDescription()),
                        t.getRelatedPersonId() == null ? "" : t.getRelatedPersonId().toString()))
            .toList());
  }

  public List<Transaction> findAll() {
    List<Transaction> found = new ArrayList<>();
    for (String line : read()) {
      try {
        String[] p = line.split(",", -1);
        if (p.length != 6) continue;
        found.add(
            new Transaction(
                Integer.parseInt(p[0]),
                TransactionType.valueOf(p[1]),
                new BigDecimal(p[2]),
                LocalDateTime.parse(p[3]),
                p[4],
                p[5].isBlank() ? null : Integer.valueOf(p[5])));
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
      throw new PersistenceException("Could not read transactions CSV.", e);
    }
  }

  private void write(List<String> lines) {
    try {
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      Files.write(file, lines);
    } catch (IOException e) {
      throw new PersistenceException("Could not write transactions CSV.", e);
    }
  }

  private static String clean(String s) {
    return s.replace(",", " ").replace("\n", " ");
  }
}
