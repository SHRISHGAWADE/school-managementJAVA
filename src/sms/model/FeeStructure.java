package sms.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Category-based annual fees for one student. */
public final class FeeStructure {
  private final Map<FeeCategory, BigDecimal> amounts = new EnumMap<>(FeeCategory.class);

  public FeeStructure(Map<FeeCategory, BigDecimal> amounts) {
    Objects.requireNonNull(amounts, "Fee amounts are required.");
    amounts.forEach(this::setAmount);
  }

  public static FeeStructure tuitionOnly(BigDecimal amount) {
    return new FeeStructure(Map.of(FeeCategory.TUITION, amount));
  }

  public void setAmount(FeeCategory category, BigDecimal amount) {
    if (category == null || amount == null || amount.signum() < 0) {
      throw new IllegalArgumentException("Fee category and non-negative amount are required.");
    }
    amounts.put(category, amount);
  }

  public BigDecimal getAmount(FeeCategory category) {
    return amounts.getOrDefault(category, BigDecimal.ZERO);
  }

  public BigDecimal getTotal() {
    return amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public Map<FeeCategory, BigDecimal> getAmounts() {
    return Collections.unmodifiableMap(amounts);
  }
}
