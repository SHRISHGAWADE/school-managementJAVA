package sms.model;

/** Connects one teacher, subject, and class for a school year. */
public record TeachingAssignment(int teacherId, int subjectId, AcademicClass academicClass) {
  public TeachingAssignment {
    if (teacherId <= 0 || subjectId <= 0 || academicClass == null) {
      throw new IllegalArgumentException("Teacher, subject, and academic class are required.");
    }
  }
}
