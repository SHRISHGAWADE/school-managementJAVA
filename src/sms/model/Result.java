package sms.model;

public class Result {
  private final int studentId, examId, subjectId, marksObtained, maxMarks;

  public Result(int studentId, int examId, int subjectId, int marksObtained, int maxMarks) {
    if (studentId <= 0
        || examId <= 0
        || subjectId <= 0
        || maxMarks <= 0
        || marksObtained < 0
        || marksObtained > maxMarks)
      throw new IllegalArgumentException("Marks must be between 0 and the subject maximum.");
    this.studentId = studentId;
    this.examId = examId;
    this.subjectId = subjectId;
    this.marksObtained = marksObtained;
    this.maxMarks = maxMarks;
  }

  public int getStudentId() {
    return studentId;
  }

  public int getExamId() {
    return examId;
  }

  public int getSubjectId() {
    return subjectId;
  }

  public int getMarksObtained() {
    return marksObtained;
  }

  public int getMaxMarks() {
    return maxMarks;
  }

  public double getPercentage() {
    return marksObtained * 100.0 / maxMarks;
  }
}
