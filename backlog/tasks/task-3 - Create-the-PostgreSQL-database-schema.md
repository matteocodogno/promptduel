---
id: TASK-3
title: Create the PostgreSQL database schema
status: To Do
assignee: []
created_date: '2026-02-28 18:37'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Define and migrate the database schema that stores all game session data persistently.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Migration scripts create game_sessions, system_prompt_versions, and injection_attempts tables matching the physical data model
- [ ] #2 version column added to game_sessions for optimistic locking; all foreign keys and UNIQUE constraints on (game_session_id, round_number, version_number) and (game_session_id, round_number, attempt_number) are in place
- [ ] #3 Indices created on injection_attempts.game_session_id and system_prompt_versions.game_session_id
- [ ] #4 No TTL or auto-deletion policy; all rows are retained indefinitely
<!-- AC:END -->
