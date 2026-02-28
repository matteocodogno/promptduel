---
id: TASK-1
title: Initialize the Kotlin/Spring Boot backend project
status: Done
assignee:
  - '@claude'
created_date: '2026-02-28 18:37'
updated_date: '2026-02-28 19:55'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Set up the backend project structure that all subsequent backend work will build upon.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Gradle project created with Kotlin DSL, Spring Boot 4, Spring WebSocket (STOMP/SockJS), Spring Data JPA, PostgreSQL driver, and Spring Actuator
- [x] #2 application.yml configured with database connection, LiteLLM endpoint URL and timeout, WebSocket config, CORS allowed origin, and input length limits
- [x] #3 Hexagonal Architecture package layout established with separate domain, application, infrastructure, and interface modules
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create backend/ directory tree
2. Write pom.xml with all starters, Kotlin plugin (allopen + noarg), and maven-compiler-plugin disabled
3. Create .mvn/wrapper/maven-wrapper.properties + mvnw scripts
4. Write PromptDuelApplication.kt main class
5. Create four empty package directories with .gitkeep
6. Write application.yml with all AC#2 properties
7. Write application-test.yml (disables Flyway + JPA validation for smoke test)
8. Write PromptDuelApplicationTests.kt using TestContainers PostgreSQL
9. Run ./mvnw test to verify
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Initialized the Kotlin/Spring Boot backend under backend/.

Changes:
- pom.xml: Spring Boot 4.0.0 parent, Kotlin 2.2.0, all required starters (web, websocket, data-jpa, actuator, validation), PostgreSQL driver, Flyway, jackson-module-kotlin, TestContainers BOM 1.20.4. maven-compiler-plugin default executions disabled; kotlin-maven-plugin with allopen+noarg handles all compilation.
- mvnw / mvnw.cmd: wrapper scripts using maven-wrapper.jar (downloads Maven 3.9.9 on first run).
- application.yml: full property map covering datasource, JPA, Flyway, Actuator, CORS allowed-origin, LiteLLM host/port/model/timeout/api-key, and input length limits — all using ${ENV_VAR:default} substitution.
- application-test.yml: disables Flyway and sets ddl-auto=none for the context smoke test (no migrations exist yet; re-enabled in TASK-3).
- PromptDuelApplication.kt: @SpringBootApplication entry point.
- Four empty packages created: domain/, application/, infrastructure/, interfaces/ (note: "interfaces" avoids the Kotlin reserved keyword).
- PromptDuelApplicationTests.kt: @SpringBootTest(NONE) + TestContainers PostgreSQL 16 + @DynamicPropertySource.

Note: spring.jackson.serialization config removed — Spring Boot 4 uses Jackson 3.x (tools.jackson.*) with different enum binding; Jackson will be configured in code in a later task.

Tests: ./mvnw test → BUILD SUCCESS, 1 test, 0 failures.
<!-- SECTION:FINAL_SUMMARY:END -->
