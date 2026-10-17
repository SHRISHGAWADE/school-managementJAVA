package sms.model;

import java.util.regex.Pattern;

public abstract class Person implements Identifiable {
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private final int id;
  private String name;
  private String email;
  private String phone;

  protected Person(int id, String name, String email, String phone) {
    if (id <= 0) throw new IllegalArgumentException("ID must be positive.");
    this.id = id;
    setName(name);
    setEmail(email);
    setPhone(phone);
  }

  @Override
  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public final void setName(String name) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be empty.");
    this.name = name.trim();
  }

  public String getEmail() {
    return email;
  }

  public final void setEmail(String email) {
    if (email != null && !email.isBlank() && !EMAIL.matcher(email.trim()).matches())
      throw new IllegalArgumentException("Email is invalid.");
    this.email = email == null ? "" : email.trim();
  }

  public String getPhone() {
    return phone;
  }

  public final void setPhone(String phone) {
    this.phone = phone == null ? "" : phone.trim();
  }

  public abstract String getRole();
}
