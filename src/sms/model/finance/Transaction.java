package sms.model.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
  private final int id;
  private final TransactionType type;
  private final BigDecimal amount;
  private final LocalDateTime timestamp;
  private final String description;
  private final Integer relatedPersonId;

  public Transaction(
      int id, TransactionType type, BigDecimal amount, String description, Integer personId) {
    this(id, type, amount, LocalDateTime.now(), description, personId);
  }

  public Transaction(
      int id,
      TransactionType type,
      BigDecimal amount,
      LocalDateTime timestamp,
      String description,
      Integer personId) {
    if (id <= 0 || type == null || amount == null || amount.signum() <= 0 || timestamp == null)
      throw new IllegalArgumentException(
          "A transaction needs an ID, type, positive amount, and timestamp.");
    this.id = id;
    this.type = type;
    this.amount = amount;
    this.timestamp = timestamp;
    this.description = description == null ? "" : description.trim();
    this.relatedPersonId = personId;
  }

  public int getId() {
    return id;
  }

  public TransactionType getType() {
    return type;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public String getDescription() {
    return description;
  }

  public Integer getRelatedPersonId() {
    return relatedPersonId;
  }

  public boolean isIncome() {
    return type == TransactionType.FEE_PAYMENT || type == TransactionType.DONATION;
  }
}
