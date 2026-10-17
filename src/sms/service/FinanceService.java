package sms.service;

import java.math.BigDecimal;
import java.util.*;
import sms.exception.InsufficientFundsException;
import sms.model.ExpenseCategory;
import sms.model.SalaryPayment;
import sms.model.Student;
import sms.model.Teacher;
import sms.model.finance.*;

public class FinanceService {
  private final List<Transaction> transactions = new ArrayList<>();
  private int nextTransactionId = 1;

  public Transaction collectFee(Student student, BigDecimal amount) {
    Objects.requireNonNull(student);
    student.payFees(amount);
    return record(
        TransactionType.FEE_PAYMENT,
        amount,
        "Fee payment from " + student.getName(),
        student.getId());
  }

  public Transaction paySalary(Teacher teacher) {
    Objects.requireNonNull(teacher);
    if (!teacher.isActive()) throw new IllegalStateException("Cannot pay an inactive teacher.");
    Transaction transaction =
        outgoing(
            TransactionType.SALARY,
            teacher.getMonthlySalary(),
            "Salary paid to " + teacher.getName(),
            teacher.getId());
    teacher.recordSalaryPayment(
        new SalaryPayment(
            transaction.getId(), transaction.getAmount(), transaction.getTimestamp()));
    return transaction;
  }

  public Transaction addExpense(ExpenseCategory category, String purpose, BigDecimal amount) {
    if (category == null || purpose == null || purpose.isBlank())
      throw new IllegalArgumentException("Expense category and purpose are required.");
    return outgoing(TransactionType.EXPENSE, amount, category + ": " + purpose, null);
  }

  public Transaction receiveDonation(String donor, BigDecimal amount) {
    if (donor == null || donor.isBlank()) throw new IllegalArgumentException("Donor is required.");
    return record(TransactionType.DONATION, amount, "Donation from " + donor, null);
  }

  public BigDecimal getTotalIncome() {
    return total(true);
  }

  public BigDecimal getTotalExpenses() {
    return total(false);
  }

  public BigDecimal getTotalFeesCollected() {
    return totalByType(TransactionType.FEE_PAYMENT);
  }

  public BigDecimal getTotalSalaryPaid() {
    return totalByType(TransactionType.SALARY);
  }

  public BigDecimal getTotalOtherExpenses() {
    return totalByType(TransactionType.EXPENSE);
  }

  public BigDecimal getTotalDonations() {
    return totalByType(TransactionType.DONATION);
  }

  public BigDecimal getPendingFees(java.util.Collection<Student> students) {
    return students.stream()
        .map(Student::getRemainingFees)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public List<Transaction> getTransactionsByType(TransactionType type) {
    return getTransactions().stream().filter(transaction -> transaction.getType() == type).toList();
  }

  public BigDecimal getBalance() {
    return getTotalIncome().subtract(getTotalExpenses());
  }

  public List<Transaction> getTransactions() {
    return transactions.stream()
        .sorted(
            Comparator.comparing(Transaction::getTimestamp).thenComparingInt(Transaction::getId))
        .toList();
  }

  public void loadTransactions(List<Transaction> saved) {
    transactions.clear();
    transactions.addAll(saved);
    nextTransactionId = transactions.stream().mapToInt(Transaction::getId).max().orElse(0) + 1;
  }

  private Transaction outgoing(
      TransactionType type, BigDecimal amount, String description, Integer personId) {
    validate(amount);
    if (getBalance().compareTo(amount) < 0)
      throw new InsufficientFundsException("Insufficient school balance for " + description);
    return record(type, amount, description, personId);
  }

  private Transaction record(
      TransactionType type, BigDecimal amount, String description, Integer id) {
    validate(amount);
    Transaction t = new Transaction(nextTransactionId++, type, amount, description, id);
    transactions.add(t);
    return t;
  }

  private BigDecimal total(boolean income) {
    return transactions.stream()
        .filter(t -> t.isIncome() == income)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal totalByType(TransactionType type) {
    return transactions.stream()
        .filter(transaction -> transaction.getType() == type)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static void validate(BigDecimal amount) {
    if (amount == null || amount.signum() <= 0)
      throw new IllegalArgumentException("Amount must be positive.");
  }
}
