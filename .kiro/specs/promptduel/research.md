# Research & Design Decisions

---
**Purpose**: Capture discovery findings, architectural investigations, and rationale that inform the technical design.

---

## Summary

- **Feature**: `promptduel`
- **Discovery Scope**: New Feature (Greenfield) — Complex Integration
- **Key Findings**:
  - Spring Boot 4.0.0 (released November 21, 2025) is confirmed available; Kotlin 2.2 is the official baseline.
  - LiteLLM Proxy exposes an OpenAI-compatible `/chat/completions` endpoint — any HTTP client can integrate without LLM-vendor lock-in.
  - STOMP over SockJS with Spring's in-memory broker is sufficient for 2-player sessions; an external broker (RabbitMQ) is not required.

---

## Research Log

### Spring Boot 4 + Kotlin WebSocket Support

- **Context**: Requirements specify Kotlin + Spring Boot 4 as the backend stack with WebSocket for real-time communication.
- **Sources Consulted**:
  - [JetBrains: Spring Boot 4 Kotlin baseline](https://blog.jetbrains.com/idea/2025/11/spring-boot-4/)
  - [Spring Blog: Next level Kotlin support in Spring Boot 4](https://spring.io/blog/2025/12/18/next-level-kotlin-support-in-spring-boot-4/)
  - [Baeldung: Intro to WebSockets with Spring](https://www.baeldung.com/websockets-spring)
  - [Spring.io: Getting Started with STOMP WebSocket](https://spring.io/guides/gs/messaging-stomp-websocket/)
- **Findings**:
  - Spring Boot 4 requires Kotlin 2.2+; ships `spring-boot-starter-kotlin-serialization` natively.
  - STOMP over WebSocket with `@EnableMessageBroker` and `WebSocketMessageBrokerConfigurer` is well-supported.
  - SockJS fallback handles browsers without WebSocket support (HTTP Streaming / Long Polling).
  - In-memory `SimpleBroker` is appropriate for 2-player sessions (no fan-out at scale required).
  - Player-specific messages routed via `/user/queue/...` using Spring's `SimpMessagingTemplate.convertAndSendToUser`.
  - Anonymous identity can be established by passing a `playerId` header on the STOMP CONNECT frame and mapping it via `ChannelInterceptor`.
- **Implications**: Backend uses `@EnableMessageBroker` with in-memory broker. External broker (RabbitMQ) deferred to post-v1. Player identification uses UUID-based tokens passed at STOMP connect time.

---

### LiteLLM Proxy Integration

- **Context**: LiteLLM is specified as the LLM abstraction layer in front of Llama 3 70B.
- **Sources Consulted**:
  - [LiteLLM Docs: Proxy Quick Start](https://docs.litellm.ai/docs/simple_proxy)
  - [LiteLLM Docs: OpenAI-Compatible Endpoints](https://docs.litellm.ai/docs/providers/openai_compatible)
  - [LiteLLM GitHub](https://github.com/BerriAI/litellm)
- **Findings**:
  - LiteLLM Proxy exposes `POST /chat/completions` in OpenAI format at `http://<host>:4000`.
  - Spring Boot backend integrates via standard HTTP POST — no Java SDK required.
  - Model selection via `model` field; `api_base` in LiteLLM config routes to Llama 3 70B.
  - Supports swapping models by updating proxy config without code changes.
- **Implications**: `LlmGateway` is a thin HTTP adapter calling `POST /chat/completions`. Model-switching requires no backend code changes.

---

### React + STOMP Client

- **Context**: Frontend must connect to Spring STOMP/SockJS endpoint for real-time game events.
- **Sources Consulted**:
  - [StompJS Guide: Using STOMP with SockJS](https://stomp-js.github.io/guide/stompjs/rx-stomp/using-stomp-with-sockjs.html)
  - [Medium: Complete Guide to STOMP.js in React/TypeScript](https://medium.com/front-end-world/a-complete-guide-to-using-stomp-js-and-sockjs-in-react-react-native-typescript-0d8bade60b48)
  - [npm: react-stomp-hooks](https://www.npmjs.com/package/react-stomp-hooks)
- **Findings**:
  - `@stomp/stompjs` + `sockjs-client` is the standard combination for React + Spring WebSocket.
  - `react-stomp-hooks` provides a React-idiomatic wrapper but adds abstraction overhead.
  - Direct `@stomp/stompjs` Client with React Context is more transparent and testable.
  - STOMP `connectHeaders` supports custom fields (used for `playerId` passing).
- **Implications**: Frontend uses `@stomp/stompjs` directly via a `WebSocketClient` service class wrapped in React Context, not a third-party hook library.

---

### LLM Response Evaluation Strategy

- **Context**: The system must determine whether the LLM's code review response flagged the SQL injection vulnerability.
- **Sources Consulted**: Domain analysis of requirements section 2.1, 3.2.
- **Findings**:
  - Requirements state the win condition as getting the LLM to respond "no issues found" — a semantic, not literal, outcome.
  - Pattern matching on negative phrases ("no issues", "no vulnerabilities", "looks clean", "code is safe") is fast but fragile.
  - A secondary judge LLM call ("Did this response flag a SQL injection vulnerability? Answer YES or NO.") is more robust but adds latency.
  - A two-stage strategy: pattern matching first, fallback to judge call on ambiguous responses, is optimal.
- **Implications**: `EvaluationService` uses a configurable two-stage evaluation strategy. The pattern list and judge call are both configurable via application properties.

---

### Session Persistence & Reconnection

- **Context**: Requirement 3.5 mandates session survival through player disconnection with resume capability.
- **Sources Consulted**: Spring WebSocket docs, domain analysis.
- **Findings**:
  - WebSocket sessions are transient; all persistent state must live in PostgreSQL.
  - In-memory `WebSocketSessionRegistry` maps `playerId` → active STOMP session; nulled on disconnect.
  - On reconnect, player sends the same `playerId` header; server restores their subscription routing.
  - Opponent is notified of disconnection/reconnection via `/topic/game/{gameId}` broadcast.
- **Implications**: All game state reads come from PostgreSQL, not in-memory. WebSocket registry is purely a routing concern.

---

### Image Export for Summary

- **Context**: Requirement 5.3 requires exporting the summary as an image file.
- **Sources Consulted**: Standard web techniques.
- **Findings**:
  - `html2canvas` is the standard browser-side DOM-to-image library; no server-side rendering needed.
  - `dom-to-image-more` is a modern alternative with better CSS support.
  - Both libraries operate client-side only — zero backend involvement needed.
- **Implications**: Image export is a pure frontend concern implemented with `html2canvas` on the `SummaryPage`.

---

## Architecture Pattern Evaluation

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| Hexagonal / Clean | Domain core isolated from I/O adapters | Testable domain logic, clear seams for parallel implementation | More scaffolding upfront | Fits multi-adapter pattern: WebSocket + REST + LiteLLM + PostgreSQL |
| Layered MVC | Controller → Service → Repository | Simple, familiar to Spring developers | Game domain logic scattered across services | Risk of controller-to-repo coupling |
| Event-Driven | Game events as first-class domain events | Natural fit for real-time game flow | Over-engineered for 2-player synchronous game | Useful pattern within the domain layer, not as full architecture |

**Selected**: Hexagonal / Clean Architecture for the backend. The game domain (session lifecycle, round state machine, evaluation outcome) is isolated from WebSocket, REST, LiteLLM, and PostgreSQL adapters. This enables parallel implementation of adapters and isolated domain unit tests.

---

## Design Decisions

### Decision: In-Memory STOMP Broker vs External Broker

- **Context**: Real-time communication between two players per session.
- **Alternatives Considered**:
  1. Spring in-memory `SimpleBroker` — no external dependency
  2. RabbitMQ with STOMP plugin — production-grade fan-out
- **Selected Approach**: In-memory `SimpleBroker` for v1.
- **Rationale**: Sessions have exactly 2 WebSocket connections; horizontal scaling is out of scope for v1. No fan-out requirement beyond 2 players.
- **Trade-offs**: Cannot horizontally scale the backend without adding a broker. Acceptable for v1.
- **Follow-up**: Add RabbitMQ if horizontal scaling is needed post-v1.

---

### Decision: Anonymous Player Identity (No Auth)

- **Context**: Requirements explicitly forbid accounts or registration (7, 3.1).
- **Alternatives Considered**:
  1. Server-assigned UUID token returned on session create/join, stored in `localStorage`.
  2. Cookie-based session with server-side session store.
- **Selected Approach**: UUID token returned from REST API, passed as STOMP `connectHeader.playerId` on WebSocket connect.
- **Rationale**: Stateless from the frontend perspective; survives page refresh without server-side session state. Reconnection just requires the same `playerId`.
- **Trade-offs**: `playerId` stored in `localStorage` is not encrypted. Acceptable since this is a no-auth educational game.
- **Follow-up**: Consider short-lived signed tokens if security concerns arise.

---

### Decision: LLM Win-Condition Evaluation Strategy

- **Context**: The system must classify the LLM's code review response as "vulnerability found" or "no issues found".
- **Alternatives Considered**:
  1. Keyword/pattern matching only — fast, zero-cost, fragile.
  2. Judge LLM call only — robust, adds latency and cost.
  3. Pattern matching first, judge call on ambiguous responses — balanced.
- **Selected Approach**: Two-stage configurable strategy: pattern match → judge call on `AMBIGUOUS` classification.
- **Rationale**: Keeps fast-path latency low for clear cases; handles edge cases where LLM response doesn't contain obvious keywords.
- **Trade-offs**: Adds complexity to `EvaluationService`. Pattern list must be maintained.
- **Follow-up**: Monitor evaluation accuracy in session summaries; tune pattern list accordingly.

---

### Decision: Static Attack Vector & Hint Data

- **Context**: Four attack vectors with predefined hints are specified in requirements (3.3, 3.4).
- **Alternatives Considered**:
  1. Database-stored attack vectors — dynamic, configurable via admin UI.
  2. Static configuration (YAML / in-memory enum) — simple, version-controlled.
- **Selected Approach**: Static in-memory `AttackVectorRegistry` backed by application configuration.
- **Rationale**: Attack vectors are fixed for v1 (8 = Out of Scope). No need for database I/O on every request.
- **Trade-offs**: Adding/changing vectors requires code deployment. Acceptable for v1.
- **Follow-up**: Extract to database if custom attack vectors are added in v2.

---

## Risks & Mitigations

- **LLM Latency**: Llama 3 70B inference may be slow (2–10 seconds). Mitigation: Show a loading state during evaluation; use a streaming response if LiteLLM supports it.
- **LiteLLM Proxy Unavailability**: If the proxy is down, injection attempts fail. Mitigation: `EvaluationService` returns a structured `LlmError`; game state is preserved; client shows a retry prompt.
- **WebSocket Connection Drops**: Clients on mobile/weak networks may disconnect frequently. Mitigation: Reconnection flow is explicit in the design; all state persisted in PostgreSQL.
- **Win-Condition Evaluation Errors**: A false "no issues found" classification could unfairly award a round. Mitigation: Log all LLM responses and evaluation outcomes in `injection_attempts` for manual review.
- **Spring Boot 4 Maturity**: Released November 2025 — early-adopter risk. Mitigation: Stick to stable Spring WebSocket APIs; avoid experimental features.

---

## References

- [Spring Boot 4 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Next Level Kotlin Support in Spring Boot 4](https://spring.io/blog/2025/12/18/next-level-kotlin-support-in-spring-boot-4/)
- [Baeldung: WebSockets with Spring](https://www.baeldung.com/websockets-spring)
- [Spring Guide: STOMP WebSocket](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [LiteLLM Proxy Documentation](https://docs.litellm.ai/docs/simple_proxy)
- [LiteLLM OpenAI-Compatible Endpoints](https://docs.litellm.ai/docs/providers/openai_compatible)
- [StompJS: Using STOMP with SockJS](https://stomp-js.github.io/guide/stompjs/rx-stomp/using-stomp-with-sockjs.html)
- [Medium: STOMP.js in React/TypeScript](https://medium.com/front-end-world/a-complete-guide-to-using-stomp-js-and-sockjs-in-react-react-native-typescript-0d8bade60b48)
