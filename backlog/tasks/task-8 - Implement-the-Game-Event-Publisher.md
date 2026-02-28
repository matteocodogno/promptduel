---
id: TASK-8
title: Implement the Game Event Publisher
status: To Do
assignee: []
created_date: '2026-02-28 18:39'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Enable real-time event delivery to both players via WebSocket STOMP topics.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Typed game events broadcast to /topic/game/{gameId} for delivery to all players in a session
- [ ] #2 Targeted delivery supported to /user/queue/game/{gameId} for player-specific messages such as hints
- [ ] #3 All event payloads serialized to JSON with UUIDs as strings and Instant fields as ISO-8601 strings
<!-- AC:END -->
