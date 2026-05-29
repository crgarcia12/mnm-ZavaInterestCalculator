# Configuration & Externalized Settings Inventory

This project uses a compact Spring Boot configuration setup with a single property file and no external configuration service integrations.

## Configuration Sources

| Source | Type | Path/Location | Notes |
|---|---|---|---|
| application.properties | Spring Boot properties | src/main/resources/application.properties | Contains view and datasource settings |
| build.gradle | Build configuration | build.gradle | Declares plugins, dependencies, Java version |

## Build Profiles

| Profile | Activation | Purpose | Key Dependencies/Plugins |
|---|---|---|---|
| default Gradle build | standard gradle invocation | Build WAR/Spring Boot app | java, war, spring-boot, dependency-management plugins |

## Runtime Profiles

| Profile | Activation Method | Config Files | Key Overrides |
|---|---|---|---|
| default | implicit (no active profile override found) | application.properties | MVC view resolver and in-memory datasource values |

## Properties Inventory

| Property Key | Default | Profiles | Source |
|---|---|---|---|
| spring.mvc.view.prefix | /WEB-INF/views/ | default | application.properties |
| spring.mvc.view.suffix | .jsp | default | application.properties |
| spring.application.name | InterestCalculator | default | application.properties |
| spring.datasource.url | jdbc:h2:mem:testdb | default | application.properties |
| spring.datasource.driverClassName | org.h2.Driver | default | application.properties |
| spring.datasource.username | sa | default | application.properties |
| spring.datasource.password | [MASKED] | default | application.properties |
| spring.jpa.database-platform | org.hibernate.dialect.H2Dialect | default | application.properties |

## Startup Parameters & Resource Requirements

| Service | JVM/Runtime Options | Memory | Instance Count |
|---|---|---|---|
| ZavaInterestCalculator | None explicitly configured in repository | Not specified | Not specified |

## Startup Dependency Chain

1. ZavaInterestCalculator starts directly with Spring Boot runtime.
2. Embedded servlet container and MVC context initialize.
3. In-memory datasource is initialized as part of bootstrapping.

## Secrets & Sensitive Configuration

| Secret Reference | Type | Storage (masked) |
|---|---|---|
| spring.datasource.password | Database credential | [MASKED] (empty in source) |

### Secrets Provisioning Workflow

No external secret manager integration is configured. Sensitive values are expected from local property files or runtime environment overrides.

## Feature Flags

| Flag Name | Default | Controlled By |
|---|---|---|
| None detected | N/A | N/A |

## Framework & Runtime Versions

| Component | Version | Source |
|---|---|---|
| Spring Boot plugin | 2.7.2 | build.gradle |
| io.spring.dependency-management plugin | 1.0.12.RELEASE | build.gradle |
| Java sourceCompatibility | 8 | build.gradle |
| Gradle | 9.5.1 (runtime observed) | local execution |
