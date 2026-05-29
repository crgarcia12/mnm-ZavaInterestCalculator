# Configuration & Externalized Settings Inventory

The application has a small but security-sensitive configuration surface made up of Gradle build files, a servlet deployment descriptor, a Dockerfile, and a properties file with environment-variable overrides. No profile framework or external secret store was found.

## Configuration Sources

| Source | Type | Path/Location | Notes |
|---|---|---|---|
| Gradle build | Build configuration | `build.gradle` | Declares Java 8 WAR build and runtime dependencies |
| Gradle settings | Build configuration | `settings.gradle` | Sets root project name |
| Servlet deployment descriptor | Runtime configuration | `src/main/webapp/WEB-INF/web.xml` | Registers servlet mappings and startup servlet |
| Application properties | Runtime configuration | `src/main/resources/interest.properties` | Supplies DB coordinates and interest defaults |
| Packaged properties copy | Build output resource | `bin/main/interest.properties` | Mirrors the source properties values inside the repo |
| Dockerfile | Container runtime configuration | `Dockerfile` | Defines build image, runtime image, environment variables, and exposed port |

## Build Profiles

| Profile | Activation | Purpose | Key Dependencies/Plugins |
|---|---|---|---|
| Default Gradle build | Automatic | Compiles Java sources and packages a `ROOT.war` artifact | `java` and `war` plugins |

## Runtime Profiles

| Profile | Activation Method | Config Files | Key Overrides |
|---|---|---|---|
| Default runtime | Automatic | `interest.properties`, `web.xml`, `Dockerfile` | Environment variables can override DB connection settings and default interest values |

## Properties Inventory

| Property Key | Default | Profiles | Source |
|---|---|---|---|
| `db.host` / `DB_HOST` | `sqlserver` | Default | `src/main/resources/interest.properties`, `Dockerfile` |
| `db.port` / `DB_PORT` | `1433` | Default | `src/main/resources/interest.properties`, `Dockerfile` |
| `db.name` / `DB_NAME` | `ZavaBankDB` | Default | `src/main/resources/interest.properties`, `Dockerfile` |
| `db.user` / `DB_USER` | `[MASKED]` | Default | `src/main/resources/interest.properties`, `Dockerfile` |
| `db.password` / `DB_PASSWORD` | `[MASKED]` | Default | `src/main/resources/interest.properties`, `Dockerfile` |
| `default.annual.rate` / `DEFAULT_ANNUAL_RATE` | `0.015` | Default | `src/main/resources/interest.properties` |
| `default.compounds.per.year` / `DEFAULT_COMPOUNDS_PER_YEAR` | `12` | Default | `src/main/resources/interest.properties` |

## Startup Parameters & Resource Requirements

| Service | JVM/Runtime Options | Memory | Instance Count |
|---|---|---|---|
| Build container | None declared beyond Gradle build image | Not specified | 1 build stage |
| Runtime Tomcat container | No JVM flags or system properties declared | Not specified | 1 runtime container |

## Startup Dependency Chain

1. SQL Server must be reachable before the Tomcat runtime can serve business requests.
2. Tomcat initializes `InterestBootstrapServlet`, which attempts to create the `InterestAccruals` table during application startup.
3. After startup succeeds, clients can use `/health` and the interest calculation endpoints.

## Secrets & Sensitive Configuration

| Secret Reference | Type | Storage (masked) |
|---|---|---|
| `DB_USER` / `db.user` | Database username | Source properties file and Docker environment variable |
| `DB_PASSWORD` / `db.password` | Database password | Source properties file and Docker environment variable |

### Secrets Provisioning Workflow

Secrets are resolved locally from environment variables first and then fall back to the checked-in `interest.properties` file. The Docker image also bakes default database credentials into environment variables. No managed identity, secret manager, vault integration, or deployment-time secret injection workflow was identified.

## Feature Flags

| Flag Name | Default | Controlled By |
|---|---|---|
| None detected | N/A | N/A |

## Framework & Runtime Versions

| Component | Version | Source |
|---|---:|---|
| Java language level | 1.8 | `build.gradle` |
| Servlet API | 3.1.0 | `build.gradle` |
| SQL Server JDBC driver | 12.6.3.jre8 | `build.gradle` |
| JSON library | 20140107 | `build.gradle` |
| Build image | `gradle:7.6-jdk8` | `Dockerfile` |
| Runtime image | `tomcat:9-jdk8` | `Dockerfile` |
