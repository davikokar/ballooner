# ADR-0005: Panel shapes are derived from the layout, never stored

- Status: Active
- Date: 2026-09-26
- Decision makers: Ballooner maintainers

## Context

Panel rectangles are currently stored as `RectFraction` values in the `panels` table, and the
comic image is composited to match them. Any change to one panel therefore has to be propagated
by hand to every other stored rect: `repositionPanelsAfterResize`, `computeRearrangeLayout`, the
magnetic snapping destinations, and the origin-shift repair all exist to keep stored geometry
consistent after an edit.

That propagation is the source of the bugs recorded in ADR-0002 and ADR-0003, including the
unsound `left += (width - height) / 2` arithmetic that mixed canvas-width and canvas-height units.

[ADR-0004](0004-a-comic-is-a-declarative-document-rendered-on-demand.md) makes the comic a
document, which allows geometry to be computed instead of stored.

## Decision

**No panel geometry is persisted. Panel shapes are computed by a pure function.**

```
panelShapes(layout, pageShape, style): List<Polygon>
```

1. The function returns one convex polygon per panel, in reading order.
2. Panels are produced in two stages: the grid's spans become rectangles, then the layout's cuts
   split them in the order the cuts were drawn. See
   [ADR-0008](0008-a-layout-is-a-grid-plus-ordered-cuts.md).
3. Gutters are produced by insetting during the split, not by offsetting a finished polygon: each
   piece of a split is inset by half the gutter from the cut line, and each grid rectangle is
   inset by half the gutter on every side it shares with a neighbour.
4. Panels stay convex under any number of straight cuts, because clipping a convex polygon
   against a half-plane yields convex pieces. This holds at any angle.
5. Reading order is defined by panel centroid, top to bottom then left to right, with a tolerance
   band that treats nearby centroids as one row.
6. `panelShapes` and its helpers live in `domain/` with no Android dependencies.

## Alternatives Considered

- **Store panel rects and keep them in sync.** Rejected: this is the current design, and keeping
  N stored rectangles mutually consistent after every edit is precisely what has failed.
- **Store derived shapes as a cache alongside the layout.** Rejected. It reintroduces two sources
  of truth for no measured performance need; the computation is a few polygon clips per frame.
- **Represent custom layouts as a split tree rather than an ordered cut list.** Rejected: a tree
  makes each cut a child of a region, so moving or deleting a cut cascades through its
  descendants. Position-anchored cuts avoid the dependency entirely (ADR-0008).
- **General polygon offsetting for gutters.** Rejected as unnecessary: insetting at split time is
  exact for straight cuts and avoids an entire class of self-intersection edge cases.

## Consequences

### Positive

- Changing the gutter, margin, page shape, or a grid proportion is a one-field edit that cannot
  desynchronise anything, because there is nothing stored to desynchronise.
- Layout changes cannot corrupt panel geometry; geometry is recomputed from scratch every time.
- Diagonal cuts cost nothing extra: half-plane clipping is angle-agnostic.
- The panel reflow, magnetic snapping, and rearrange code is deleted.

### Negative

- Panel shapes must be recomputed whenever the layout or style changes, and hit testing works
  against polygons rather than rectangles.
- Non-rectangular panels force approximations elsewhere: `coverScale` uses the panel's bounding
  box, so a triangular panel's image zooms more than strictly necessary, and panel-local balloon
  coordinates are bounding-box fractions, so some positions fall outside the visible panel.
- Reading order for diagonally cut pages depends on a tolerance band, which is a tunable value
  rather than an exact rule and needs its own tests.
- Panels have no stable identity of their own across layout changes; they are matched by index in
  reading order.

## References

- [Comic creation workflow](../../design/comic-creation-workflow.md)
- [ADR-0004](0004-a-comic-is-a-declarative-document-rendered-on-demand.md),
  [ADR-0008](0008-a-layout-is-a-grid-plus-ordered-cuts.md)
- [ADR-0003](0003-a-panel-quarter-turn-is-anchored-at-the-panel-top-left-corner.md), whose unit
  mixing bug is structurally impossible once geometry is derived
