package sms.model;

import java.time.LocalDate;

/** A named school year, for example 2026-27. */
public record AcademicYear(String name, LocalDate startDate, LocalDate endDate) {
  public AcademicYear {
    if (name == null
        || name.isBlank()
        || startDate == null
        || endDate == null
        || !endDate.isAfter(startDate)) {
      throw new IllegalArgumentException("Academic year needs a name and valid start/end dates.");
    }
    name = name.trim();
  }
}
