package sms.model;

import java.time.DayOfWeek;

public record DayPeriod(DayOfWeek day, int period) {
  public DayPeriod {
    if (day == null || period <= 0)
      throw new IllegalArgumentException("Day and a positive period are required.");
  }
}
