package sms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalaryPayment(int transactionId, BigDecimal amount, LocalDateTime paidAt) {
  public SalaryPayment {
    if (transactionId <= 0 || amount == null || amount.signum() <= 0 || paidAt == null) {
      throw new IllegalArgumentException(
          "Salary payment needs a transaction, amount, and timestamp.");
    }
  }
}
