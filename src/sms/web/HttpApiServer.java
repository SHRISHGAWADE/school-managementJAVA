package sms.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import sms.exception.EntityNotFoundException;
import sms.exception.InsufficientFundsException;
import sms.model.*;
import sms.model.finance.Transaction;
import sms.model.finance.TransactionType;
import sms.repository.StudentCsvRepository;
import sms.repository.TeacherCsvRepository;
import sms.repository.TransactionCsvRepository;
import sms.repository.AttendanceCsvRepository;
import sms.repository.ExamCsvRepository;
import sms.repository.SubjectCsvRepository;
import sms.repository.ResultCsvRepository;
import sms.repository.TimetableCsvRepository;
import sms.repository.AcademicYearCsvRepository;
import sms.repository.AcademicClassCsvRepository;
import sms.service.AttendanceService;
import sms.service.FinanceService;
import sms.service.ResultService;
import sms.service.SchoolService;
import sms.service.TimetableService;

/**
 * Lightweight, zero-dependency HTTP REST API server for the School Management System.
 * Read-only in Stage 1; reuses existing services and models.
 */
public final class HttpApiServer {
  private final HttpServer server;
  private final ExecutorService executor;
  private final School school;
  private final SchoolService schoolService;
  private final FinanceService financeService;
  private final AttendanceService attendanceService;
  private final AttendanceCsvRepository attendanceRepository;
  private final ExamCsvRepository examRepository;
  private final SubjectCsvRepository subjectRepository;
  private final ResultCsvRepository resultRepository;
  private final ResultService resultService;
  private final TimetableService timetableService;
  private final TimetableCsvRepository timetableRepository;
  private final AcademicYearCsvRepository academicYearRepository;
  private final AcademicClassCsvRepository academicClassRepository;
  private final StudentCsvRepository studentRepository;
  private final TeacherCsvRepository teacherRepository;
  private final TransactionCsvRepository transactionRepository;
  private final Path dataDir;
  private boolean running = false;

  public HttpApiServer(int preferredPort, Path dataDir, boolean fallbackIfOccupied)
      throws IOException {
    this.dataDir = Objects.requireNonNull(dataDir);
    this.school = new School("Green Valley High School");
    this.schoolService = new SchoolService(school);
    this.financeService = new FinanceService();
    this.attendanceService = new AttendanceService();
    this.resultService = new ResultService();
    this.timetableService = new TimetableService();
    this.studentRepository = new StudentCsvRepository(dataDir.resolve("students.csv"));
    this.teacherRepository = new TeacherCsvRepository(dataDir.resolve("teachers.csv"));
    this.transactionRepository =
        new TransactionCsvRepository(dataDir.resolve("transactions.csv"));
    this.attendanceRepository = new AttendanceCsvRepository(dataDir.resolve("attendance.csv"));
    this.examRepository = new ExamCsvRepository(dataDir.resolve("exams.csv"));
    this.subjectRepository = new SubjectCsvRepository(dataDir.resolve("subjects.csv"));
    this.resultRepository = new ResultCsvRepository(dataDir.resolve("results.csv"));
    this.timetableRepository = new TimetableCsvRepository(dataDir.resolve("timetable.csv"));
    this.academicYearRepository = new AcademicYearCsvRepository(dataDir.resolve("academic-years.csv"));
    this.academicClassRepository = new AcademicClassCsvRepository(dataDir.resolve("academic-classes.csv"));

    // Populate services from persistent CSV files
    studentRepository.findAll().forEach(schoolService::enrollStudent);
    teacherRepository.findAll().forEach(schoolService::hireTeacher);
    financeService.loadTransactions(transactionRepository.findAll());
    attendanceService.loadAttendance(attendanceRepository.findAll());
    examRepository.findAll().forEach(schoolService::createExam);
    subjectRepository.findAll().forEach(schoolService::addSubject);
    resultService.loadResults(resultRepository.findAll());
    timetableService.loadEntries(timetableRepository.findAll());
    academicYearRepository.findAll().forEach(schoolService::createAcademicYear);
    academicClassRepository.findAll(new ArrayList<>(schoolService.getAcademicYears()))
        .forEach(schoolService::createClass);

    HttpServer s;
    try {
      s = HttpServer.create(new InetSocketAddress(preferredPort), 0);
    } catch (BindException e) {
      if (fallbackIfOccupied) {
        s = HttpServer.create(new InetSocketAddress(0), 0);
      } else {
        throw e;
      }
    }
    this.server = s;
    this.executor = Executors.newSingleThreadExecutor();
    this.server.setExecutor(executor);
    configureRoutes();
  }

  public HttpApiServer(int preferredPort, Path dataDir) throws IOException {
    this(preferredPort, dataDir, true);
  }

  public HttpApiServer(int preferredPort) throws IOException {
    this(preferredPort, Path.of("data"), true);
  }

  public HttpApiServer() throws IOException {
    this(8080, Path.of("data"), true);
  }

  public synchronized void start() {
    if (!running) {
      server.start();
      running = true;
    }
  }

  public synchronized void stop(int delaySeconds) {
    if (running) {
      server.stop(delaySeconds);
      executor.shutdown();
      running = false;
    }
  }

  public synchronized void stop() {
    stop(0);
  }

  public int getPort() {
    return server.getAddress().getPort();
  }

  public String getBaseUrl() {
    return "http://localhost:" + getPort();
  }

  public SchoolService getSchoolService() {
    return schoolService;
  }

  public FinanceService getFinanceService() {
    return financeService;
  }

  private void configureRoutes() {
    server.createContext("/", new RootHandler());
    server.createContext("/api/health", new HealthHandler());
    server.createContext("/api/dashboard/stats", new DashboardHandler());
    server.createContext("/api/students", new StudentsHandler());
    server.createContext("/api/teachers", new TeachersHandler());
    server.createContext("/api/finance/transactions", new TransactionsHandler());
    server.createContext("/api/finance/transactions/", new TransactionsHandler());
    server.createContext("/api/attendance", new AttendanceHandler());
    server.createContext("/api/results", new ResultsHandler());
    server.createContext("/api/academics/exams", new AcademicsHandler());
    server.createContext("/api/academics/subjects", new AcademicsHandler());
    server.createContext("/api/academics/years", new AcademicsHandler());
    server.createContext("/api/academics/classes", new AcademicsHandler());
    server.createContext("/api/timetable", new TimetableHandler());
  }

  // ==========================================
  // HTTP ROUTE HANDLERS
  // ==========================================

  private final class RootHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (handleCors(exchange)) return;
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendError(exchange, 405, "Method Not Allowed", "Only GET is supported.");
        return;
      }

      String path = exchange.getRequestURI().getPath();

      // Handle root path
      if ("/".equals(path)) {
        String accept = exchange.getRequestHeaders().getFirst("Accept");
        if (accept != null && accept.contains("text/html")) {
          serveStaticFile(exchange, "index.html");
          return;
        }
        // Default API JSON response for API clients / smoke tests
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "UP");
        res.put("message", "School Management System HTTP API (Stage 1 Read-Only)");
        res.put("port", getPort());
        sendJson(exchange, 200, res);
        return;
      }

      // Handle static assets under web/
      if (!path.startsWith("/api/")) {
        String relPath = path.startsWith("/") ? path.substring(1) : path;
        if (serveStaticFile(exchange, relPath)) {
          return;
        }
      }

      sendError(exchange, 404, "Not Found", "Resource not found: " + path);
    }
  }

  private boolean serveStaticFile(HttpExchange exchange, String relativePath) throws IOException {
    Path webDir = Path.of("web").toAbsolutePath().normalize();
    Path filePath = webDir.resolve(relativePath).normalize();

    // Security: prevent directory traversal
    if (!filePath.startsWith(webDir)) {
      sendError(exchange, 403, "Forbidden", "Access denied.");
      return true;
    }

    if (!java.nio.file.Files.exists(filePath) || java.nio.file.Files.isDirectory(filePath)) {
      return false;
    }

    byte[] bytes = java.nio.file.Files.readAllBytes(filePath);
    String contentType = determineContentType(filePath.getFileName().toString());
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
    return true;
  }

  private static String determineContentType(String fileName) {
    String lower = fileName.toLowerCase();
    if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
    if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
    if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript; charset=UTF-8";
    if (lower.endsWith(".svg")) return "image/svg+xml; charset=UTF-8";
    if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".ico")) return "image/x-icon";
    return "application/octet-stream";
  }

  private final class HealthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (handleCors(exchange)) return;
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendError(exchange, 405, "Method Not Allowed", "Only GET is supported.");
        return;
      }
      Map<String, Object> res = new LinkedHashMap<>();
      res.put("status", "UP");
      res.put("school", school.getName());
      res.put("port", getPort());
      sendJson(exchange, 200, res);
    }
  }

  private final class DashboardHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (handleCors(exchange)) return;
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendError(exchange, 405, "Method Not Allowed", "Only GET is supported.");
        return;
      }
      Map<String, Object> stats = new LinkedHashMap<>();
      stats.put("schoolName", school.getName());
      stats.put("studentCount", schoolService.getStudents().size());
      stats.put("teacherCount", schoolService.getTeachers().size());
      stats.put("totalIncome", financeService.getTotalIncome());
      stats.put("totalExpenses", financeService.getTotalExpenses());
      stats.put("balance", financeService.getBalance());
      stats.put("totalFeesCollected", financeService.getTotalFeesCollected());
      stats.put("pendingFees", financeService.getPendingFees(schoolService.getStudents()));
      stats.put("totalSalaryPaid", financeService.getTotalSalaryPaid());
      stats.put("totalOtherExpenses", financeService.getTotalOtherExpenses());
      stats.put("totalDonations", financeService.getTotalDonations());
      stats.put("transactionCount", financeService.getTransactions().size());
      sendJson(exchange, 200, stats);
    }
  }

  private final class StudentsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (handleCors(exchange)) return;
      String method = exchange.getRequestMethod().toUpperCase();
      String path = exchange.getRequestURI().getPath();
      String query = exchange.getRequestURI().getRawQuery();
      Map<String, String> params = parseQueryParams(query);

      try {
        switch (method) {
          case "GET" -> {
            if ("/api/students".equals(path) || "/api/students/".equals(path)) {
              String search = params.get("q");
              Collection<Student> list;
              if (search != null && !search.isBlank()) {
                list = schoolService.searchStudentsByName(search);
              } else {
                list = schoolService.getStudents();
              }
              String statusFilter = params.get("status");
              if (statusFilter != null && !statusFilter.isBlank()) {
                list = list.stream()
                    .filter(s -> s.getStatus().name().equalsIgnoreCase(statusFilter.trim()))
                    .toList();
              }
              List<Map<String, Object>> dtos = list.stream().map(HttpApiServer::studentDto).toList();
              sendJson(exchange, 200, dtos);
            } else if (path.startsWith("/api/students/")) {
              int studentId = parseIdFromPath(path, "/api/students/");
              Student s = schoolService.getStudent(studentId);
              sendJson(exchange, 200, studentDto(s));
            } else {
              sendError(exchange, 404, "Not Found", "Resource not found: " + path);
            }
          }
          case "POST" -> {
            if (!"/api/students".equals(path) && !"/api/students/".equals(path)) {
              sendError(exchange, 404, "Not Found", "Resource not found: " + path);
              return;
            }
            Map<String, Object> body = readJsonBody(exchange);
            int studentId = optInt(body, "id", 0);
            if (studentId <= 0) {
              studentId = schoolService.getStudents().stream().mapToInt(Student::getId).max().orElse(0) + 1;
            }
            String name = optString(body, "name", "");
            String admissionNumber = optString(body, "admissionNumber", "");
            String grade = optString(body, "grade", "");
            String section = optString(body, "section", "A");
            String email = optString(body, "email", "");
            String phone = optString(body, "phone", "");
            BigDecimal totalFees = optBigDecimal(body, "totalFees", BigDecimal.ZERO);
            String dateStr = optString(body, "admissionDate", "");
            LocalDate admissionDate = dateStr.isBlank() ? LocalDate.now() : LocalDate.parse(dateStr);

            Student student =
                new Student(
                    studentId,
                    admissionNumber,
                    name,
                    email,
                    phone,
                    new ClassSection(grade, section),
                    admissionDate,
                    totalFees,
                    BigDecimal.ZERO,
                    true);

            schoolService.enrollStudent(student);
            studentRepository.saveAll(new ArrayList<>(schoolService.getStudents()));
            sendJson(exchange, 201, studentDto(student));
          }
          case "PUT" -> {
            if ("/api/students".equals(path) || "/api/students/".equals(path)) {
              sendError(
                  exchange,
                  405,
                  "Method Not Allowed",
                  "PUT is not supported on the collection root. Specify a student ID.");
              return;
            }
            if (!path.startsWith("/api/students/")) {
              sendError(exchange, 404, "Not Found", "Resource not found: " + path);
              return;
            }
            int studentId = parseIdFromPath(path, "/api/students/");
            Student student = schoolService.getStudent(studentId);
            Map<String, Object> body = readJsonBody(exchange);

            if (body.containsKey("name")) student.setName(optString(body, "name", ""));
            if (body.containsKey("email")) student.setEmail(optString(body, "email", ""));
            if (body.containsKey("phone")) student.setPhone(optString(body, "phone", ""));
            if (body.containsKey("grade") || body.containsKey("section")) {
              String grade = optString(body, "grade", student.getGrade());
              String section = optString(body, "section", student.getClassSection().section());
              student.setClassSection(new ClassSection(grade, section));
            }
            if (body.containsKey("totalFees")) {
              BigDecimal revised = optBigDecimal(body, "totalFees", student.getTotalFees());
              student.setTotalFees(revised);
            }

            studentRepository.saveAll(new ArrayList<>(schoolService.getStudents()));
            sendJson(exchange, 200, studentDto(student));
          }
          case "DELETE" -> {
            if ("/api/students".equals(path) || "/api/students/".equals(path)) {
              sendError(
                  exchange,
                  405,
                  "Method Not Allowed",
                  "DELETE is not supported on the collection root. Specify a student ID.");
              return;
            }
            if (!path.startsWith("/api/students/")) {
              sendError(exchange, 404, "Not Found", "Resource not found: " + path);
              return;
            }
            int studentId = parseIdFromPath(path, "/api/students/");
            schoolService.removeStudent(studentId);
            studentRepository.saveAll(new ArrayList<>(schoolService.getStudents()));
            Student student = schoolService.getStudent(studentId);
            sendJson(exchange, 200, studentDto(student));
          }
          case "PATCH" -> {
            if (path.matches("^/api/students/\\d+/activate/?$")) {
              int studentId = Integer.parseInt(path.replaceAll("\\D+", ""));
              Student student = schoolService.getStudent(studentId);
              student.activate();
              studentRepository.saveAll(new ArrayList<>(schoolService.getStudents()));
              sendJson(exchange, 200, studentDto(student));
            } else {
              sendError(exchange, 404, "Not Found", "Resource not found: " + path);
            }
          }
          default -> sendError(
              exchange,
              405,
              "Method Not Allowed",
              "Method " + method + " is not supported for this endpoint.");
        }
      } catch (EntityNotFoundException e) {
        sendError(exchange, 404, "Not Found", e.getMessage());
      } catch (IllegalArgumentException e) {
        sendError(exchange, 400, "Bad Request", e.getMessage());
      } catch (IllegalStateException e) {
        sendError(exchange, 409, "Conflict", e.getMessage());
      } catch (Exception e) {
        sendError(exchange, 500, "Internal Server Error", e.getMessage());
      }
    }
  }

  private final class TeachersHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (handleCors(exchange)) return;
      String method = exchange.getRequestMethod().toUpperCase();
      String path = exchange.getRequestURI().getPath();
      String query = exchange.getRequestURI().getRawQuery();
      Map<String, String> params = parseQueryParams(query);

      try {
        switch (method) {
          case "GET" -> {
            if ("/api/teachers".equals(path) || "/api/teachers/".equals(path)) {
              String search = params.get("q");
              Collection<Teacher> list;
              if (search != null && !search.isBlank()) {
                list = schoolService.searchTeachersByName(search);
              } else {
                list = schoolService.getTeachers();
              }
              String activeFilter = params.get("active");
              if (activeFilter != null && !activeFilter.isBlank()) {
                boolean active = Boolean.parseBoolean(activeFilter.trim());
                list = list.stream().filter(t -> t.isActive() == active).toList();
              }
              List<Map<String, Object>> dtos = list.stream().map(HttpApiServer::teacherDto).toList();
              sendJson(exchange, 200, dtos);
            } else if (path.startsWith("/api/teachers/")) {
              int teacherId = parseIdFromPath(path, "/api/teachers/");
              Teacher t = schoolService.getTeacher(teacherId);
              sendJson(exchange, 200, teacherDto(t));
            } else {
              sendError(exchange, 404, "Not Found", "Resource not found: " + path);
            }
          }
          case "POST" -> {
            if (!"/api/teachers".equals(path) && !"/api/teachers/".equals(path)) {
              sendError(exchange, 404, "Not Found", "Resource not found: " + path);
              return;
            }
            Map<String, Object> body = readJsonBody(exchange);
            int teacherId = optInt(body, "id", 0);
            if (teacherId <= 0) {
              teacherId = schoolService.getTeachers().stream().mapToInt(Teacher::getId).max().orElse(0) + 1;
            }
            String name = optString(body, "name", "");
            String department = optString(body, "department", "General");
            BigDecimal monthlySalary = optBigDecimal(body, "monthlySalary", BigDecimal.ZERO);
            String email = optString(body, "email", "");
            String phone = optString(body, "phone", "");

            Teacher teacher =
                new Teacher(
                    teacherId,
                    name,
                    email,
                    phone,
                    "",
                    department,
                    monthlySalary,
                    true);

            schoolService.hireTeacher(teacher);
            teacherRepository.saveAll(new ArrayList<>(schoolService.getTeachers()));
            sendJson(exchange, 201, teacherDto(teacher));
          }
          case "PUT" -> {
            if ("/api/teachers".equals(path) || "/api/teachers/".equals(path)) {
              sendError(
                  exchange,
                  405,
                  "Method Not Allowed",
                  "PUT is not supported on the collection root. Specify a teacher ID.");
              return;
            }
            if (!path.startsWith("/api/teachers/")) {
              sendError(exchange, 404, "Not Found", "Resource not found: " + path);
              return;
            }
            int teacherId = parseIdFromPath(path, "/api/teachers/");
            Teacher teacher = schoolService.getTeacher(teacherId);
            Map<String, Object> body = readJsonBody(exchange);

            if (body.containsKey("name")) teacher.setName(optString(body, "name", ""));
            if (body.containsKey("email")) teacher.setEmail(optString(body, "email", ""));
            if (body.containsKey("phone")) teacher.setPhone(optString(body, "phone", ""));
            if (body.containsKey("department")) teacher.setDepartment(optString(body, "department", ""));
            if (body.containsKey("monthlySalary")) {
              BigDecimal salary = optBigDecimal(body, "monthlySalary", teacher.getMonthlySalary());
              teacher.setMonthlySalary(salary);
            }

            teacherRepository.saveAll(new ArrayList<>(schoolService.getTeachers()));
            sendJson(exchange, 200, teacherDto(teacher));
          }
          case "DELETE" -> {
            if ("/api/teachers".equals(path) || "/api/teachers/".equals(path)) {
              sendError(
                  exchange,
                  405,
                  "Method Not Allowed",
                  "DELETE is not supported on the collection root. Specify a teacher ID.");
              return;
            }
            if (!path.startsWith("/api/teachers/")) {
              sendError(exchange, 404, "Not Found", "Resource not found: " + path);
              return;
            }
            int teacherId = parseIdFromPath(path, "/api/teachers/");
            schoolService.removeTeacher(teacherId);
            teacherRepository.saveAll(new ArrayList<>(schoolService.getTeachers()));
            Teacher teacher = schoolService.getTeacher(teacherId);
            sendJson(exchange, 200, teacherDto(teacher));
          }
          case "PATCH" -> {
            if (path.matches("^/api/teachers/\\d+/activate/?$")) {
              int teacherId = Integer.parseInt(path.replaceAll("\\D+", ""));
              Teacher teacher = schoolService.getTeacher(teacherId);
              teacher.activate();
              teacherRepository.saveAll(new ArrayList<>(schoolService.getTeachers()));
              sendJson(exchange, 200, teacherDto(teacher));
            } else {
              sendError(exchange, 404, "Not Found", "Resource not found: " + path);
            }
          }
          default -> sendError(
              exchange,
              405,
              "Method Not Allowed",
              "Method " + method + " is not supported for this endpoint.");
        }
      } catch (EntityNotFoundException e) {
        sendError(exchange, 404, "Not Found", e.getMessage());
      } catch (IllegalArgumentException e) {
        sendError(exchange, 400, "Bad Request", e.getMessage());
      } catch (IllegalStateException e) {
        sendError(exchange, 409, "Conflict", e.getMessage());
      } catch (Exception e) {
        sendError(exchange, 500, "Internal Server Error", e.getMessage());
      }
    }
  }

  private final class TransactionsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (handleCors(exchange)) return;
      String path = exchange.getRequestURI().getPath();
      if (!"/api/finance/transactions".equals(path) && !"/api/finance/transactions/".equals(path)) {
        sendError(exchange, 404, "Not Found", "Resource not found: " + path);
        return;
      }
      try {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
          Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
          String typeStr = params.get("type");
          List<Transaction> list;
          if (typeStr != null && !typeStr.isBlank()) {
            TransactionType type = TransactionType.valueOf(typeStr.trim().toUpperCase());
            list = financeService.getTransactionsByType(type);
          } else {
            list = financeService.getTransactions();
          }
          List<Map<String, Object>> dtos = list.stream().map(HttpApiServer::transactionDto).toList();
          sendJson(exchange, 200, dtos);
        } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
          Map<String, Object> body = readJsonBody(exchange);
          TransactionType type = TransactionType.valueOf(optString(body, "type", "").toUpperCase());
          BigDecimal amount = optBigDecimal(body, "amount", null);
          Transaction transaction;
          switch (type) {
            case FEE_PAYMENT -> {
              Student student = schoolService.getStudent(optInt(body, "studentId", 0));
              transaction = financeService.collectFee(student, amount);
              studentRepository.saveAll(new ArrayList<>(schoolService.getStudents()));
            }
            case SALARY -> {
              Teacher teacher = schoolService.getTeacher(optInt(body, "teacherId", 0));
              if (amount != null && amount.compareTo(teacher.getMonthlySalary()) != 0)
                throw new IllegalArgumentException("Salary amount must match the teacher's monthly salary.");
              transaction = financeService.paySalary(teacher);
              teacherRepository.saveAll(new ArrayList<>(schoolService.getTeachers()));
            }
            case EXPENSE -> transaction = financeService.addExpense(
                ExpenseCategory.valueOf(optString(body, "category", "OTHER").toUpperCase()),
                optString(body, "purpose", ""), amount);
            case DONATION -> transaction = financeService.receiveDonation(
                optString(body, "donor", ""), amount);
            default -> throw new IllegalArgumentException("Unsupported transaction type: " + type);
          }
          transactionRepository.saveAll(financeService.getTransactions());
          sendJson(exchange, 201, transactionDto(transaction));
        } else {
          sendError(exchange, 405, "Method Not Allowed", "Only GET and POST are supported.");
        }
      } catch (EntityNotFoundException e) {
        sendError(exchange, 404, "Not Found", e.getMessage());
      } catch (InsufficientFundsException e) {
        sendError(exchange, 409, "Conflict", e.getMessage());
      } catch (IllegalArgumentException e) {
        sendError(exchange, 400, "Bad Request", e.getMessage());
      } catch (IllegalStateException e) {
        sendError(exchange, 409, "Conflict", e.getMessage());
      } catch (Exception e) {
        sendError(exchange, 500, "Internal Server Error", e.getMessage());
      }
    }
  }

  private final class AttendanceHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (handleCors(exchange)) return;
      try {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
          Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
          LocalDate date = params.containsKey("date")
              ? LocalDate.parse(params.get("date"))
              : LocalDate.now();
          Map<Integer, AttendanceRecord> todaysRecords = new HashMap<>();
          attendanceService.getAttendanceByDate(date)
              .forEach(record -> todaysRecords.put(record.studentId(), record));
          List<Map<String, Object>> roster = schoolService.getStudents().stream()
              .filter(Student::isActive)
              .sorted(Comparator.comparing(Student::getName, String.CASE_INSENSITIVE_ORDER))
              .map(student -> attendanceDto(student, date, todaysRecords.get(student.getId())))
              .toList();
          sendJson(exchange, 200, roster);
        } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
          Map<String, Object> body = readJsonBody(exchange);
          Student student = schoolService.getStudent(optInt(body, "studentId", 0));
          if (!student.isActive()) throw new IllegalArgumentException("Attendance can only be marked for active students.");
          LocalDate date = LocalDate.parse(optString(body, "date", ""));
          AttendanceStatus status = AttendanceStatus.valueOf(optString(body, "status", "").toUpperCase());
          attendanceService.markAttendance(student, date, status);
          attendanceRepository.saveAll(attendanceService.getAllAttendance());
          sendJson(exchange, 201, attendanceDto(student, date, attendanceService.getAttendanceByDate(date)
              .stream().filter(record -> record.studentId() == student.getId()).findFirst().orElseThrow()));
        } else {
          sendError(exchange, 405, "Method Not Allowed", "Only GET and POST are supported.");
        }
      } catch (EntityNotFoundException e) {
        sendError(exchange, 404, "Not Found", e.getMessage());
      } catch (IllegalArgumentException e) {
        sendError(exchange, 400, "Bad Request", e.getMessage());
      } catch (IllegalStateException e) {
        sendError(exchange, 409, "Conflict", e.getMessage());
      } catch (Exception e) {
        sendError(exchange, 500, "Internal Server Error", e.getMessage());
      }
    }
  }

  private final class AcademicsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (handleCors(exchange)) return;
      String path = exchange.getRequestURI().getPath();
      try {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
            && !"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
          sendError(exchange, 405, "Method Not Allowed", "Only GET and POST are supported.");
          return;
        }
        if (path.startsWith("/api/academics/exams")) {
          if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, schoolService.getExams().stream().map(HttpApiServer::examDto).toList());
          } else {
            Map<String, Object> body = readJsonBody(exchange);
            int id = optInt(body, "id", schoolService.getExams().stream().mapToInt(Exam::getId).max().orElse(0) + 1);
            Exam exam = new Exam(id, optString(body, "name", ""), LocalDate.parse(optString(body, "examDate", "")));
            schoolService.createExam(exam);
            examRepository.saveAll(new ArrayList<>(schoolService.getExams()));
            sendJson(exchange, 201, examDto(exam));
          }
        } else if (path.startsWith("/api/academics/subjects")) {
          if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, schoolService.getSubjects().stream().map(HttpApiServer::subjectDto).toList());
          } else {
            Map<String, Object> body = readJsonBody(exchange);
            int id = optInt(body, "id", schoolService.getSubjects().stream().mapToInt(Subject::getId).max().orElse(0) + 1);
            Subject subject = new Subject(id, optString(body, "name", ""), optInt(body, "maxMarks", 0));
            schoolService.addSubject(subject);
            subjectRepository.saveAll(new ArrayList<>(schoolService.getSubjects()));
            sendJson(exchange, 201, subjectDto(subject));
          }
        } else if (path.startsWith("/api/academics/years")) {
          if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, schoolService.getAcademicYears().stream()
                .sorted(Comparator.comparing(AcademicYear::startDate)).map(HttpApiServer::academicYearDto).toList());
          } else {
            Map<String, Object> body = readJsonBody(exchange);
            AcademicYear year = new AcademicYear(optString(body, "name", ""),
                LocalDate.parse(optString(body, "startDate", "")),
                LocalDate.parse(optString(body, "endDate", "")));
            if (schoolService.getAcademicYears().stream().anyMatch(existing -> existing.name().equalsIgnoreCase(year.name())))
              throw new IllegalArgumentException("Academic year name already exists.");
            schoolService.createAcademicYear(year);
            academicYearRepository.saveAll(new ArrayList<>(schoolService.getAcademicYears()));
            sendJson(exchange, 201, academicYearDto(year));
          }
        } else if (path.startsWith("/api/academics/classes")) {
          if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, schoolService.getAcademicClasses().stream()
                .map(HttpApiServer::academicClassDto).toList());
          } else {
            Map<String, Object> body = readJsonBody(exchange);
            String yearName = optString(body, "academicYear", "");
            AcademicYear year = schoolService.getAcademicYears().stream()
                .filter(item -> item.name().equals(yearName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Create or select a valid academic year first."));
            AcademicClass academicClass = new AcademicClass(year,
                new ClassSection(optString(body, "grade", ""), optString(body, "section", "")));
            schoolService.createClass(academicClass);
            academicClassRepository.saveAll(new ArrayList<>(schoolService.getAcademicClasses()));
            sendJson(exchange, 201, academicClassDto(academicClass));
          }
        } else {
          sendError(exchange, 404, "Not Found", "Resource not found: " + path);
        }
      } catch (IllegalArgumentException e) {
        sendError(exchange, 400, "Bad Request", e.getMessage());
      } catch (Exception e) {
        sendError(exchange, 500, "Internal Server Error", e.getMessage());
      }
    }
  }

  private final class ResultsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (handleCors(exchange)) return;
      try {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
          Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
          List<Result> results = resultService.getAllResults().stream()
              .filter(r -> !params.containsKey("studentId") || r.getStudentId() == Integer.parseInt(params.get("studentId")))
              .filter(r -> !params.containsKey("examId") || r.getExamId() == Integer.parseInt(params.get("examId")))
              .filter(r -> !params.containsKey("subjectId") || r.getSubjectId() == Integer.parseInt(params.get("subjectId")))
              .toList();
          sendJson(exchange, 200, results.stream().map(this::resultDto).toList());
        } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
          Map<String, Object> body = readJsonBody(exchange);
          Student student = schoolService.getStudent(optInt(body, "studentId", 0));
          Exam exam = schoolService.getExam(optInt(body, "examId", 0));
          Subject subject = schoolService.getSubject(optInt(body, "subjectId", 0));
          resultService.recordMarks(student, exam, subject, optInt(body, "marks", -1));
          resultRepository.saveAll(resultService.getAllResults());
          sendJson(exchange, 201, resultDto(resultService.getAllResults().stream()
              .filter(r -> r.getStudentId() == student.getId() && r.getExamId() == exam.getId()
                  && r.getSubjectId() == subject.getId()).findFirst().orElseThrow()));
        } else {
          sendError(exchange, 405, "Method Not Allowed", "Only GET and POST are supported.");
        }
      } catch (EntityNotFoundException e) {
        sendError(exchange, 404, "Not Found", e.getMessage());
      } catch (IllegalArgumentException e) {
        sendError(exchange, 400, "Bad Request", e.getMessage());
      } catch (Exception e) {
        sendError(exchange, 500, "Internal Server Error", e.getMessage());
      }
    }

    private Map<String, Object> resultDto(Result result) {
      Map<String, Object> map = new LinkedHashMap<>();
      Student student = schoolService.getStudent(result.getStudentId());
      Exam exam = schoolService.getExam(result.getExamId());
      Subject subject = schoolService.getSubject(result.getSubjectId());
      map.put("studentId", student.getId());
      map.put("studentName", student.getName());
      map.put("examId", exam.getId());
      map.put("examName", exam.getName());
      map.put("examDate", exam.getExamDate().toString());
      map.put("subjectId", subject.getId());
      map.put("subjectName", subject.getName());
      map.put("marks", result.getMarksObtained());
      map.put("maxMarks", result.getMaxMarks());
      map.put("percentage", result.getPercentage());
      map.put("grade", resultService.getGrade(result.getPercentage()));
      return map;
    }
  }

  private final class TimetableHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (handleCors(exchange)) return;
      String method = exchange.getRequestMethod().toUpperCase();
      String path = exchange.getRequestURI().getPath();
      try {
        if ("GET".equals(method) && "/api/timetable".equals(path)) {
          List<TimetableEntry> entries = timetableService.getEntries();
          List<Map<String, Object>> response = new ArrayList<>();
          for (int i = 0; i < entries.size(); i++) response.add(timetableDto(i, entries.get(i)));
          sendJson(exchange, 200, response);
        } else if ("POST".equals(method) && "/api/timetable".equals(path)) {
          TimetableEntry entry = parseTimetableEntry(readJsonBody(exchange));
          timetableService.addEntry(entry);
          timetableRepository.saveAll(timetableService.getEntries());
          sendJson(exchange, 201, timetableDto(timetableService.getEntries().size() - 1, entry));
        } else if ("PUT".equals(method) && path.startsWith("/api/timetable/")) {
          int id = parseIdFromPath(path, "/api/timetable/");
          TimetableEntry entry = parseTimetableEntry(readJsonBody(exchange));
          timetableService.updateEntry(id, entry);
          timetableRepository.saveAll(timetableService.getEntries());
          sendJson(exchange, 200, timetableDto(id, entry));
        } else if ("DELETE".equals(method) && path.startsWith("/api/timetable/")) {
          int id = parseIdFromPath(path, "/api/timetable/");
          if (!timetableService.removeEntry(id)) throw new IllegalArgumentException("Timetable entry not found.");
          timetableRepository.saveAll(timetableService.getEntries());
          sendJson(exchange, 200, Map.of("deleted", true));
        } else {
          sendError(exchange, 405, "Method Not Allowed", "Unsupported timetable request.");
        }
      } catch (EntityNotFoundException e) {
        sendError(exchange, 404, "Not Found", e.getMessage());
      } catch (IllegalArgumentException e) {
        sendError(exchange, 400, "Bad Request", e.getMessage());
      } catch (IllegalStateException e) {
        sendError(exchange, 409, "Conflict", e.getMessage());
      } catch (Exception e) {
        sendError(exchange, 500, "Internal Server Error", e.getMessage());
      }
    }
  }

  private TimetableEntry parseTimetableEntry(Map<String, Object> body) {
    ClassSection section = new ClassSection(optString(body, "grade", ""), optString(body, "section", ""));
    DayOfWeek day = DayOfWeek.valueOf(optString(body, "day", "").toUpperCase());
    LocalTime start = LocalTime.parse(optString(body, "startTime", ""));
    LocalTime end = LocalTime.parse(optString(body, "endTime", ""));
    int period = start.toSecondOfDay() / 60 / 50 + 1;
    int subjectId = optInt(body, "subjectId", 0);
    int teacherId = optInt(body, "teacherId", 0);
    schoolService.getSubject(subjectId);
    if (!schoolService.getTeacher(teacherId).isActive())
      throw new IllegalArgumentException("An active teacher is required for a timetable entry.");
    return new TimetableEntry(section, day, period, start, end, subjectId, teacherId,
        optString(body, "room", ""));
  }

  private Map<String, Object> timetableDto(int id, TimetableEntry entry) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", id);
    map.put("grade", entry.classSection().grade());
    map.put("section", entry.classSection().section());
    map.put("day", entry.day().name());
    map.put("period", entry.period());
    map.put("startTime", entry.startTime().toString());
    map.put("endTime", entry.endTime().toString());
    Subject subject = schoolService.getSubject(entry.subjectId());
    Teacher teacher = schoolService.getTeacher(entry.teacherId());
    map.put("subjectId", subject.getId());
    map.put("subjectName", subject.getName());
    map.put("teacherId", teacher.getId());
    map.put("teacherName", teacher.getName());
    map.put("room", entry.room());
    return map;
  }

  // ==========================================
  // DTO MAPPERS
  // ==========================================

  private static Map<String, Object> studentDto(Student s) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", s.getId());
    map.put("admissionNumber", s.getAdmissionNumber());
    map.put("name", s.getName());
    map.put("email", s.getEmail());
    map.put("phone", s.getPhone());
    map.put("grade", s.getGrade());
    map.put("section", s.getClassSection().section());
    map.put("admissionDate", s.getAdmissionDate().toString());
    map.put("totalFees", s.getTotalFees());
    map.put("feesPaid", s.getFeesPaid());
    map.put("remainingFees", s.getRemainingFees());
    map.put("status", s.getStatus().name());
    return map;
  }

  private static Map<String, Object> teacherDto(Teacher t) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", t.getId());
    map.put("name", t.getName());
    map.put("email", t.getEmail());
    map.put("phone", t.getPhone());
    map.put("department", t.getDepartment());
    map.put("monthlySalary", t.getMonthlySalary());
    map.put("active", t.isActive());
    map.put("subjectIds", new ArrayList<>(t.getSubjectIds()));
    return map;
  }

  private static Map<String, Object> transactionDto(Transaction t) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", t.getId());
    map.put("type", t.getType().name());
    map.put("amount", t.getAmount());
    map.put("timestamp", t.getTimestamp().toString());
    map.put("description", t.getDescription());
    map.put("relatedPersonId", t.getRelatedPersonId());
    return map;
  }

  private Map<String, Object> attendanceDto(Student student, LocalDate date, AttendanceRecord record) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("studentId", student.getId());
    map.put("studentName", student.getName());
    map.put("grade", student.getGrade());
    map.put("section", student.getClassSection().section());
    map.put("date", date.toString());
    map.put("status", record == null ? "UNRECORDED" : record.status().name());
    map.put("attendancePercentage", attendanceService.getAttendancePercentage(student.getId()));
    return map;
  }

  private static Map<String, Object> examDto(Exam exam) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", exam.getId());
    map.put("name", exam.getName());
    map.put("examDate", exam.getExamDate().toString());
    return map;
  }

  private static Map<String, Object> subjectDto(Subject subject) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", subject.getId());
    map.put("name", subject.getName());
    map.put("maxMarks", subject.getMaxMarks());
    return map;
  }

  private static Map<String, Object> academicYearDto(AcademicYear year) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", year.name());
    map.put("startDate", year.startDate().toString());
    map.put("endDate", year.endDate().toString());
    return map;
  }

  private static Map<String, Object> academicClassDto(AcademicClass academicClass) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("academicYear", academicClass.academicYear().name());
    map.put("grade", academicClass.classSection().grade());
    map.put("section", academicClass.classSection().section());
    return map;
  }

  // ==========================================
  // HTTP HELPERS & CORS
  // ==========================================

  private static boolean handleCors(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    exchange.getResponseHeaders().set(
        "Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(204, -1);
      exchange.close();
      return true;
    }
    return false;
  }

  private static int parseIdFromPath(String path, String prefix) {
    String sub = path.substring(prefix.length());
    int slash = sub.indexOf('/');
    String idStr = slash >= 0 ? sub.substring(0, slash) : sub;
    try {
      return Integer.parseInt(idStr.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid numeric ID format: " + idStr);
    }
  }

  private static Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
    try (java.io.InputStream is = exchange.getRequestBody()) {
      byte[] bytes = is.readAllBytes();
      if (bytes.length == 0) return Collections.emptyMap();
      String body = new String(bytes, StandardCharsets.UTF_8).trim();
      if (body.isEmpty()) return Collections.emptyMap();
      return JsonUtil.parseObject(body);
    }
  }

  private static String optString(Map<String, Object> map, String key, String def) {
    Object v = map.get(key);
    return v == null ? def : String.valueOf(v).trim();
  }

  private static BigDecimal optBigDecimal(Map<String, Object> map, String key, BigDecimal def) {
    Object v = map.get(key);
    if (v == null) return def;
    if (v instanceof Number n) return new BigDecimal(n.toString());
    try {
      return new BigDecimal(String.valueOf(v).trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid decimal amount for '" + key + "': " + v);
    }
  }

  private static int optInt(Map<String, Object> map, String key, int def) {
    Object v = map.get(key);
    if (v == null) return def;
    if (v instanceof Number n) return n.intValue();
    try {
      return Integer.parseInt(String.valueOf(v).trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid integer for '" + key + "': " + v);
    }
  }

  private static void sendJson(HttpExchange exchange, int statusCode, Object body)
      throws IOException {
    byte[] bytes = JsonUtil.toJson(body).getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  private static void sendError(
      HttpExchange exchange, int statusCode, String error, String message) throws IOException {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("status", statusCode);
    map.put("error", error);
    map.put("message", message);
    sendJson(exchange, statusCode, map);
  }

  private static Map<String, String> parseQueryParams(String rawQuery) {
    Map<String, String> params = new LinkedHashMap<>();
    if (rawQuery == null || rawQuery.isBlank()) return params;
    for (String pair : rawQuery.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
        String val = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
        params.put(key, val);
      }
    }
    return params;
  }

  // ==========================================
  // MAIN RUNNER
  // ==========================================

  public static void main(String[] args) {
    int port = 8080;
    if (args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        System.err.println("Invalid port number: " + args[0] + ". Using 8080.");
      }
    }
    try {
      HttpApiServer server = new HttpApiServer(port);
      server.start();
      System.out.println("==================================================");
      System.out.println("  School Management System HTTP API Server");
      System.out.println("  Running at: " + server.getBaseUrl());
      System.out.println("  Health: " + server.getBaseUrl() + "/api/health");
      System.out.println("  Dashboard: " + server.getBaseUrl() + "/api/dashboard/stats");
      System.out.println("  Press Ctrl+C to stop.");
      System.out.println("==================================================");

      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    System.out.println("Stopping HTTP server...");
                    server.stop();
                    System.out.println("Server stopped.");
                  }));
    } catch (IOException e) {
      System.err.println("Failed to start HTTP server: " + e.getMessage());
      System.exit(1);
    }
  }
}

