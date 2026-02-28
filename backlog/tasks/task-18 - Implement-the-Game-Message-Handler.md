---
id: TASK-18
title: Implement the Game Message Handler
status: To Do
assignee: []
created_date: '2026-02-28 19:17'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Route all inbound STOMP game messages to the correct application service handlers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 /app/game/{gameId}/ready frames routed to Game Service setPlayerReady
- [ ] #2 /app/game/{gameId}/inject frames routed to Round Service submitInjection with injectionText payload
- [ ] #3 /app/game/{gameId}/update-prompt frames routed to Round Service updateSystemPrompt with systemPrompt payload
- [ ] #4 /app/game/{gameId}/request-hint frames routed to Hint Service with tier payload
- [ ] #5 /app/game/{gameId}/reconnect frames routed to Game Service reconnectPlayer
- [ ] #6 Business-rule validation failures send a targeted 403-equivalent error event to the originating player only
<!-- AC:END -->
