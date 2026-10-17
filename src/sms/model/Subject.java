package sms.model;

public class Subject implements Identifiable {
  private final int id;
  private final String name;
  private final int maxMarks;
  private Integer teacherId;
  private final java.util.Set<AcademicClass> assignedClasses = new java.util.LinkedHashSet<>();

  public Subject(int id, String name, int maxMarks) {
    this(id, name, maxMarks, null);
  }

  public Subject(int id, String name, int maxMarks, Integer teacherId) {
    if (id <= 0 || name == null || name.isBlank() || maxMarks <= 0)
      throw new IllegalArgumentException(
          "Subject ID, name, and positive maximum marks are required.");
    this.id = id;
    this.name = name.trim();
    this.maxMarks = maxMarks;
    this.teacherId = teacherId;
  }

  @Override
  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getMaxMarks() {
    return maxMarks;
  }

  public Integer getTeacherId() {
    return teacherId;
  }

  public void assignTeacher(int teacherId) {
    if (teacherId <= 0) throw new IllegalArgumentException("Teacher ID must be positive.");
    this.teacherId = teacherId;
  }

  public void assignToClass(AcademicClass academicClass) {
    assignedClasses.add(
        java.util.Objects.requireNonNull(academicClass, "Academic class is required."));
  }

  public java.util.Set<AcademicClass> getAssignedClasses() {
    return java.util.Set.copyOf(assignedClasses);
  }
}
