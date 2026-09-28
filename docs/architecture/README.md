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
| [ADR-0002](decisions/0002-panel-rotation-is-a-destructive-undoable-image-edit.md) | Panel rotation is a destructive, undoable edit to the merged comic image | Disabled | 2026-09-25 |
| [ADR-0003](decisions/0003-a-panel-quarter-turn-is-anchored-at-the-panel-top-left-corner.md) | A panel quarter turn is anchored at the panel's top-left corner | Disabled | 2026-09-26 |
| [ADR-0004](decisions/0004-a-comic-is-a-declarative-document-rendered-on-demand.md) | A comic is a declarative document rendered on demand | Active | 2026-09-26 |
| [ADR-0005](decisions/0005-panel-shapes-are-derived-from-the-layout.md) | Panel shapes are derived from the layout, never stored | Active | 2026-09-26 |
| [ADR-0006](decisions/0006-balloon-scope-and-coordinate-spaces.md) | Balloon scope and the coordinate spaces for balloons and panel images | Active | 2026-09-26 |
| [ADR-0007](decisions/0007-the-comic-editor-is-a-three-step-workflow.md) | The comic editor is a three-step workflow over one document | Active | 2026-09-26 |
| [ADR-0008](decisions/0008-a-layout-is-a-grid-plus-ordered-cuts.md) | A layout is a grid with merged spans plus an ordered list of cuts | Active | 2026-09-26 |
| [ADR-0009](decisions/0009-the-page-takes-its-height-from-a-reference-panel.md) | The page takes its height from a reference panel | Active | 2026-09-28 |
