---
description: Testing conventions for Ballooner unit tests.
applyTo: "app/src/**/test/**/*.kt"
---

# Test conventions

These rules apply only to unit test files. They complement the project-wide
rules in [AGENTS.md](../../AGENTS.md).

## Structure

- One behavior per test. Prefer several small tests over one big one.
- Name tests with backticked sentences describing behavior, e.g.
  `` fun `emits Empty when the comic has no panels`() ``.
- Follow Arrange / Act / Assert, separated by blank lines.

## Flows and coroutines

- Assert on `Flow` emissions with **Turbine** (`flow.test { ... }`), not by
  collecting into a list manually.
- Use `runTest` and an injected test dispatcher. Never call `Thread.sleep`.
- Advance virtual time with the test scheduler instead of real delays.

## Room

- Use an **in-memory** database (`Room.inMemoryDatabaseBuilder(...)`) and close
  it in `@After`. Never touch the real on-device database from a test.

## Doubles

- Prefer hand-written fakes over mocking frameworks for repositories.
- Assert on observable behavior (emitted state), not on which methods were
  called.
