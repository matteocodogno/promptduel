# Implementation Plan

## PromptDuel — Implementation Tasks

---

- [ ] 1. Project Foundation and Environment Setup

- [x] 1.1 Initialize the Kotlin/Spring Boot backend project
  - Create a Maven project with Kotlin DSL, Spring Boot 4, Spring WebSocket (STOMP/SockJS), Spring Data JPA, PostgreSQL
    driver, and Spring Actuator
  - Set up application.yml with database connection properties, LiteLLM endpoint URL and timeout, WebSocket config, CORS allowed origin, and input length limits
  - Establish the Hexagonal Architecture package layout separating domain, application, infrastructure, and interface modules
  - _Requirements: 6.1_

- [x] 1.2 (P) Initialize the React/TypeScript frontend project <!-- gh:#1 -->
  - Bootstrap a Vite + React 19 + TypeScript project with strict mode enabled
  - Use Tanstack Query for data fetching and caching
  - Use React Router for client-side routing
  - Use React Query for data fetching and caching
  - Use React-Toastify for notifications
  - Use React-Loading-Skeleton for loading indicators
  - Use React-Error-Boundary for error handling
  - Use React-Query-Devtools for debugging
  - Use shadcn/ui as the UI library
  - Add @stomp/stompjs 7.x, sockjs-client 1.x, and html2canvas 1.x as dependencies
  - Configure environment variables for the backend REST base URL and WebSocket endpoint URL
  - _Requirements: 6.1_

- [x] 1.3 Create the PostgreSQL database schema <!-- gh:#2 -->
  - Write migration scripts (Liquibase) that create game_sessions, system_prompt_versions, and injection_attempts tables matching the physical data model in the design
  - Add the version column on game_sessions for optimistic locking, all foreign keys, and UNIQUE constraints on (game_session_id, round_number, version_number) and (game_session_id, round_number, attempt_number)
  - Create indices on injection_attempts.game_session_id and system_prompt_versions.game_session_id
  - Set NO TTL or auto-deletion policy; all rows must be retained indefinitely
  - _Requirements: 6.4_

- [x] 1.4 (P) Configure LiteLLM integration and connect to the local LiteLLM proxy <!-- gh:#3 -->
  - Add application properties for LiteLLM host (localhost), port (4000), model name (local-smart), and request timeout
    (default 30,000 ms). LiteLLM key is set in LITELLM_MASTER_KEY environment variable.
  - Connect to the local LiteLLM proxy using Spring http interfaces (@HttpExchange)
  - _Requirements: 6.2_

---

- [ ] 2. Domain Layer — Core Game Logic

- [x] 2.1 (P) Build the Attack Vector Registry <!-- gh:#4 -->
  - Define the four attack vectors in strict order: (1) Direct Override, (2) Role Confusion, (3) Context Manipulation, (4) Indirect Injection
  - For each vector store: round number, name, educational description, tier-1 hint text, and tier-2 example defensive prompt
  - Include the intentionally weak base system prompt given to Guardians at game start — designed to be defeated by round-1 immediately
  - Include the static vulnerable-code sample (SQL injection) used as the code-review input for every evaluation call
  - Keep the registry read-only with zero I/O or external dependencies
  - _Requirements: 2.1, 3.3, 3.4, 4.1_

- [ ] 2.2 (P) Build the Session State Machine <!-- gh:#5 -->
  - Model valid top-level states: WAITING_FOR_PLAYERS → IN_PROGRESS → COMPLETED with no other transitions allowed
  - Model per-round sub-states: ROUND_ACTIVE → EVALUATING → ROUND_RESULT (JAILBREAKER_WIN or GUARDIAN_WIN)
  - Return a typed error for any illegal transition rather than silently mutating state
  - Keep all logic pure — no I/O, no side effects, only input-to-output transformation
  - Mark the GameSession JPA entity with an optimistic-lock @Version field to prevent concurrent update conflicts
  - _Requirements: 2.3, 3.1, 3.5_

---

- [ ] 3. Infrastructure Layer — External Adapters and Persistence

- [ ] 3.1 (P) Implement the LLM Gateway adapter <!-- gh:#6 -->
  - Build an HTTP client (Spring RestClient or WebClient) that sends POST /chat/completions requests to the LiteLLM proxy in the OpenAI-compatible format (model, messages with system and user roles)
  - Map a successful 200 response to a structured result containing the response text, model name, and token count
  - Map HTTP 4xx/5xx responses and timeouts to distinct typed error values (HttpError, Timeout, ParseError)
  - Read LiteLLM host, port, model name, and timeout from application properties; never hard-code them
  - _Requirements: 6.2_

- [ ] 3.2 (P) Implement the Game Event Publisher <!-- gh:#7 -->
  - Use Spring's SimpMessagingTemplate to broadcast typed game events to /topic/game/{gameId} for all-player delivery
  - Support targeted delivery to /user/queue/game/{gameId} for player-specific messages (hints)
  - Serialize all event payloads to JSON with UUIDs as strings and Instant fields as ISO-8601 strings
  - _Requirements: 6.3_

- [ ] 3.3 (P) Implement repository adapters for game data persistence <!-- gh:#8 -->
  - Implement JPA entities and Spring Data repositories for GameSession, SystemPromptVersion, and InjectionAttempt
  - Support all operations required by application services: insert new sessions and prompt versions, find session by ID, update session status and current round, list all prompt versions and injection attempts by session
  - Enforce that no rows are deleted (repository must not expose any delete methods)
  - _Requirements: 6.4_

---

- [ ] 4. Application Layer — Business Services

- [ ] 4.1 Implement Game Service for session lifecycle <!-- gh:#9 -->
  - Create session: generate a UUID Game ID, assign the creator's chosen role, persist the session in WAITING_FOR_PLAYERS status, and persist the base system prompt as version 1
  - Join session: assign the second player to the remaining role; reject with an appropriate error if the session is not in WAITING_FOR_PLAYERS or already has two players
  - Handle player-ready signals: when both players have signaled ready, transition session to IN_PROGRESS and broadcast GAME_STARTED (including round 1, the current system prompt, and the attack vector name) to both players
  - Handle reconnection: load the full current session state from the database, assemble a GameSessionSnapshot, and send SESSION_RESUMED directly to the reconnecting player; broadcast PLAYER_RECONNECTED to the other player
  - Detect WebSocket disconnection events and broadcast PLAYER_DISCONNECTED to the remaining player
  - _Requirements: 2.2, 2.3, 3.1, 3.5_

- [ ] 4.2 (P) Implement Evaluation Service for LLM outcome classification <!-- gh:#10 -->
  - Assemble the user message by combining the static vulnerable code, the current attack vector context, and the Jailbreaker's injection text
  - Call the LLM Gateway with the Guardian's current system prompt as the system message and the assembled user message as the user turn
  - Classify the LLM response using pattern matching: "no issues found" (or equivalent) → JAILBREAKER_WIN; "SQL injection" or vulnerability flagged → GUARDIAN_WIN; anything else → AMBIGUOUS
  - When AMBIGUOUS, make a second LLM call with a judge prompt that asks the LLM to decide whether the vulnerability was flagged; resolve from that response
  - Return GUARDIAN_WIN if the LLM identified the SQL injection vulnerability; JAILBREAKER_WIN otherwise
  - Make the pattern list and judge prompt configurable via application properties
  - _Requirements: 2.1, 3.2_

- [ ] 4.3 Implement Round Service for the turn loop <!-- gh:#11 -->
  - Accept injection submissions from the Jailbreaker: validate that the session is IN_PROGRESS and the caller holds the JAILBREAKER role before delegating to Evaluation Service
  - Persist each InjectionAttempt with its injection text, full LLM response, evaluation method (PATTERN_MATCH or JUDGE_CALL), outcome, and the system prompt version ID used
  - On JAILBREAKER_WIN: broadcast ROUND_ATTEMPT_FAILED carrying the injection text, LLM response, and attempt number to both players; allow the Guardian to then update the system prompt
  - Accept system prompt updates from the Guardian: validate that the caller is GUARDIAN and the current round outcome is JAILBREAKER_WIN; persist a new SystemPromptVersion with an incremented version number; broadcast PROMPT_UPDATED
  - On GUARDIAN_WIN: advance the round counter on the session; if round 4 just completed, mark session COMPLETED and broadcast GAME_COMPLETED (including the summary URL); otherwise broadcast ROUND_COMPLETED and begin the next round
  - _Requirements: 3.2, 3.3, 4.2_

- [ ] 4.4 (P) Implement Hint Service <!-- gh:#12 -->
  - For tier-1 hints: retrieve the attack-type description for the current round's vector from the Attack Vector Registry and return it
  - For tier-2 hints: retrieve the example defensive prompt for the current round's vector and return it
  - Validate that the requesting player is the Guardian and the session is IN_PROGRESS before serving any hint
  - _Requirements: 3.4_

- [ ] 4.5 (P) Implement Summary Service <!-- gh:#13 -->
  - Assemble the full session summary by joining data from GameSession, SystemPromptVersion, and InjectionAttempt for the given Game ID
  - Include: overall session recap (teams, total rounds, number of attempts per round), chronological round timeline (each injection attempt with its outcome and subsequent prompt update), attack vector educational descriptions, and defensive techniques applied per round
  - Make the summary available by Game ID with no authentication or time limit
  - _Requirements: 5.1, 5.2_

---

- [ ] 5. Backend Interface — REST API

- [ ] 5.1 Implement REST controllers for session and summary endpoints <!-- gh:#14 -->
  - POST /api/sessions: accept and validate a role field (JAILBREAKER or GUARDIAN), call Game Service createSession, return gameId, playerId, and role; reject unknown roles with 400
  - POST /api/sessions/{gameId}/join: call Game Service joinSession; return 404 if session not found, 409 if session is full, 200 with gameId/playerId/role on success
  - GET /api/sessions/{gameId}/summary: delegate to Summary Service; return 404 if not found, 200 with SessionSummary JSON on success
  - GET /api/sessions/{gameId}/hints?round=&tier=: validate round and tier query parameters; delegate to Hint Service; return 400 on invalid input, 404 on missing session
  - Map all domain Result errors to appropriate HTTP status codes following the error categories in the design
  - _Requirements: 2.2, 3.1, 3.4, 5.1, 5.2_

- [ ] 5.2 (P) Configure CORS policy and input validation limits <!-- gh:#15 -->
  - Restrict CORS to the configured frontend origin only — explicitly disallow wildcard origins
  - Enforce a 2,000-character maximum on injectionText and a 4,000-character maximum on systemPrompt at the controller/handler boundary; reject with 400 if exceeded
  - _Requirements: 6.1_

---

- [ ] 6. Backend Interface — WebSocket Layer

- [ ] 6.1 Configure WebSocket infrastructure and player identity validation <!-- gh:#16 -->
  - Enable STOMP over SockJS with an in-memory SimpleBroker; configure /app as the application destination prefix, /topic and /user as broker prefixes
  - Implement a STOMP ChannelInterceptor that reads the playerId header on every CONNECT frame; validate it against the session in the database; disconnect the client with a typed error event if the playerId is absent or unknown
  - Set heartbeat to 10,000 ms on the broker relay to detect dead connections
  - _Requirements: 6.3, 3.5_

- [ ] 6.2 Implement the Game Message Handler <!-- gh:#17 -->
  - Route /app/game/{gameId}/ready frames to Game Service setPlayerReady
  - Route /app/game/{gameId}/inject frames to Round Service submitInjection (payload: injectionText)
  - Route /app/game/{gameId}/update-prompt frames to Round Service updateSystemPrompt (payload: systemPrompt)
  - Route /app/game/{gameId}/request-hint frames to Hint Service (payload: tier)
  - Route /app/game/{gameId}/reconnect frames to Game Service reconnectPlayer
  - When a business-rule validation fails (wrong role, wrong state), send a targeted 403-equivalent error event back to the originating player without affecting the other player
  - _Requirements: 3.2, 3.4, 3.5, 4.2_

---

- [ ] 7. Frontend Foundation — State Management and Communication

- [ ] 7.1 (P) Implement the WebSocket Client service <!-- gh:#18 -->
  - Build a STOMP client using @stomp/stompjs Client with SockJS as the WebSocket factory
  - On connect, subscribe to /topic/game/{gameId} and forward all received events to a caller-supplied event handler
  - Set heartbeat to 10,000 ms and reconnect delay to 3,000 ms; on reconnect, automatically re-subscribe and send a reconnect frame to /app/game/{gameId}/reconnect
  - Persist gameId and playerId to localStorage on session creation and join so a full page refresh triggers automatic reconnection via the stored identifiers
  - _Requirements: 6.3, 3.5_

- [ ] 7.2 (P) Implement the Game Context and state reducer <!-- gh:#19 -->
  - Define the full GameState shape: gameId, playerId, myRole, status, currentRound, currentSystemPrompt, opponentConnected, rounds (with attempts), and pendingEvaluation flag
  - Implement a useReducer dispatcher that handles all incoming GameEvent types and transitions state immutably: PLAYER_JOINED, GAME_STARTED, ROUND_ATTEMPT_FAILED, PROMPT_UPDATED, ROUND_COMPLETED, GAME_COMPLETED, PLAYER_DISCONNECTED, PLAYER_RECONNECTED, SESSION_RESUMED, HINT_RECEIVED
  - Expose context actions: createSession (REST → store gameId/playerId → connect WebSocket), joinSession (REST → store → connect), setReady (WebSocket send), submitInjection (WebSocket send), updateSystemPrompt (WebSocket send), requestHint (WebSocket send)
  - Set pendingEvaluation to true on injection submit and false when any round outcome event arrives
  - _Requirements: 2.2, 3.1, 3.2, 3.4, 3.5, 4.2_

---

- [ ] 8. Frontend UI — Game Pages

- [ ] 8.1 (P) Build the Lobby Page <!-- gh:#20 -->
  - Show two options: create a new session (with role selector: Jailbreaker or Guardian) and join an existing session (with a Game ID input)
  - After session creation, display the Game ID prominently with a copy-to-clipboard action and instructions to share it with the opponent
  - After joining, show a waiting room indicator for both players until both have signaled ready; then send the ready frame automatically or via a "Ready" button
  - Handle 404 (session not found) and 409 (session full) join errors with inline user-visible messages
  - _Requirements: 2.2, 3.1_

- [ ] 8.2 (P) Build the Jailbreaker game panel <!-- gh:#21 -->
  - Display the current attack vector number, name, and educational description as context for the player's attack strategy
  - Show the vulnerable SQL injection code sample formatted as read-only code for the current round
  - Provide a textarea for composing the injection attempt and a submit button that is disabled while evaluation is pending
  - Show a loading / evaluating indicator while pendingEvaluation is true
  - After evaluation, display the LLM's full response and the round outcome (GUARDIAN_WIN = round lost, advance; JAILBREAKER_WIN = prompt updated, try again)
  - _Requirements: 2.1, 3.2_

- [ ] 8.3 (P) Build the Guardian game panel <!-- gh:#22 -->
  - Show the current system prompt in an editable textarea; initially populated with the base system prompt from the GAME_STARTED event
  - When a ROUND_ATTEMPT_FAILED event arrives, display the Jailbreaker's injection text and the LLM's response so the Guardian can learn from the breach
  - After a failed defense, enable the system prompt editor and a "Update Prompt" submit button; disable them while evaluation is in progress
  - Provide two hint buttons (Tier 1: Attack Type, Tier 2: Example Defense); on click, send a hint request and display the returned hint content inline below the prompt editor
  - Show a GUARDIAN_WIN success indicator and disable editing when the round is won
  - _Requirements: 3.2, 3.4, 4.2_

- [ ] 8.4 (P) Build the Summary Page <!-- gh:#23 -->
  - Accept the gameId from the URL path (/summary/{gameId}) and fetch summary data from GET /api/sessions/{gameId}/summary on mount
  - Display the session recap: team roles, total rounds played, and number of injection attempts per round
  - Render a chronological timeline per round: each injection attempt (injection text, LLM response, outcome) paired with the prompt update that followed it
  - Show educational descriptions of all four attack vectors and list the defensive techniques the Guardians applied across the session
  - Implement an "Export as Image" button that uses html2canvas to capture the summary DOM element and triggers a download; no backend involvement
  - Ensure the page is publicly accessible by Game ID with no login or token required
  - _Requirements: 5.1, 5.2, 5.3_

---

- [ ] 9. Integration, Error Handling, and Security

- [ ] 9.1 Wire frontend and backend together for the full game flow <!-- gh:#24 -->
  - Verify the complete end-to-end path: create session (REST) → join session (REST) → both ready (WebSocket) → Jailbreaker injects (WebSocket) → LLM evaluates → ROUND_ATTEMPT_FAILED delivered to both → Guardian updates prompt (WebSocket) → Jailbreaker injects again → GUARDIAN_WIN → ROUND_COMPLETED → repeat for rounds 2–4 → GAME_COMPLETED with summary URL
  - Confirm that a page refresh on either client correctly reconnects via localStorage-stored credentials and receives SESSION_RESUMED with full game state
  - Validate that GAME_COMPLETED delivers the /summary/{gameId} URL and that the Summary Page renders the full session data
  - _Requirements: 2.3, 3.1, 3.2, 3.3, 3.5, 5.1_

- [ ] 9.2 Implement LLM failure handling and graceful degradation <!-- gh:#25 -->
  - When the LLM Gateway returns an error (502, 504, or parse failure), keep the round in EVALUATING state — do not advance the round or corrupt session data
  - Broadcast a typed error event (LLM_ERROR) to both players so the Jailbreaker sees a human-readable retry prompt
  - Log every LLM Gateway call with gameId, roundNumber, attemptNumber, durationMs, evaluation method, and outcome using structured logging; log AMBIGUOUS classifications separately for accuracy auditing
  - _Requirements: 6.2_

- [ ] 9.3 Expose health monitoring and validate security settings <!-- gh:#26 -->
  - Configure Spring Actuator to expose /actuator/health with a custom LiteLLM reachability indicator that attempts a lightweight ping to the LiteLLM proxy
  - Verify that CORS is restricted to the configured frontend origin (not wildcard) in both REST and WebSocket configuration
  - Verify that the LiteLLM API key is read from an environment variable and never appears in logs, responses, or frontend-accessible endpoints
  - _Requirements: 6.1, 6.2_

---

- [ ] 10. Testing

- [ ] 10.1 (P) Write unit tests for domain and application logic <!-- gh:#27 -->
  - Test SessionStateMachine: all valid state transitions succeed, all invalid transitions return errors; cover both top-level and per-round sub-states
  - Test EvaluationService: pattern-match returns GUARDIAN_WIN for vulnerability-flagged responses, JAILBREAKER_WIN for "no issues found", and AMBIGUOUS for unclear responses; verify that AMBIGUOUS triggers a second judge LLM call
  - Test AttackVectorRegistry: correct vector data retrieved for rounds 1–4; base system prompt returned without error
  - Test GameService.createSession and joinSession: correct role assignment, second-player capacity enforcement, rejection of join on full sessions, and rejection on wrong session status
  - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.3, 4.1_

- [ ] 10.2 (P) Write integration tests for application and infrastructure layers <!-- gh:#28 -->
  - Test the full injection turn via RoundService: submit injection → EvaluationService with mocked LlmGateway → InjectionAttempt persisted → correct events published
  - Test GameService reconnection: simulate disconnect notification, verify DB state is unchanged, verify snapshot assembled from DB matches current session state
  - Test SessionController endpoints via MockMvc: create session (200/400), join session (200/404/409), get summary (200/404), get hint (200/400/404)
  - Test LlmGateway with WireMock simulating the LiteLLM proxy: verify correct request format, successful response mapping, and correct error types for 500 and timeout scenarios
  - _Requirements: 3.2, 3.5, 5.1, 5.2, 6.2_

- [ ] 10.3 Write end-to-end tests for complete game scenarios <!-- gh:#29 -->
  - Simulate a full two-player session from creation to completion: create → join → ready → inject (JAILBREAKER_WIN) → update prompt → inject again (GUARDIAN_WIN) → advance through all four rounds → GAME_COMPLETED event received → summary URL accessible and returns valid data
  - Simulate disconnection and reconnection mid-round: disconnect one player, verify the other receives PLAYER_DISCONNECTED, reconnect, verify SESSION_RESUMED restores correct state, verify the other receives PLAYER_RECONNECTED
  - Verify the Summary Page renders all timeline data and that the html2canvas image export produces a non-empty output
  - _Requirements: 2.3, 3.2, 3.5, 5.1, 5.2, 5.3_
