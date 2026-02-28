---
id: TASK-17
title: Configure WebSocket infrastructure and player identity validation
status: To Do
assignee: []
created_date: '2026-02-28 19:17'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Bootstrap the WebSocket broker and validate player identity on every connection to prevent session hijacking.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 STOMP over SockJS enabled with in-memory SimpleBroker; /app prefix for application destinations, /topic and /user prefixes for broker destinations
- [ ] #2 STOMP ChannelInterceptor reads the playerId header on every CONNECT frame and validates it against the session in the database
- [ ] #3 Clients with absent or unknown playerId are disconnected with a typed error event
- [ ] #4 Heartbeat set to 10,000 ms on the broker relay to detect dead connections
<!-- AC:END -->
