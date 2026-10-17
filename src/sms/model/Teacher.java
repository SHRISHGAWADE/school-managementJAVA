package sms.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Teacher extends Person {
  private final Set<Integer> subjectIds = new LinkedHashSet<>();
  private final Set<AcademicClass> assignedClasses = new LinkedHashSet<>();
  private final List<SalaryPayment> salaryPayments = new ArrayList<>();
  private String department;
  private BigDecimal monthlySalary;
  private boolean active = true;

  public Teacher(int id, String name, String subject, BigDecimal monthlySalary) {
    this(id, name, "", "", subject, "General", monthlySalary, true);
  }

  public Teacher(
      int id,
      String name,
      String email,
      String phone,
      String subject,
      String department,
      BigDecimal monthlySalary,
      boolean active) {
    super(id, name, email, phone);
    this.department = requireText(department, "Department");
    setMonthlySalary(monthlySalary);
    this.active = active;
    if (subject != null && !subject.isBlank()) {}
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = requireText(department, "Department");
  }

  public BigDecimal getMonthlySalary() {
    return monthlySalary;
  }

  public final void setMonthlySalary(BigDecimal salary) {
    if (salary == null || salary.signum() < 0)
      throw new IllegalArgumentException("Monthly salary cannot be negative.");
    monthlySalary = salary;
  }

  public Set<Integer> getSubjectIds() {
    return Set.copyOf(subjectIds);
  }

  public void assignSubject(int subjectId) {
    if (subjectId <= 0) throw new IllegalArgumentException("Subject ID must be positive.");
    subjectIds.add(subjectId);
  }

  public void removeSubject(int subjectId) {
    subjectIds.remove(subjectId);
  }

  public void assignClass(AcademicClass academicClass) {
    assignedClasses.add(
        java.util.Objects.requireNonNull(academicClass, "Academic class is required."));
  }

  public Set<AcademicClass> getAssignedClasses() {
    return Set.copyOf(assignedClasses);
  }

  public void recordSalaryPayment(SalaryPayment payment) {
    salaryPayments.add(java.util.Objects.requireNonNull(payment));
  }

  public List<SalaryPayment> getSalaryPayments() {
    return List.copyOf(salaryPayments);
  }

  public boolean isActive() {
    return active;
  }

  public void deactivate() {
    active = false;
  }

  public void activate() {
    active = true;
  }

  @Override
  public String getRole() {
    return "TEACHER";
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException(field + " cannot be empty.");
    return value.trim();
  }
}
