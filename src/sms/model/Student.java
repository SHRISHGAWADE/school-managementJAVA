package sms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Student extends Person {
  private final String admissionNumber;
  private AcademicClass currentAcademicClass;
  private final Map<AcademicYear, ClassSection> classHistory = new LinkedHashMap<>();
  private final LocalDate admissionDate;
  private FeeStructure feeStructure;
  private BigDecimal feesPaid = BigDecimal.ZERO;
  private StudentStatus status = StudentStatus.ACTIVE;

  public Student(int id, String admissionNumber, String name, String grade, BigDecimal totalFees) {
    this(
        id,
        admissionNumber,
        name,
        "",
        "",
        new ClassSection(grade, "A"),
        LocalDate.now(),
        totalFees,
        BigDecimal.ZERO,
        true);
  }

  public Student(
      int id,
      String admissionNumber,
      String name,
      String email,
      String phone,
      ClassSection classSection,
      LocalDate admissionDate,
      BigDecimal totalFees,
      BigDecimal feesPaid,
      boolean active) {
    super(id, name, email, phone);
    if (admissionNumber == null || admissionNumber.isBlank())
      throw new IllegalArgumentException("Admission number cannot be empty.");
    this.admissionNumber = admissionNumber.trim();
    this.admissionDate = admissionDate == null ? LocalDate.now() : admissionDate;
    this.currentAcademicClass =
        new AcademicClass(
            new AcademicYear(
                "Current", this.admissionDate.minusMonths(1), this.admissionDate.plusMonths(11)),
            Objects.requireNonNull(classSection, "Class section is required."));
    this.classHistory.put(currentAcademicClass.academicYear(), currentAcademicClass.classSection());
    setTotalFees(totalFees);
    if (feesPaid == null || feesPaid.signum() < 0 || feesPaid.compareTo(getTotalFees()) > 0)
      throw new IllegalArgumentException("Fees paid must be between zero and total fees.");
    this.feesPaid = feesPaid;
    this.status = active ? StudentStatus.ACTIVE : StudentStatus.INACTIVE;
  }

  public String getAdmissionNumber() {
    return admissionNumber;
  }

  public ClassSection getClassSection() {
    return currentAcademicClass.classSection();
  }

  public AcademicClass getCurrentAcademicClass() {
    return currentAcademicClass;
  }

  public Map<AcademicYear, ClassSection> getClassHistory() {
    return Map.copyOf(classHistory);
  }

  public String getGrade() {
    return currentAcademicClass.classSection().grade();
  }

  public void setClassSection(ClassSection classSection) {
    promoteTo(currentAcademicClass.academicYear(), classSection);
  }

  public void promoteTo(AcademicYear academicYear, ClassSection classSection) {
    AcademicClass next = new AcademicClass(academicYear, classSection);
    currentAcademicClass = next;
    classHistory.put(academicYear, classSection);
  }

  public LocalDate getAdmissionDate() {
    return admissionDate;
  }

  public final BigDecimal getTotalFees() {
    return feeStructure.getTotal();
  }

  public FeeStructure getFeeStructure() {
    return feeStructure;
  }

  public void setFeeAmount(FeeCategory category, BigDecimal amount) {
    if (category == null || amount == null || amount.signum() < 0) {
      throw new IllegalArgumentException("Fee category and non-negative amount are required.");
    }
    BigDecimal revisedTotal = getTotalFees().subtract(feeStructure.getAmount(category)).add(amount);
    if (revisedTotal.compareTo(feesPaid) < 0) {
      throw new IllegalArgumentException("Total fees cannot be below fees already paid.");
    }
    feeStructure.setAmount(category, amount);
  }

  public BigDecimal getFeesPaid() {
    return feesPaid;
  }

  public BigDecimal getRemainingFees() {
    return getTotalFees().subtract(feesPaid);
  }

  public final void setTotalFees(BigDecimal totalFees) {
    if (totalFees == null || totalFees.signum() < 0 || totalFees.compareTo(feesPaid) < 0)
      throw new IllegalArgumentException("Total fees cannot be negative or below fees paid.");
    this.feeStructure = FeeStructure.tuitionOnly(totalFees);
  }

  public void payFees(BigDecimal amount) {
    validatePositive(amount, "Fee payment");
    if (amount.compareTo(getRemainingFees()) > 0)
      throw new IllegalArgumentException("Fee payment exceeds the remaining fee balance.");
    feesPaid = feesPaid.add(amount);
  }

  public boolean isActive() {
    return status == StudentStatus.ACTIVE;
  }

  public StudentStatus getStatus() {
    return status;
  }

  public void deactivate() {
    status = StudentStatus.INACTIVE;
  }

  public void activate() {
    status = StudentStatus.ACTIVE;
  }

  public void graduate() {
    status = StudentStatus.GRADUATED;
  }

  public void transfer() {
    status = StudentStatus.TRANSFERRED;
  }

  /** Restores a persisted lifecycle state without exposing a general-purpose setter. */
  public void restoreStatus(StudentStatus restoredStatus) {
    status = Objects.requireNonNull(restoredStatus, "Student status is required.");
  }

  @Override
  public String getRole() {
    return "STUDENT";
  }

  @Override
  public String toString() {
    return admissionNumber + " - " + getName() + " (" + getClassSection() + ")";
  }

  private static void validatePositive(BigDecimal amount, String label) {
    if (amount == null || amount.signum() <= 0)
      throw new IllegalArgumentException(label + " must be positive.");
  }
}
