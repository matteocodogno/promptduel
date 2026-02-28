---
id: TASK-9
title: Implement repository adapters for game data persistence
status: To Do
assignee: []
created_date: '2026-02-28 18:39'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Persist all game data to PostgreSQL so sessions survive disconnections and are available for summaries.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 JPA entities and Spring Data repositories implemented for GameSession, SystemPromptVersion, and InjectionAttempt
- [ ] #2 All required read/write operations supported: insert sessions and prompt versions, find by ID, update session status and round counter, list attempts and versions by session
- [ ] #3 No delete methods exposed on any repository
<!-- AC:END -->
