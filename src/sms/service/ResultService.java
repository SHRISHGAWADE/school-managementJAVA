package sms.service;

import java.util.*;
import sms.model.*;

public class ResultService {
  private final Map<String, Result> results = new LinkedHashMap<>();

  public void recordMarks(Student student, Exam exam, Subject subject, int marks) {
    if (student == null || exam == null || subject == null)
      throw new IllegalArgumentException("Student, exam, and subject are required.");
    Result result =
        new Result(student.getId(), exam.getId(), subject.getId(), marks, subject.getMaxMarks());
    results.put(key(result), result);
  }

  public List<Result> getStudentResults(int studentId) {
    return results.values().stream().filter(r -> r.getStudentId() == studentId).toList();
  }

  public List<Result> getResultsByExamAndSubject(int examId, int subjectId) {
    return results.values().stream()
        .filter(r -> r.getExamId() == examId && r.getSubjectId() == subjectId)
        .toList();
  }

  public double getOverallPercentage(int studentId) {
    List<Result> all = getStudentResults(studentId);
    int max = all.stream().mapToInt(Result::getMaxMarks).sum();
    return max == 0 ? 0 : all.stream().mapToInt(Result::getMarksObtained).sum() * 100.0 / max;
  }

  public String getGrade(double percentage) {
    if (percentage >= 90) return "A+";
    if (percentage >= 75) return "A";
    if (percentage >= 60) return "B";
    if (percentage >= 40) return "C";
    return "F";
  }

  private static String key(Result r) {
    return r.getStudentId() + ":" + r.getExamId() + ":" + r.getSubjectId();
  }
}
