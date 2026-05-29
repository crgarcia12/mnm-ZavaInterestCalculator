# Security Assessment Report

**Generated:** 2026-05-29T04:05:55.0000000Z

## Summary

| Metric | Count |
|--------|-------|
| Total Findings | 4 |
| CVE Vulnerabilities | 2 |
| CWE Vulnerabilities | 2 |
| Total Rules Assessed | 59 |
| Rules Passed | 57 |

### By Severity

| Severity | Count |
|----------|-------|
| mandatory | 2 |
| optional | 2 |
| potential | 0 |

## CVE Findings (Dependency Vulnerabilities)

### CVE-2023-5072: Java: DoS Vulnerability in JSON-JAVA
- **Severity:** mandatory
- **Story Points:** 1
- **Files:** build.gradle:17

[CVE-2023-5072](https://github.com/advisories/GHSA-4jq9-2xhw-jpx7): Java: DoS Vulnerability in JSON-JAVA

Severity: HIGH

Affected dependencies:
  - org.json:json:20140107 (declared at build.gradle:17)

Recommended fix:
  - Upgrade to 20231013 or later

### CVE-2022-45688: json stack overflow vulnerability
- **Severity:** mandatory
- **Story Points:** 1
- **Files:** build.gradle:17

[CVE-2022-45688](https://github.com/advisories/GHSA-3vqj-43w4-2q58): json stack overflow vulnerability

Severity: HIGH

Affected dependencies:
  - org.json:json:20140107 (declared at build.gradle:17)

Recommended fix:
  - Upgrade to 20230227 or later

## CWE Findings (Code-Level Vulnerabilities)

### CWE-259: Use of Hard-coded Password
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** Dockerfile:11, src/main/resources/interest.properties:5, bin/main/interest.properties:5

Hard-coded database passwords are committed in Docker and properties configuration. The values appear in `Dockerfile` line 11 and `interest.properties` line 5, and `InterestConfig.getDbPassword()` loads them for outbound SQL Server connectivity.

### CWE-798: Use of Hard-coded Credentials
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** Dockerfile:10-11, src/main/resources/interest.properties:4-5, bin/main/interest.properties:4-5

Hard-coded database credentials are committed in both the Docker environment and the properties file. `InterestConfig` consumes `DB_USER` and `DB_PASSWORD` / `db.user` and `db.password` for direct SQL Server authentication.
