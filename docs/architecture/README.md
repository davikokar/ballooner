# Architecture

This directory holds the architectural documentation for Ballooner.

## Architecture Decision Records

Architecture Decision Records (ADRs) live in [decisions/](decisions/) and capture choices with
lasting impact on system structure, dependencies, data ownership, deployment, security,
performance, platform support, or cross-feature conventions.

Records are never deleted or renumbered. When a decision stops governing the project, a new ADR
is created and the old one is marked `Disabled` with a link between the two.

See [.github/instructions/architecture-decisions.instructions.md](../../.github/instructions/architecture-decisions.instructions.md)
for the required format and status rules.

### Index

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [ADR-0001](decisions/0001-view-rotation-is-a-display-only-layer-transform.md) | View rotation is a display-only layer transform | Disabled | 2026-09-25 |
| [ADR-0002](decisions/0002-panel-rotation-is-a-destructive-undoable-image-edit.md) | Panel rotation is a destructive, undoable edit to the merged comic image | Active | 2026-09-25 |
