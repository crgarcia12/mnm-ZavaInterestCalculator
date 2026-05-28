# Security Assessment Report

**Generated:** 2026-05-28T23:00:00.0000000Z

## Summary

| Metric | Count |
|--------|-------|
| Total Findings | 7 |
| CVE Vulnerabilities | 2 |
| CWE Vulnerabilities | 5 |
| Total Rules Assessed | 59 |
| Rules Passed | 54 |

### By Severity

| Severity | Count |
|----------|-------|
| mandatory | 2 |
| optional | 3 |
| potential | 2 |

## CVE Findings (Dependency Vulnerabilities)

### CVE-2023-5072: Java: DoS Vulnerability in JSON-JAVA
- **Severity:** mandatory
- **Story Points:** 1
- **Files:** build.gradle:17

[CVE-2023-5072](https://github.com/advisories/GHSA-4jq9-2xhw-jpx7): Java: DoS Vulnerability in JSON-JAVA

Severity: HIGH

A denial of service vulnerability in JSON-Java. A bug in the parser means that an input string of modest size can lead to indefinite amounts of memory being used. By nesting JSON objects, an attacker can trigger exponential memory usage, leading to an OutOfMemoryError.

Affected dependencies:
  - org.json:json:20140107 (declared at build.gradle:17)

Vulnerable version range: <= 20230618

Recommended fix:
  - Upgrade org.json:json to 20231013 or later

---

### CVE-2022-45688: json stack overflow vulnerability
- **Severity:** mandatory
- **Story Points:** 1
- **Files:** build.gradle:17

[CVE-2022-45688](https://github.com/advisories/GHSA-3vqj-43w4-2q58): json stack overflow vulnerability

Severity: HIGH

A stack overflow in the XML.toJSONObject component of org.json:json before version 20230227 allows attackers to cause a Denial of Service (DoS) via crafted JSON or XML data.

Affected dependencies:
  - org.json:json:20140107 (declared at build.gradle:17)

Vulnerable version range: < 20230227

Recommended fix:
  - Upgrade org.json:json to 20230227 or later

---

## CWE Findings (Code-Level Vulnerabilities)

### CWE-477: Use of Obsolete Function
- **Category:** Code Quality
- **Severity:** optional
- **Story Points:** 1
- **Files:** src/main/java/com/zavabank/interestcalculator/InterestConnectionFactory.java

In InterestConnectionFactory.java at line 10, the code uses `Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")` to manually load the JDBC driver. This pattern is obsolete since JDBC 4.0 (Java SE 6), which introduced automatic driver discovery via the ServiceLoader mechanism. Modern JDBC drivers are automatically loaded when the driver JAR is present on the classpath, making the explicit `Class.forName()` call unnecessary and indicating the code has not been updated to use current practices.

---

### CWE-1057: Data Access Operations Outside of Expected Data Manager Component
- **Category:** Code Quality
- **Severity:** potential
- **Story Points:** 5
- **Files:** src/main/java/com/zavabank/interestcalculator/InterestCalculateServlet.java, src/main/java/com/zavabank/interestcalculator/InterestAccrueAllServlet.java, src/main/java/com/zavabank/interestcalculator/InterestBootstrapServlet.java

All three servlet classes (InterestCalculateServlet, InterestAccrueAllServlet, InterestBootstrapServlet) contain direct SQL data-access operations (`prepareStatement`, `executeQuery`, `executeUpdate`) embedded within business logic. There is no dedicated data manager or repository/DAO layer; the InterestConnectionFactory only provides raw JDBC connections. This means data access logic is scattered across multiple presentation-layer components rather than being centralized in a dedicated data manager.

---

### CWE-259: Use of Hard-coded Password
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** src/main/resources/interest.properties

The file `interest.properties` contains a hard-coded database password at line 5. This password is used by `InterestConnectionFactory.openConnection()` via `InterestConfig.getDbPassword()` to authenticate against the SQL Server database. Storing credentials in a properties file included in the application package exposes the password to anyone with access to the deployed artifact or source repository.

---

### CWE-778: Insufficient Logging
- **Category:** Credentials & Secrets
- **Severity:** potential
- **Story Points:** 3
- **Files:** src/main/java/com/zavabank/interestcalculator/InterestCalculateServlet.java, src/main/java/com/zavabank/interestcalculator/InterestAccrueAllServlet.java

The application performs no logging of any kind — no logging framework (Log4j, SLF4J, java.util.logging) is used anywhere in the codebase. Security-critical financial operations such as interest accruals and balance modifications complete without generating any audit trail. SQL exceptions are silently caught and generic error responses are returned to clients. The absence of logging makes it impossible to detect security incidents, unauthorized access attempts, or data integrity issues.

---

### CWE-798: Use of Hard-coded Credentials
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** src/main/resources/interest.properties

The file `interest.properties` contains hard-coded database credentials: username `sa` (line 4) and password `YourStrong!Passw0rd` (line 5). These credentials are used in `InterestConnectionFactory.openConnection()` to establish JDBC connections to the SQL Server database. The `sa` account is the SQL Server system administrator account, meaning the application connects with maximum database privileges.

---
