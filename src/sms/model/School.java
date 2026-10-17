package sms.model;

import java.util.*;

public class School {
  private final String name;
  private final Map<Integer, Student> students = new LinkedHashMap<>();
  private final Map<Integer, Teacher> teachers = new LinkedHashMap<>();
  private final Map<Integer, Subject> subjects = new LinkedHashMap<>();
  private final Map<Integer, Exam> exams = new LinkedHashMap<>();
  private final Set<AcademicYear> academicYears = new LinkedHashSet<>();
  private final Set<AcademicClass> academicClasses = new LinkedHashSet<>();
  private final Set<TeachingAssignment> teachingAssignments = new LinkedHashSet<>();

  public School(String name) {
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("School name cannot be empty.");
    this.name = name.trim();
  }

  public String getName() {
    return name;
  }

  public void addStudent(Student s) {
    putUnique(students, s, "Student");
  }

  public void addTeacher(Teacher t) {
    putUnique(teachers, t, "Teacher");
  }

  public void addSubject(Subject s) {
    putUnique(subjects, s, "Subject");
  }

  public void addExam(Exam e) {
    putUnique(exams, e, "Exam");
  }

  public void addAcademicYear(AcademicYear academicYear) {
    if (!academicYears.add(Objects.requireNonNull(academicYear, "Academic year is required."))) {
      throw new IllegalArgumentException("Academic year already exists: " + academicYear.name());
    }
  }

  public void addAcademicClass(AcademicClass academicClass) {
    Objects.requireNonNull(academicClass, "Academic class is required.");
    if (!academicYears.contains(academicClass.academicYear())) {
      throw new IllegalArgumentException("Create the academic year before its classes.");
    }
    if (!academicClasses.add(academicClass)) {
      throw new IllegalArgumentException("Class already exists for this academic year.");
    }
  }

  public void addTeachingAssignment(TeachingAssignment assignment) {
    Objects.requireNonNull(assignment, "Teaching assignment is required.");
    if (!teachers.containsKey(assignment.teacherId())
        || !subjects.containsKey(assignment.subjectId())
        || !academicClasses.contains(assignment.academicClass())) {
      throw new IllegalArgumentException(
          "Teacher, subject, and academic class must exist before assignment.");
    }
    teachingAssignments.removeIf(
        item ->
            item.subjectId() == assignment.subjectId()
                && item.academicClass().equals(assignment.academicClass()));
    teachingAssignments.add(assignment);
  }

  public Optional<Student> findStudentById(int id) {
    return Optional.ofNullable(students.get(id));
  }

  public Optional<Teacher> findTeacherById(int id) {
    return Optional.ofNullable(teachers.get(id));
  }

  public Optional<Subject> findSubjectById(int id) {
    return Optional.ofNullable(subjects.get(id));
  }

  public Optional<Exam> findExamById(int id) {
    return Optional.ofNullable(exams.get(id));
  }

  public Collection<Student> getStudents() {
    return List.copyOf(students.values());
  }

  public Collection<Teacher> getTeachers() {
    return List.copyOf(teachers.values());
  }

  public Collection<Subject> getSubjects() {
    return List.copyOf(subjects.values());
  }

  public Collection<Exam> getExams() {
    return List.copyOf(exams.values());
  }

  public Set<AcademicYear> getAcademicYears() {
    return Set.copyOf(academicYears);
  }

  public Set<AcademicClass> getAcademicClasses() {
    return Set.copyOf(academicClasses);
  }

  public Set<TeachingAssignment> getTeachingAssignments() {
    return Set.copyOf(teachingAssignments);
  }

  private static <T extends Identifiable> void putUnique(
      Map<Integer, T> map, T value, String label) {
    Objects.requireNonNull(value, label + " is required.");
    if (map.putIfAbsent(value.getId(), value) != null)
      throw new IllegalArgumentException(label + " ID already exists: " + value.getId());
  }
}
