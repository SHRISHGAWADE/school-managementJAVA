package sms.web;

import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.*;

/**
 * Automated behavioral check and integration test for HttpApiServer and JsonUtil.
 * Runnable with standard java -ea.
 */
public final class HttpApiServerSmokeTest {

  public static void main(String[] args) throws Exception {
    System.out.println("==================================================");
    System.out.println("  STARTING STAGE 1 AUTOMATED SMOKE TESTS");
    System.out.println("==================================================");

    testJsonUtil();
    testHttpApiServerLifecycleAndEndpoints();

    System.out.println("==================================================");
    System.out.println("  ALL STAGE 1 SMOKE TESTS PASSED SUCCESSFULLY!");
    System.out.println("==================================================");
  }

  // ==========================================
  // PART 1: JSON UTIL TESTS
  // ==========================================

  private static void testJsonUtil() {
    System.out.println("\n[1/2] Testing JsonUtil serialization and parsing...");

    // 1. String escaping & special characters
    String escaped = JsonUtil.toJson("Hello \"World\" \n \t \\ \r \b \f");
    assert escaped.equals("\"Hello \\\"World\\\" \\n \\t \\\\ \\r \\b \\f\"")
        : "Failed escaping: " + escaped;
    Object parsedStr = JsonUtil.parse(escaped);
    assert "Hello \"World\" \n \t \\ \r \b \f".equals(parsedStr)
        : "Failed parse string: " + parsedStr;

    // 2. Unicode parsing
    String unicodeJson = "\"Unicode \\u0041\\u0042\\u0043 and \\u20AC\"";
    Object parsedUnicode = JsonUtil.parse(unicodeJson);
    assert "Unicode ABC and \u20AC".equals(parsedUnicode)
        : "Failed unicode parse: " + parsedUnicode;

    // 3. Numbers (integers, negative numbers, floats, exponential)
    assert JsonUtil.parse("0").equals(0);
    assert JsonUtil.parse("42").equals(42);
    assert JsonUtil.parse("-123").equals(-123);
    assert ((Number) JsonUtil.parse("3.1415")).doubleValue() == 3.1415;
    assert ((Number) JsonUtil.parse("-0.5")).doubleValue() == -0.5;
    assert ((Number) JsonUtil.parse("1e3")).doubleValue() == 1000.0;
    assert ((Number) JsonUtil.parse("2.5E-2")).doubleValue() == 0.025;

    // 4. Booleans and Null
    assert Boolean.TRUE.equals(JsonUtil.parse("true"));
    assert Boolean.FALSE.equals(JsonUtil.parse("false"));
    assert JsonUtil.parse("null") == null;

    // 5. Arrays
    String arrayJson = "[1, \"two\", true, null, [3, 4]]";
    List<Object> list = JsonUtil.parseArray(arrayJson);
    assert list.size() == 5;
    assert list.get(0).equals(1);
    assert list.get(1).equals("two");
    assert list.get(2).equals(true);
    assert list.get(3) == null;
    assert list.get(4) instanceof List<?>;
    assert ((List<?>) list.get(4)).size() == 2;

    // 6. Objects & Nesting
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", "Green Valley");
    map.put("count", 150);
    map.put("active", true);
    map.put("tags", List.of("school", "education"));
    map.put("details", Map.of("city", "Springfield"));

    String serializedObj = JsonUtil.toJson(map);
    Map<String, Object> deserializedMap = JsonUtil.parseObject(serializedObj);
    assert "Green Valley".equals(deserializedMap.get("name"));
    assert Integer.valueOf(150).equals(deserializedMap.get("count"));
    assert Boolean.TRUE.equals(deserializedMap.get("active"));
    assert deserializedMap.get("tags") instanceof List<?>;
    assert deserializedMap.get("details") instanceof Map<?, ?>;

    // 7. Malformed JSON handling (must throw IllegalArgumentException)
    expectMalformed(null, "null input");
    expectMalformed("", "empty input");
    expectMalformed("   ", "whitespace only input");
    expectMalformed("{\"unclosed\": 1", "unclosed object");
    expectMalformed("[1, 2, 3", "unclosed array");
    expectMalformed("\"unclosed string", "unclosed string");
    expectMalformed("{\"a\": 1,}", "trailing comma in object");
    expectMalformed("[1, 2,]", "trailing comma in array");
    expectMalformed("{\"missingColon\" 123}", "missing colon");
    expectMalformed("{\"a\": 1} extraTokens", "trailing content");
    expectMalformed("tru", "partial true");
    expectMalformed("fal", "partial false");
    expectMalformed("nul", "partial null");
    expectMalformed("{\"invalid\": \\uXYZ}", "invalid unicode sequence");

    System.out.println("  -> JsonUtil passed all string, unicode, number, array, object, and malformed tests.");
  }

  // ==========================================
  // PART 2: HTTP SERVER LIFECYCLE & ENDPOINTS
  // ==========================================

  private static void testHttpApiServerLifecycleAndEndpoints() throws Exception {
    System.out.println("\n[2/2] Testing HttpApiServer lifecycle, routes, and error handling...");

    // 1. Create temporary test data directory to verify CSV persistence without dirtying working tree
    Path testDataDir = java.nio.file.Files.createTempDirectory("sms-smoke-test-data");
    java.nio.file.Files.copy(java.nio.file.Path.of("data", "students.csv"), testDataDir.resolve("students.csv"));
    java.nio.file.Files.copy(java.nio.file.Path.of("data", "teachers.csv"), testDataDir.resolve("teachers.csv"));
    java.nio.file.Files.copy(java.nio.file.Path.of("data", "transactions.csv"), testDataDir.resolve("transactions.csv"));

    HttpApiServer server = new HttpApiServer(0, testDataDir, true);
    server.start();
    int boundPort = server.getPort();
    String baseUrl = server.getBaseUrl();
    System.out.println("  -> HttpApiServer successfully started at: " + baseUrl);

    // 2. Test port conflict detection
    try {
      // Trying to bind to the exact same port without fallback must throw BindException
      HttpApiServer conflicting = new HttpApiServer(boundPort, Path.of("data"), false);
      conflicting.start();
      throw new AssertionError("Expected BindException for occupied port " + boundPort);
    } catch (BindException e) {
      System.out.println("  -> Port conflict correctly detected and BindException handled.");
    }

    // 3. Test HTTP Endpoints using HttpClient
    HttpClient client = HttpClient.newHttpClient();

    try {
      // Root /
      HttpResponse<String> rootRes = get(client, baseUrl + "/");
      assert rootRes.statusCode() == 200 : "Expected 200 for root, got " + rootRes.statusCode();
      Map<String, Object> rootObj = JsonUtil.parseObject(rootRes.body());
      assert "UP".equals(rootObj.get("status"));

      // /api/health
      HttpResponse<String> healthRes = get(client, baseUrl + "/api/health");
      assert healthRes.statusCode() == 200;
      Map<String, Object> healthObj = JsonUtil.parseObject(healthRes.body());
      assert "UP".equals(healthObj.get("status"));
      assert "Green Valley High School".equals(healthObj.get("school"));
      System.out.println("  -> GET /api/health: 200 OK (Status: UP, School: " + healthObj.get("school") + ")");

      // /api/dashboard/stats
      HttpResponse<String> statsRes = get(client, baseUrl + "/api/dashboard/stats");
      assert statsRes.statusCode() == 200;
      Map<String, Object> stats = JsonUtil.parseObject(statsRes.body());
      assert stats.containsKey("schoolName");
      assert stats.containsKey("studentCount");
      assert stats.containsKey("teacherCount");
      assert stats.containsKey("balance");
      assert stats.containsKey("totalIncome");
      assert stats.containsKey("totalExpenses");
      assert stats.containsKey("transactionCount");
      int studentCount = ((Number) stats.get("studentCount")).intValue();
      int teacherCount = ((Number) stats.get("teacherCount")).intValue();
      int txCount = ((Number) stats.get("transactionCount")).intValue();
      System.out.println("  -> GET /api/dashboard/stats: 200 OK (Students: " + studentCount
          + ", Teachers: " + teacherCount
          + ", Balance: " + stats.get("balance")
          + ", Transactions: " + txCount + ")");

      // /api/students
      HttpResponse<String> studentsRes = get(client, baseUrl + "/api/students");
      assert studentsRes.statusCode() == 200;
      List<Object> students = JsonUtil.parseArray(studentsRes.body());
      assert students.size() == studentCount : "Student count mismatch: " + students.size() + " vs " + studentCount;
      assert students.size() > 0 : "Expected at least 1 student from CSV";
      @SuppressWarnings("unchecked")
      Map<String, Object> firstStudent = (Map<String, Object>) students.get(0);
      int firstStudentId = ((Number) firstStudent.get("id")).intValue();
      assert firstStudent.containsKey("admissionNumber");
      assert firstStudent.containsKey("name");
      assert firstStudent.containsKey("remainingFees");
      System.out.println("  -> GET /api/students: 200 OK (Found " + students.size() + " students, first ID: " + firstStudentId + ")");

      // /api/students?q=Ava
      HttpResponse<String> studentSearchRes = get(client, baseUrl + "/api/students?q=Ava");
      assert studentSearchRes.statusCode() == 200;
      List<Object> searchResults = JsonUtil.parseArray(studentSearchRes.body());
      assert searchResults.size() >= 1 : "Expected search for 'Ava' to return at least 1 record";

      // /api/students/{id} - existing
      HttpResponse<String> singleStudentRes = get(client, baseUrl + "/api/students/" + firstStudentId);
      assert singleStudentRes.statusCode() == 200;
      Map<String, Object> singleStudent = JsonUtil.parseObject(singleStudentRes.body());
      assert Integer.valueOf(firstStudentId).equals(singleStudent.get("id"));

      // /api/students/{id} - not found (404)
      HttpResponse<String> notFoundStudent = get(client, baseUrl + "/api/students/999999");
      assert notFoundStudent.statusCode() == 404 : "Expected 404 for missing student, got " + notFoundStudent.statusCode();
      Map<String, Object> notFoundObj = JsonUtil.parseObject(notFoundStudent.body());
      assert Integer.valueOf(404).equals(notFoundObj.get("status"));
      System.out.println("  -> GET /api/students/999999: 404 Not Found correctly returned.");

      // /api/students/invalid-id (400)
      HttpResponse<String> invalidStudentId = get(client, baseUrl + "/api/students/abc");
      assert invalidStudentId.statusCode() == 400 : "Expected 400 for bad student ID, got " + invalidStudentId.statusCode();
      Map<String, Object> badIdObj = JsonUtil.parseObject(invalidStudentId.body());
      assert Integer.valueOf(400).equals(badIdObj.get("status"));
      System.out.println("  -> GET /api/students/abc: 400 Bad Request correctly returned.");

      // /api/teachers
      HttpResponse<String> teachersRes = get(client, baseUrl + "/api/teachers");
      assert teachersRes.statusCode() == 200;
      List<Object> teachers = JsonUtil.parseArray(teachersRes.body());
      assert teachers.size() == teacherCount;
      assert teachers.size() > 0;
      @SuppressWarnings("unchecked")
      Map<String, Object> firstTeacher = (Map<String, Object>) teachers.get(0);
      int firstTeacherId = ((Number) firstTeacher.get("id")).intValue();
      assert firstTeacher.containsKey("name");
      assert firstTeacher.containsKey("department");
      assert firstTeacher.containsKey("monthlySalary");
      System.out.println("  -> GET /api/teachers: 200 OK (Found " + teachers.size() + " teachers, first ID: " + firstTeacherId + ")");

      // /api/teachers/{id} - existing & not found
      HttpResponse<String> singleTeacherRes = get(client, baseUrl + "/api/teachers/" + firstTeacherId);
      assert singleTeacherRes.statusCode() == 200;
      HttpResponse<String> notFoundTeacher = get(client, baseUrl + "/api/teachers/999999");
      assert notFoundTeacher.statusCode() == 404;

      // /api/finance/transactions
      HttpResponse<String> txRes = get(client, baseUrl + "/api/finance/transactions");
      assert txRes.statusCode() == 200;
      List<Object> txList = JsonUtil.parseArray(txRes.body());
      assert txList.size() == txCount;
      System.out.println("  -> GET /api/finance/transactions: 200 OK (Found " + txList.size() + " transactions)");

      // /api/finance/transactions?type=DONATION
      HttpResponse<String> donationTxRes = get(client, baseUrl + "/api/finance/transactions?type=DONATION");
      assert donationTxRes.statusCode() == 200;
      List<Object> donationList = JsonUtil.parseArray(donationTxRes.body());
      assert donationList.size() > 0;
      for (Object item : donationList) {
        @SuppressWarnings("unchecked")
        Map<String, Object> tx = (Map<String, Object>) item;
        assert "DONATION".equals(tx.get("type")) : "Expected only DONATION type, got: " + tx.get("type");
      }

      // /api/finance/transactions?type=INVALID_TYPE (400)
      HttpResponse<String> badTxType = get(client, baseUrl + "/api/finance/transactions?type=INVALID_TYPE");
      assert badTxType.statusCode() == 400;
      Map<String, Object> badTxObj = JsonUtil.parseObject(badTxType.body());
      assert Integer.valueOf(400).equals(badTxObj.get("status"));
      System.out.println("  -> GET /api/finance/transactions?type=INVALID_TYPE: 400 Bad Request correctly returned.");

      // Invalid route /api/nonexistent (404)
      HttpResponse<String> missingRoute = get(client, baseUrl + "/api/nonexistent");
      assert missingRoute.statusCode() == 404;
      System.out.println("  -> GET /api/nonexistent: 404 Not Found correctly returned.");

      // ==========================================
      // STAGE 3: STUDENT & TEACHER CRUD & CSV SYNC
      // ==========================================

      // 1. POST /api/students (Enroll Student)
      Map<String, Object> newStudentReq = new LinkedHashMap<>();
      newStudentReq.put("admissionNumber", "ADM999");
      newStudentReq.put("name", "Ananya Verma");
      newStudentReq.put("email", "ananya@example.com");
      newStudentReq.put("phone", "9876543210");
      newStudentReq.put("grade", "Grade 11");
      newStudentReq.put("section", "B");
      newStudentReq.put("totalFees", 35000);

      HttpRequest enrollReq = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/students"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(newStudentReq)))
          .build();
      HttpResponse<String> enrollRes = client.send(enrollReq, HttpResponse.BodyHandlers.ofString());
      assert enrollRes.statusCode() == 201 : "Expected 201 Created, got " + enrollRes.statusCode();
      Map<String, Object> createdStudent = JsonUtil.parseObject(enrollRes.body());
      int newStudentId = ((Number) createdStudent.get("id")).intValue();
      assert "Ananya Verma".equals(createdStudent.get("name"));
      assert "ACTIVE".equals(createdStudent.get("status"));
      System.out.println("  -> POST /api/students: 201 Created (New student ID: " + newStudentId + ")");

      // 2. PUT /api/students/{id} (Update Student)
      Map<String, Object> updateStudentReq = new LinkedHashMap<>();
      updateStudentReq.put("name", "Ananya Verma Senior");
      updateStudentReq.put("email", "ananya.sr@example.com");
      updateStudentReq.put("phone", "9876543299");

      HttpRequest updateReq = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/students/" + newStudentId))
          .header("Content-Type", "application/json")
          .PUT(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(updateStudentReq)))
          .build();
      HttpResponse<String> updateRes = client.send(updateReq, HttpResponse.BodyHandlers.ofString());
      assert updateRes.statusCode() == 200 : "Expected 200 OK, got " + updateRes.statusCode();
      Map<String, Object> updatedStudent = JsonUtil.parseObject(updateRes.body());
      assert "Ananya Verma Senior".equals(updatedStudent.get("name"));
      assert "ananya.sr@example.com".equals(updatedStudent.get("email"));
      System.out.println("  -> PUT /api/students/" + newStudentId + ": 200 OK (Updated contact details)");

      // 3. DELETE /api/students/{id} (Soft-deactivate Student)
      HttpRequest deleteStudentReq = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/students/" + newStudentId))
          .DELETE()
          .build();
      HttpResponse<String> deleteStudentRes = client.send(deleteStudentReq, HttpResponse.BodyHandlers.ofString());
      assert deleteStudentRes.statusCode() == 200;
      Map<String, Object> deactivatedStudent = JsonUtil.parseObject(deleteStudentRes.body());
      assert "INACTIVE".equals(deactivatedStudent.get("status"));
      System.out.println("  -> DELETE /api/students/" + newStudentId + ": 200 OK (Status now INACTIVE)");

      // 4. PATCH /api/students/{id}/activate (Reactivate Student)
      HttpRequest activateStudentReq = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/students/" + newStudentId + "/activate"))
          .method("PATCH", HttpRequest.BodyPublishers.noBody())
          .build();
      HttpResponse<String> activateStudentRes = client.send(activateStudentReq, HttpResponse.BodyHandlers.ofString());
      assert activateStudentRes.statusCode() == 200;
      Map<String, Object> reactivatedStudent = JsonUtil.parseObject(activateStudentRes.body());
      assert "ACTIVE".equals(reactivatedStudent.get("status"));
      System.out.println("  -> PATCH /api/students/" + newStudentId + "/activate: 200 OK (Status restored to ACTIVE)");

      // 5. POST /api/teachers (Hire Teacher)
      Map<String, Object> newTeacherReq = new LinkedHashMap<>();
      newTeacherReq.put("name", "Vikram Patel");
      newTeacherReq.put("email", "vikram@example.com");
      newTeacherReq.put("phone", "9812345678");
      newTeacherReq.put("department", "Computer Science");
      newTeacherReq.put("monthlySalary", 32000);

      HttpRequest hireReq = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/teachers"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(newTeacherReq)))
          .build();
      HttpResponse<String> hireRes = client.send(hireReq, HttpResponse.BodyHandlers.ofString());
      assert hireRes.statusCode() == 201;
      Map<String, Object> createdTeacher = JsonUtil.parseObject(hireRes.body());
      int newTeacherId = ((Number) createdTeacher.get("id")).intValue();
      assert "Vikram Patel".equals(createdTeacher.get("name"));
      assert "Computer Science".equals(createdTeacher.get("department"));
      System.out.println("  -> POST /api/teachers: 201 Created (New teacher ID: " + newTeacherId + ")");

      // 6. PUT /api/teachers/{id} (Update Teacher)
      Map<String, Object> updateTeacherReq = new LinkedHashMap<>();
      updateTeacherReq.put("name", "Dr. Vikram Patel");
      updateTeacherReq.put("monthlySalary", 35000);

      HttpRequest updateTeacherHttp = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/teachers/" + newTeacherId))
          .header("Content-Type", "application/json")
          .PUT(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(updateTeacherReq)))
          .build();
      HttpResponse<String> updateTeacherRes = client.send(updateTeacherHttp, HttpResponse.BodyHandlers.ofString());
      assert updateTeacherRes.statusCode() == 200;
      Map<String, Object> updatedTeacher = JsonUtil.parseObject(updateTeacherRes.body());
      assert "Dr. Vikram Patel".equals(updatedTeacher.get("name"));
      System.out.println("  -> PUT /api/teachers/" + newTeacherId + ": 200 OK (Updated salary and title)");

      // 7. DELETE /api/teachers/{id} (Soft-deactivate Teacher)
      HttpRequest deleteTeacherReq = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/teachers/" + newTeacherId))
          .DELETE()
          .build();
      HttpResponse<String> deleteTeacherRes = client.send(deleteTeacherReq, HttpResponse.BodyHandlers.ofString());
      assert deleteTeacherRes.statusCode() == 200;
      Map<String, Object> deactivatedTeacher = JsonUtil.parseObject(deleteTeacherRes.body());
      assert Boolean.FALSE.equals(deactivatedTeacher.get("active"));
      System.out.println("  -> DELETE /api/teachers/" + newTeacherId + ": 200 OK (Teacher deactivated)");

      // 8. Verify CSV Persistence on disk
      List<String> studentCsvLines = java.nio.file.Files.readAllLines(testDataDir.resolve("students.csv"));
      boolean studentInCsv = studentCsvLines.stream().anyMatch(l -> l.contains("Ananya Verma Senior"));
      assert studentInCsv : "Expected student Ananya Verma Senior to be persisted in students.csv";

      List<String> teacherCsvLines = java.nio.file.Files.readAllLines(testDataDir.resolve("teachers.csv"));
      boolean teacherInCsv = teacherCsvLines.stream().anyMatch(l -> l.contains("Dr. Vikram Patel"));
      assert teacherInCsv : "Expected teacher Dr. Vikram Patel to be persisted in teachers.csv";
      System.out.println("  -> CSV Persistence Verified: Both student and teacher updates written to disk.");

      // 9. Validation error tests (Bad request)
      HttpRequest badStudentReq = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/students"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString("{\"name\": \"\", \"admissionNumber\": \"\"}"))
          .build();
      HttpResponse<String> badStudentRes = client.send(badStudentReq, HttpResponse.BodyHandlers.ofString());
      assert badStudentRes.statusCode() == 400 : "Expected 400 for empty student name, got " + badStudentRes.statusCode();

      HttpRequest badTeacherReq = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/teachers"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString("{\"name\": \"\", \"department\": \"\"}"))
          .build();
      HttpResponse<String> badTeacherRes = client.send(badTeacherReq, HttpResponse.BodyHandlers.ofString());
      assert badTeacherRes.statusCode() == 400 : "Expected 400 for empty teacher name, got " + badTeacherRes.statusCode();
      System.out.println("  -> Validation Errors: 400 Bad Request correctly returned for invalid inputs.");

      // 10. Unsupported method test
      HttpRequest unsupportedReq = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/students"))
          .DELETE()
          .build();
      HttpResponse<String> unsupportedRes = client.send(unsupportedReq, HttpResponse.BodyHandlers.ofString());
      assert unsupportedRes.statusCode() == 405 : "Expected 405 for DELETE /api/students, got " + unsupportedRes.statusCode();
      System.out.println("  -> Method Not Allowed: 405 correctly returned for DELETE /api/students collection root.");

      // Static Frontend Assets Verification
      HttpRequest htmlReq = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/"))
          .header("Accept", "text/html,application/xhtml+xml")
          .GET()
          .build();
      HttpResponse<String> htmlRes = client.send(htmlReq, HttpResponse.BodyHandlers.ofString());
      assert htmlRes.statusCode() == 200;
      assert htmlRes.headers().firstValue("Content-Type").orElse("").contains("text/html");
      assert htmlRes.body().contains("Institutional Overview");
      assert htmlRes.body().contains("app.js");
      System.out.println("  -> GET / (Accept: text/html): 200 OK (Served web/index.html)");

      HttpResponse<String> cssRes = get(client, baseUrl + "/css/style.css");
      assert cssRes.statusCode() == 200;
      assert cssRes.headers().firstValue("Content-Type").orElse("").contains("text/css");
      assert cssRes.body().contains("--color-sage: #B8CBAA;");
      System.out.println("  -> GET /css/style.css: 200 OK (Served web/css/style.css)");

      HttpResponse<String> jsAppRes = get(client, baseUrl + "/js/app.js");
      assert jsAppRes.statusCode() == 200;
      assert jsAppRes.headers().firstValue("Content-Type").orElse("").contains("javascript");
      assert jsAppRes.body().contains("initDashboard");
      System.out.println("  -> GET /js/app.js: 200 OK (Served web/js/app.js)");

      HttpResponse<String> jsApiRes = get(client, baseUrl + "/js/api.js");
      assert jsApiRes.statusCode() == 200;
      assert jsApiRes.headers().firstValue("Content-Type").orElse("").contains("javascript");
      assert jsApiRes.body().contains("getDashboardStats");
      System.out.println("  -> GET /js/api.js: 200 OK (Served web/js/api.js)");

      HttpResponse<String> jsDashRes = get(client, baseUrl + "/js/dashboard.js");
      assert jsDashRes.statusCode() == 200;
      assert jsDashRes.headers().firstValue("Content-Type").orElse("").contains("javascript");
      assert jsDashRes.body().contains("renderDashboard");
      System.out.println("  -> GET /js/dashboard.js: 200 OK (Served web/js/dashboard.js)");

    } finally {
      // 4. Clean shutdown
      server.stop();
      System.out.println("  -> HttpApiServer stopped cleanly.");
    }
  }

  // ==========================================
  // TEST HELPERS
  // ==========================================

  private static HttpResponse<String> get(HttpClient client, String url) throws IOException, InterruptedException {
    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
    return client.send(req, HttpResponse.BodyHandlers.ofString());
  }

  private static void expectMalformed(String json, String scenario) {
    try {
      JsonUtil.parse(json);
      throw new AssertionError("Expected IllegalArgumentException for " + scenario);
    } catch (IllegalArgumentException expected) {
      // Expected
    }
  }
}

