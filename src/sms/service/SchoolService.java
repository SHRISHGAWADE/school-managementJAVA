package sms.service;

import java.util.*;
import sms.exception.EntityNotFoundException;
import sms.model.*;

public class SchoolService {
  private final School school;

  public SchoolService(School school) {
    this.school = Objects.requireNonNull(school);
  }

  public void enrollStudent(Student s) {
    school.addStudent(s);
  }

  public void hireTeacher(Teacher t) {
    school.addTeacher(t);
  }

  public void addSubject(Subject s) {
    school.addSubject(s);
  }

  public void createExam(Exam e) {
    school.addExam(e);
  }

  public void createAcademicYear(AcademicYear academicYear) {
    school.addAcademicYear(academicYear);
  }

  public void createClass(AcademicClass academicClass) {
    school.addAcademicClass(academicClass);
  }

  public void removeStudent(int id) {
    getStudent(id).deactivate();
  }

  public void removeTeacher(int id) {
    getTeacher(id).deactivate();
  }

  public void assignTeacherToSubject(int teacherId, int subjectId) {
    Teacher teacher = getTeacher(teacherId);
    Subject subject = getSubject(subjectId);
    teacher.assignSubject(subjectId);
    subject.assignTeacher(teacherId);
  }

  public void assignTeacherToSubjectForClass(
      int teacherId, int subjectId, AcademicClass academicClass) {
    Teacher teacher = getTeacher(teacherId);
    Subject subject = getSubject(subjectId);
    if (!teacher.isActive()) throw new IllegalStateException("Cannot assign an inactive teacher.");
    teacher.assignSubject(subjectId);
    teacher.assignClass(academicClass);
    subject.assignTeacher(teacherId);
    subject.assignToClass(academicClass);
    school.addTeachingAssignment(new TeachingAssignment(teacherId, subjectId, academicClass));
  }

  public List<Subject> getSubjectsTaughtByTeacher(int teacherId) {
    Teacher teacher = getTeacher(teacherId);
    return teacher.getSubjectIds().stream().map(this::getSubject).toList();
  }

  public Optional<Teacher> getTeacherForSubjectAndClass(
      int subjectId, AcademicClass academicClass) {
    return school.getTeachingAssignments().stream()
        .filter(item -> item.subjectId() == subjectId && item.academicClass().equals(academicClass))
        .map(item -> getTeacher(item.teacherId()))
        .findFirst();
  }

  public void promoteStudent(int studentId, AcademicClass nextClass) {
    if (!school.getAcademicClasses().contains(nextClass)) {
      throw new IllegalArgumentException("Create the destination academic class before promotion.");
    }
    getStudent(studentId).promoteTo(nextClass.academicYear(), nextClass.classSection());
  }

  public Student getStudent(int id) {
    return school
        .findStudentById(id)
        .orElseThrow(() -> new EntityNotFoundException("Student not found: " + id));
  }

  public Teacher getTeacher(int id) {
    return school
        .findTeacherById(id)
        .orElseThrow(() -> new EntityNotFoundException("Teacher not found: " + id));
  }

  public Subject getSubject(int id) {
    return school
        .findSubjectById(id)
        .orElseThrow(() -> new EntityNotFoundException("Subject not found: " + id));
  }

  public Exam getExam(int id) {
    return school
        .findExamById(id)
        .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + id));
  }

  public List<Student> searchStudentsByName(String q) {
    return school.getStudents().stream()
        .filter(s -> s.getName().toLowerCase().contains(normalize(q)))
        .toList();
  }

  public List<Teacher> searchTeachersByName(String q) {
    return school.getTeachers().stream()
        .filter(t -> t.getName().toLowerCase().contains(normalize(q)))
        .toList();
  }

  public Collection<Student> getStudents() {
    return school.getStudents();
  }

  public Collection<Teacher> getTeachers() {
    return school.getTeachers();
  }

  public Collection<Subject> getSubjects() {
    return school.getSubjects();
  }

  public Collection<Exam> getExams() {
    return school.getExams();
  }

  public Set<AcademicYear> getAcademicYears() {
    return school.getAcademicYears();
  }

  public Set<AcademicClass> getAcademicClasses() {
    return school.getAcademicClasses();
  }

  public Set<TeachingAssignment> getTeachingAssignments() {
    return school.getTeachingAssignments();
  }

  public boolean hasTeachingAssignment(int teacherId, int subjectId, AcademicClass academicClass) {
    return school
        .getTeachingAssignments()
        .contains(new TeachingAssignment(teacherId, subjectId, academicClass));
  }

  private static String normalize(String q) {
    if (q == null || q.isBlank()) throw new IllegalArgumentException("Search text is required.");
    return q.trim().toLowerCase();
  }
}
