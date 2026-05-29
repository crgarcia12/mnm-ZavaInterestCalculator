# Architecture Diagram

This document summarizes the high-level application architecture and component relationships for ZavaInterestCalculator.

## Application Architecture

```mermaid
flowchart TD
    subgraph Client["Client Layer"]
        Browser["Web Browser"]
    end
    subgraph App["Application Layer - Spring Boot 2.7"]
        MVC["Spring MVC Controller"]
        View["JSP View Renderer"]
        Domain["Interest Calculation Logic"]
    end
    subgraph Data["Data Layer"]
        JPA["Spring Data JPA"]
        H2[("H2 In-Memory Database")]
    end

    Browser -->|"HTTP requests"| MVC
    MVC -->|"returns view model"| View
    MVC -->|"bootstrapped persistence"| JPA
    JPA -->|"JDBC"| H2
    Domain -->|"calculation result"| MVC
```

### Technology Stack Summary

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Presentation | JSP | Servlet/JSP | Render calculator form and result |
| Application | Spring Boot Web | 2.7.2 | Handle web routing and MVC flow |
| Business | Java service logic | Java 8 | Compute simple interest |
| Data | Spring Data JPA + H2 | Boot-managed | In-memory datasource configuration |

### Data Storage & External Services

The application is configured with an in-memory H2 database through Spring datasource settings. No external APIs, queues, or third-party runtime service integrations were identified.

### Key Architectural Decisions

- Uses server-side MVC with JSP views rather than a separate frontend SPA.
- Keeps business logic lightweight in the controller flow for a single use-case calculator.
- Uses embedded in-memory data source configuration, minimizing operational dependencies.

## Component Relationships

```mermaid
flowchart LR
    subgraph Presentation
        Ctl["InterestController"]
        Jsp["calculator.jsp"]
    end
    subgraph Business["Business Logic"]
        Calc["Simple Interest Formula"]
        Dto["InterestCalculationResult"]
    end
    subgraph DataAccess["Data Access"]
        Jpa["Spring Data JPA Auto Config"]
    end
    subgraph Infrastructure
        Boot["ZavaInterestCalculatorApplication"]
    end

    Ctl -->|"returns view"| Jsp
    Ctl -->|"computes"| Calc
    Calc -->|"creates"| Dto
    Ctl -->|"model attribute"| Dto
    Boot -.->|"starts"| Ctl
    Boot -.->|"configures"| Jpa
```

### Component Inventory

| Component | Layer | Type | Responsibility |
|---|---|---|---|
| ZavaInterestCalculatorApplication | Infrastructure | Spring Boot App | Bootstraps runtime |
| InterestController | Presentation | MVC Controller | Serves form and handles calculation post |
| calculator.jsp | Presentation | JSP View | Displays form and computed result |
| InterestCalculationResult | Business Logic | DTO/Model | Carries calculated values to the view |
| Spring Data JPA Auto Config | Data Access | Framework Component | Provides JPA/data source wiring |
