package sms.model;

import java.time.LocalDate;

public class Exam implements Identifiable {
  private final int id;
  private final String name;
  private final LocalDate examDate;

  public Exam(int id, String name, LocalDate examDate) {
    if (id <= 0 || name == null || name.isBlank() || examDate == null)
      throw new IllegalArgumentException("Exam ID, name, and date are required.");
    this.id = id;
    this.name = name.trim();
    this.examDate = examDate;
  }

  @Override
  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public LocalDate getExamDate() {
    return examDate;
  }
}
