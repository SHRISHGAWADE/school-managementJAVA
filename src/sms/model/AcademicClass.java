package sms.model;

import java.util.Objects;

/** A class/section in a particular academic year. */
public record AcademicClass(AcademicYear academicYear, ClassSection classSection) {
  public AcademicClass {
    Objects.requireNonNull(academicYear, "Academic year is required.");
    Objects.requireNonNull(classSection, "Class section is required.");
  }
}
