package sms.model;

public record ClassSection(String grade, String section) {
  public ClassSection {
    if (grade == null || grade.isBlank() || section == null || section.isBlank())
      throw new IllegalArgumentException("Grade and section are required.");
    grade = grade.trim();
    section = section.trim();
  }

  @Override
  public String toString() {
    return grade + "-" + section;
  }
}
