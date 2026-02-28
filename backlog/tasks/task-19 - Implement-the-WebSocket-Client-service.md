---
id: TASK-19
title: Implement the WebSocket Client service
status: To Do
assignee: []
created_date: '2026-02-28 19:17'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Manage the STOMP over SockJS connection lifecycle so the frontend can send and receive real-time game events.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 STOMP client built using @stomp/stompjs with SockJS as the WebSocket factory
- [ ] #2 On connect, client subscribes to /topic/game/{gameId} and forwards events to a caller-supplied event handler
- [ ] #3 Heartbeat set to 10,000 ms and reconnect delay to 3,000 ms; on reconnect, client re-subscribes and sends a reconnect frame
- [ ] #4 gameId and playerId persisted to localStorage on session creation and join so a page refresh triggers automatic reconnection
<!-- AC:END -->
