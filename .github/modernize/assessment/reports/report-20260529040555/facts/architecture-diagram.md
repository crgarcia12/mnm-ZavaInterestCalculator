# Architecture Diagram

This repository contains a single deployable banking web application that exposes servlet endpoints for interest calculation and accrual. It is packaged as a WAR, runs on Tomcat, and talks directly to SQL Server through JDBC.

## Application Architecture

```mermaid
flowchart TD
    subgraph Client["Client Layer"]
        Browser["Browser or API Client"]
    end
    subgraph App["Application Layer - Servlet WAR"]
        Jsp["index.jsp"]
        Health["HealthServlet"]
        Calc["InterestCalculateServlet"]
        Accrue["InterestAccrueAllServlet"]
        Boot["InterestBootstrapServlet"]
    end
    subgraph Core["Core Logic"]
        Config["InterestConfig"]
        Rules["Interest calculation rules"]
        Conn["InterestConnectionFactory"]
    end
    subgraph Data["Data Layer"]
        Jdbc["JDBC access"]
        Sql[("SQL Server")]
    end

    Browser -->|"GET /"| Jsp
    Browser -->|"health check"| Health
    Browser -->|"POST calculate request"| Calc
    Browser -->|"POST accrue-all request"| Accrue
    Boot -->|"startup schema check"| Conn
    Calc -->|"loads defaults"| Config
    Accrue -->|"loads defaults"| Config
    Calc -->|"computes interest"| Rules
    Accrue -->|"computes interest"| Rules
    Calc -->|"opens connection"| Conn
    Accrue -->|"opens connection"| Conn
    Conn -->|"creates JDBC sessions"| Jdbc
    Jdbc -->|"SQL queries and updates"| Sql
```

### Technology Stack Summary

| Layer | Technology | Version | Purpose |
|---|---|---:|---|
| Presentation | JSP + HttpServlet | Servlet 3.1 | Serves landing page, health page, and JSON endpoints |
| Business Logic | Custom Java classes | Java 8 | Computes simple, compound, and amortized interest |
| Data Access | JDBC + SQL Server driver | mssql-jdbc 12.6.3.jre8 | Reads account balances and writes accrual records |
| Runtime | Tomcat container | 9-jdk8 | Hosts the WAR package |
| Build | Gradle | build image 7.6 | Produces the deployable WAR |

### Data Storage & External Services

The application depends on a single SQL Server database. It reads account and account-type data from shared banking tables and writes accrual history into an `InterestAccruals` table that is created during servlet startup when it does not already exist. No caches, queues, or third-party APIs were identified.

### Key Architectural Decisions

- Uses deployment-descriptor registration in `web.xml` instead of annotation-driven endpoint discovery.
- Keeps business logic inside servlets with helper methods rather than a separate service or repository layer.
- Initializes schema state at application startup through `InterestBootstrapServlet`.

## Component Relationships

```mermaid
flowchart LR
    subgraph Presentation
        Index["index.jsp"]
        HealthServlet["HealthServlet"]
        CalcServlet["InterestCalculateServlet"]
        AccrueServlet["InterestAccrueAllServlet"]
        BootstrapServlet["InterestBootstrapServlet"]
    end
    subgraph Business["Business Logic"]
        ConfigComp["InterestConfig"]
        CalcRules["Interest calculation helpers"]
    end
    subgraph DataAccess["Data Access"]
        Factory["InterestConnectionFactory"]
        JdbcOps["PreparedStatement usage"]
    end
    subgraph Infrastructure
        SqlDb["SQL Server"]
    end

    CalcServlet -->|"loads config"| ConfigComp
    AccrueServlet -->|"loads config"| ConfigComp
    CalcServlet -->|"delegates math"| CalcRules
    AccrueServlet -->|"delegates math"| CalcRules
    CalcServlet -->|"opens connections"| Factory
    AccrueServlet -->|"opens connections"| Factory
    BootstrapServlet -->|"opens connections"| Factory
    Factory -->|"creates statements"| JdbcOps
    JdbcOps -->|"reads and writes"| SqlDb
```

### Component Inventory

| Component | Layer | Type | Responsibility |
|---|---|---|---|
| `index.jsp` | Presentation | JSP page | Static landing page for the application |
| `HealthServlet` | Presentation | Servlet | Returns a simple availability page |
| `InterestCalculateServlet` | Presentation | Servlet | Calculates interest for a single account and optionally applies it |
| `InterestAccrueAllServlet` | Presentation | Servlet | Accrues interest for all active accounts in one transaction |
| `InterestBootstrapServlet` | Presentation | Servlet | Creates the accrual table during startup |
| `InterestConfig` | Business Logic | Configuration helper | Resolves environment variables and default rates |
| `InterestConnectionFactory` | Data Access | Connection factory | Opens SQL Server JDBC connections |
| SQL Server | Infrastructure | Database | Stores account balances, account types, and accrual history |
