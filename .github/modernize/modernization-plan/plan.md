# Modernization Plan: modernization-plan

**Project**: mnm-ZavaInterestCalculator

---

## Technical Framework

- **Language**: Java 8
- **Framework**: Java Servlet API 3.1 (WAR web application)
- **Build Tool**: Gradle
- **Database**: Microsoft SQL Server (JDBC driver)
- **Key Dependencies**: javax.servlet-api, mssql-jdbc, org.json

---

## Overview

> This migration modernizes the mnm-ZavaInterestCalculator application for Azure.
> The application currently runs as a traditional Java WAR-based web app.
> The new architecture will:
>
> - Improve deployment consistency for cloud hosting on Azure
> - Include dependency vulnerability remediation before release
> - Prepare the application for repeatable Azure deployment operations
>
> The migration follows a phased approach: security hardening first,
> then Azure deployment enablement.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| mnm-ZavaInterestCalculator | Local/web-hosted WAR | Azure Container Apps | Managed Identity | Baseline modernization plan |
