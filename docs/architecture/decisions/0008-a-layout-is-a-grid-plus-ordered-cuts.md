# ADR-0008: A layout is a grid with merged spans plus an ordered list of cuts

- Status: Active
- Date: 2026-09-26
- Decision makers: Ballooner maintainers

## Context

Panels are currently created one at a time by compositing an image beside an existing panel, so
the layout is a consequence of the images rather than a choice. The user needs to design the page
first, and needs layouts that a plain grid cannot express — in particular panels separated by
diagonals, which is a defining comic idiom.

Two candidate models were considered for freely drawn layouts. Both must produce panels for
[ADR-0005](0005-panel-shapes-are-derived-from-the-layout.md) to derive shapes from, and both must
survive the user going back and changing the layout after images and balloons exist.

A grid restricted to horizontal and vertical dividers, and a freehand layout restricted to
horizontal and vertical lines, express exactly the same set of pages. Keeping them as two
separate layout kinds would be two ways to say the same thing.

## Decision

**A layout is one grid plus an ordered list of cuts.**

```
Layout
    grid: Grid(rows, columns, rowWeights, columnWeights, spans)
    cuts: List<Cut>

Span(firstRow, firstColumn, rowCount, columnCount)
Cut(a, b, scope)      // the line through page-normalized points a and b
                      // scope: WholePage | AtPoint(anchor)
```

1. Every preset is a position in this one model. A single panel is a 1×1 grid, a strip is 1×n, a
   blank page for freehand work is a 1×1 grid with cuts.
2. A **span** is one panel covering a rectangular block of cells. Merging requires the selection
   to form a rectangle; L-shaped merges are refused. Unmerging restores the covered cells.
3. A **cut** is a straight line at any angle. A **page cut** splits every panel it crosses. A
   **panel cut** splits only the panel that currently contains its anchor point.
4. **Cuts are targeted by position, not by index.** A panel cut carries the point at which the
   user began the trace, and at evaluation it splits whichever panel contains that point. Cuts
   therefore never depend on one another: deleting or moving an earlier cut reshapes panels, and
   a later cut's anchor simply lands in whatever panel now occupies that place.
5. If an anchor falls in a gutter or off the page, its cut is skipped and the Layout step marks
   it so the user can move or delete it. This is a policy, not a geometric necessity: it is
   implemented as a single function mapping an anchor to a target panel, so it can be changed —
   to nearest-panel, for example — without touching the surrounding code.
6. Cuts are the only source of non-rectangular panels. A grid alone always yields rectangles.

## Alternatives Considered

- **Grid layouts and freehand layouts as two separate kinds.** Rejected: horizontal and vertical
  cuts duplicate what grid lines already do, so the same page would have two representations.
- **Cuts that always cross the whole page.** Rejected: full-page lines are all-or-nothing for
  every row they pass through, so a page with two panels above and three below is unreachable,
  and a diagonal intended for one panel slashes the entire page.
- **A recursive split tree, each node dividing its region.** Rejected: it makes every cut a child
  of a region, so moving or deleting a cut cascades to its descendants and needs re-parenting
  rules. Position-anchored cuts get the same expressiveness with no dependency structure.
- **Panel cuts stored as endpoints in their target panel's coordinates.** Rejected: it requires
  the target to be identified by index, which is exactly the dependency that deleting an earlier
  cut would break.
- **Curved cuts.** Deferred, not rejected. Straight cuts keep every panel convex, which is what
  makes splitting, gutter insetting, and covering a panel with a rotated image simple and exact.
  A curve breaks all three at once, and finding the regions an arrangement of curves carves out
  is a substantially harder problem than clipping against a line.

## Consequences

### Positive

- One layout model to implement, persist, and test, while the user still sees familiar presets.
- Diagonals are available inside grid layouts, not only in a separate freehand mode.
- Deleting a cut can never orphan another cut, so there is no cascade and no re-parenting code.
- The layout is a small, immutable value, which suits document snapshot undo.

### Negative

- Anchors are resolved against derived geometry, so an anchor can land somewhere the user did not
  intend after an unrelated edit, and a cut can silently become inactive.
- Cut order still matters for the resulting shapes, so the list is not a set and reordering is
  not free.
- Merging is restricted to rectangular selections, so L-shaped and other non-rectangular merges
  are unavailable.
- Curved cuts, which the user asked about, are not available in the first version.

## References

- [Comic creation workflow](../../design/comic-creation-workflow.md)
- [ADR-0005](0005-panel-shapes-are-derived-from-the-layout.md),
  [ADR-0004](0004-a-comic-is-a-declarative-document-rendered-on-demand.md)
