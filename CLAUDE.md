## Execution rules
- Before implementing any task: show a plan and wait for explicit approval
- After each task: stop, show git diff summary + test results, wait for GO/NO
- Never move to the next task without a human "go ahead"
- If tests fail: surface the failure, propose a fix, wait for approval before applying

## GitHub Issues sync rules
- When tasks.md is finalized: create one GitHub Issue per task
    - Title: task title from tasks.md
    - Body: acceptance criteria + link to .kiro/specs/<feature>/tasks.md
    - Label: "spec-task", milestone = feature name
    - Save the issue number back into tasks.md as a comment: <!-- gh:#42 -->
- When starting a task: add label "in-progress" to its issue
- When a task checkbox is ticked - [x]: close the issue with "Closes #N" in the commit footer
- Never close an issue without the corresponding checkbox being ticked first
- Never tick a checkbox without closing the issue (keep them atomic)

## Commit message rules (Conventional Commits)
- Format: <type>(<scope>): <description>
- Types: feat | fix | docs | style | refactor | test | chore | perf | ci
- scope = task ID or module name, e.g. feat(task-7): or fix(auth):
- Description: imperative, lowercase, no period, max 72 chars
- Breaking changes: add BREAKING CHANGE: in footer
- Examples:
  feat(stripe): add webhook signature validation
  fix(auth): handle 401 on token refresh
  test(pdf): add export edge case coverage
  chore(deps): upgrade stripe-node to v14
- NEVER use generic messages like "fix bug" or "update code"

## Pre-commit checklist (run in order, block commit if any fail)
# Note: linting runs automatically via git pre-commit hook
1. mise run test        ← all tests must pass
2. Update README.md     ← reflect any changed public API, commands, or config
3. git add -A && git commit ...

## README rules
- README.md is generated from design.md at the start of Phase 03
- Update README.md before every commit — this is mandatory, not optional
- Update only the sections affected by that task
- Sections to maintain: Overview, Prerequisites, Installation, Usage,
  Configuration, API/CLI reference, Architecture, Contributing
- Never remove a section — add a "Coming soon" note if not yet implemented
- Keep it runnable: every code block in README must act

# AI-DLC and Spec-Driven Development

Kiro-style Spec Driven Development implementation on AI-DLC (AI Development Life Cycle)

## Project Context

### Paths
- Steering: `.kiro/steering/`
- Specs: `.kiro/specs/`

### Steering vs Specification

**Steering** (`.kiro/steering/`) - Guide AI with project-wide rules and context
**Specs** (`.kiro/specs/`) - Formalize development process for individual features

### Active Specifications
- Check `.kiro/specs/` for active specifications
- Use `/kiro:spec-status [feature-name]` to check progress

## Development Guidelines
- Think in English, generate responses in English. All Markdown content written to project files (e.g., requirements.md, design.md, tasks.md, research.md, validation reports) MUST be written in the target language configured for this specification (see spec.json.language).

## Minimal Workflow
- Phase 0 (optional): `/kiro:steering`, `/kiro:steering-custom`
- Phase 1 (Specification):
  - `/kiro:spec-init "description"`
  - `/kiro:spec-requirements {feature}`
  - `/kiro:validate-gap {feature}` (optional: for existing codebase)
  - `/kiro:spec-design {feature} [-y]`
  - `/kiro:validate-design {feature}` (optional: design review)
  - `/kiro:spec-tasks {feature} [-y]`
- Phase 2 (Implementation): `/kiro:spec-impl {feature} [tasks]`
  - `/kiro:validate-impl {feature}` (optional: after implementation)
- Progress check: `/kiro:spec-status {feature}` (use anytime)

## Development Rules
- 3-phase approval workflow: Requirements → Design → Tasks → Implementation
- Human review required each phase; use `-y` only for intentional fast-track
- Keep steering current and verify alignment with `/kiro:spec-status`
- Follow the user's instructions precisely, and within that scope act autonomously: gather the necessary context and complete the requested work end-to-end in this run, asking questions only when essential information is missing or the instructions are critically ambiguous.

## Steering Configuration
- Load entire `.kiro/steering/` as project memory
- Default files: `product.md`, `tech.md`, `structure.md`
- Custom files are supported (managed via `/kiro:steering-custom`)
