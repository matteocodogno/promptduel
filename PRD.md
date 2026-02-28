# PromptDuel — Product Requirements Document

**Version:** 1.0  
**Date:** February 2026  
**Status:** Draft

---

## 1. Overview

### 1.1 Product Summary

PromptDuel is an educational, browser-based game that teaches prompt injection attack and defense techniques through adversarial gameplay. Two players compete in real time: the **Jailbreakers** (red team) attempt to manipulate an LLM-powered code review assistant into ignoring a known vulnerability, while the **Guardians** (blue team) iteratively harden the system prompt to resist the attack.

### 1.2 Problem Statement

Prompt injection is one of the most critical and poorly understood vulnerabilities in AI-powered applications. Developers and security professionals lack hands-on tools to learn both how to craft injections and how to defend against them. Existing resources are mostly theoretical; PromptDuel fills this gap with experiential, competitive learning.

### 1.3 Target Users

- **Security researchers** who want to explore and demonstrate prompt injection techniques in a controlled environment.
- **Developers learning about AI security** who need a practical, engaging way to understand both attack vectors and defensive prompting strategies.

### 1.4 Success Outcome

After a PromptDuel session, both players should be able to:
- Recognize and name the four main prompt injection attack vectors.
- Apply concrete system prompt hardening techniques to defend against each.
- Understand how the evolution of a system prompt reflects the iterative nature of AI security.

---

## 2. Core Concept

### 2.1 The Scenario

The game simulates an AI-powered code review assistant. A vulnerable piece of code (containing a SQL injection vulnerability) is submitted for review. The LLM's expected correct response is to flag the vulnerability. The Jailbreakers win a round if they can get the model to respond **"no issues found"** instead.

### 2.2 Team Roles

| Team | Name | Device | Goal |
|------|------|--------|------|
| Red Team | Jailbreakers | Own device | Craft a prompt injection that causes the LLM to ignore the vulnerability |
| Blue Team | Guardians | Own device | Write and iteratively improve the system prompt to resist the injection |

### 2.3 Win Condition

The Guardians successfully defend all four attack vectors in order. The game ends when the blue team's system prompt withstands the final attack.

---

## 3. Game Flow

### 3.1 Session Setup

1. The **first player** creates a new game session and receives a unique **Game ID** (also referred to as arena ID or room ID).
2. The first player chooses their team role (Jailbreakers or Guardians).
3. The first player shares the Game ID with their opponent.
4. The **second player** enters the Game ID to join the session and is assigned the remaining role.
5. Both players confirm readiness and the session begins.

> No account or registration is required. The session is entirely anonymous.

### 3.2 Turn Structure (Per Round)

Each round follows this sequence:

```
[Jailbreakers] Submit injection attempt
        ↓
[LLM] Evaluates the code + injected prompt against the current system prompt
        ↓
[System] Determines outcome: "vulnerability found" (Guardians win round) 
         or "no issues found" (Jailbreakers win round)
        ↓
If Jailbreakers win:
  → Guardians are notified
  → Guardians see the full injection attempt
  → Guardians update their system prompt (may request a hint)
  → Next attempt begins
        ↓
If Guardians win:
  → Round is complete
  → Game advances to the next attack vector
```

### 3.3 Attack Vector Progression

The game guides the Jailbreakers through four attack vectors in strict order. The next vector is only unlocked once the Guardians have successfully defended the current one.

| Round | Attack Vector | Description |
|-------|--------------|-------------|
| 1 | **Direct Override** | Add instructions directly before the code to override the review task |
| 2 | **Role Confusion** | Claim to be an authorized developer who has already approved the code |
| 3 | **Context Manipulation** | Claim the SQL injection vulnerability is intentional by design |
| 4 | **Indirect Injection** | Embed malicious instructions inside a comment within the code itself |

### 3.4 Hint System

When the Guardians are struggling to defend against an attack, they may request assistance in two tiers:

- **Tier 1 — Hint:** A short description of the type of attack being used (e.g., "The attacker is attempting to reframe their identity to claim authority").
- **Tier 2 — Example Solution:** A concrete example of a defensive prompt technique the Guardians can adapt and apply.

### 3.5 Disconnection Handling

If either player disconnects mid-session, the game pauses and preserves all state. The disconnected player can rejoin at any time using the original Game ID, and the session resumes exactly where it left off.

---

## 4. System Prompt Design

### 4.1 Starting Prompt

At the beginning of the game, the Guardians receive a **base system prompt** that is intentionally weak — designed to be vulnerable to the first attack vector. This ensures:
- The Jailbreakers win the first round quickly, providing an immediate learning moment.
- The Guardians start with a concrete baseline to improve upon.

### 4.2 Prompt Evolution

The Guardians may update their system prompt after each failed defense round. There is no limit to the number of update attempts per round. The full history of prompt versions is preserved and displayed in the session summary.

---

## 5. Session Summary

### 5.1 Availability

The session summary is accessible via a permanent URL using the Game ID:

```
/summary/{gameId}
```

This URL is available indefinitely and requires no authentication to view.

### 5.2 Content

The summary includes:

- **Session recap:** Teams, total rounds, number of attempts per round.
- **Game evolution timeline:** A chronological view of what happened each round — the injection attempt, the outcome, and the system prompt update made in response.
- **Attack vector descriptions:** An educational explanation of each of the four attack vectors encountered.
- **Defensive techniques implemented:** A description of the prompt hardening strategies the Guardians applied across the session.

### 5.3 Export

Users can export the summary as an **image file** for sharing or archival purposes.

---

## 6. Technical Architecture

### 6.1 Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React |
| Backend | Kotlin + Spring Boot 4 |
| Real-time communication | WebSockets |
| Database | PostgreSQL |
| LLM proxy | LiteLLM (model-agnostic) |
| Primary model | Llama 3 70B (self-hosted) |
| Fallback/alternative | Any model via LiteLLM |

### 6.2 LLM Integration

LiteLLM is used as a proxy layer to abstract the underlying model. This allows:
- Switching models without changing application code.
- Using a self-hosted Llama 3 70B instance as the default to minimize inference costs.
- Future flexibility to support other models or allow model selection.

LLM API costs are covered by the product team. Users do not need to supply API keys.

### 6.3 Real-time Communication

WebSocket connections manage live session state between two players, including:
- Notifying the Guardians when an injection attempt has been submitted and evaluated.
- Pushing round outcomes and prompt update requests in real time.
- Handling reconnection and session resume on disconnect.

### 6.4 Data Storage

PostgreSQL stores:
- Game sessions (ID, status, current round, team assignments).
- Round history (injection attempts, outcomes, timestamps).
- System prompt versions per session.
- Session summary data (persisted indefinitely by Game ID).

---

## 7. Non-Functional Requirements

| Requirement | Detail |
|-------------|--------|
| No authentication | Users play anonymously; no account creation required |
| Session persistence | Game state survives player disconnection |
| Summary permanence | Summary URL remains accessible indefinitely |
| Stateless frontend | All state managed by backend; reconnection fully supported |
| Model flexibility | LiteLLM abstraction allows model changes with zero code changes |

---

## 8. Out of Scope (v1)

The following are explicitly excluded from the first release:

- User accounts, profiles, or history across sessions.
- Leaderboards or scoring across multiple sessions.
- More than two players per session.
- Custom attack vectors beyond the four defined.
- Model selection by end users.
- Localization or multi-language support.

---

## 9. Glossary

| Term | Definition |
|------|-----------|
| **Game ID** | Unique identifier for a session, used to join and retrieve the summary |
| **Jailbreakers** | The red team; attempts to inject prompts to bypass the code review |
| **Guardians** | The blue team; hardens the system prompt against injection |
| **System Prompt** | The LLM instruction set controlled by the Guardians |
| **Injection Attempt** | The text submitted by the Jailbreakers alongside the vulnerable code |
| **Round** | One attack vector cycle — ends when Guardians successfully defend |
| **Session** | A full game covering all four attack vectors |