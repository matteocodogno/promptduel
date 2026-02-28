---
id: TASK-15
title: Implement REST controllers for session and summary endpoints
status: To Do
assignee: []
created_date: '2026-02-28 19:16'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Expose the REST API surface so clients can create sessions, join sessions, retrieve summaries, and request hints.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 POST /api/sessions validates role input, calls Game Service, returns gameId/playerId/role; rejects unknown roles with 400
- [ ] #2 POST /api/sessions/{gameId}/join returns 404 if session not found, 409 if session is full, 200 with gameId/playerId/role on success
- [ ] #3 GET /api/sessions/{gameId}/summary returns 404 if not found, 200 with SessionSummary JSON on success
- [ ] #4 GET /api/sessions/{gameId}/hints validates round and tier query params, returns 400 on invalid input, 404 on missing session
- [ ] #5 All domain Result errors mapped to appropriate HTTP status codes
<!-- AC:END -->
