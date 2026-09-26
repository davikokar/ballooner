# ADR-0006: Balloon scope and the coordinate spaces for balloons and panel images

- Status: Active
- Date: 2026-09-26
- Decision makers: Ballooner maintainers

## Context

Balloons are currently stored in coordinates relative to the whole merged comic image, and every
panel edit remaps them with `Balloon.remappedBetween` or `Balloon.remappedByQuarterTurns`. Those
helpers rescale a balloon's width and height by the panel's width and height ratios
independently, so a balloon stretches whenever a panel's aspect ratio changes. The same edit also
has to decide which balloons belong to the edited panel by testing whether their centre falls
inside it.

Separately, users need both balloons that belong to a panel and balloons that sit across the
page, which the single coordinate space cannot express.

Panel images have a related problem: a zoom factor stored as an absolute scale is only valid for
the frame it was measured in, so it has to be re-clamped whenever the frame changes.

## Decision

### Balloon scope

Every balloon has a scope, which the user can toggle on a selected balloon:

1. **Panel balloon** (default) — belongs to one panel. It is clipped to that panel, so overflow
   including its tail is not drawn. It follows its panel and is deleted with it.
2. **Comic balloon** — belongs to the page. It draws above every panel, is clipped only by the
   page, and is unaffected by layout changes.

Toggling scope preserves the balloon's on-screen position; only the space it is measured in
changes. Balloons draw in the order they were added, and comic balloons always draw above panel
balloons.

### Coordinate spaces

| Value | Space |
|-------|-------|
| Panel balloon position | panel-local `(u, v)` in `[0, 1]`, measured in the panel's bounding box |
| Comic balloon position | page-local `(u, v)` in `[0, 1]` |
| Balloon size, tail length, text size | fraction of **page width** |
| Panel image centre | image-local `(u, v)` |
| Panel image zoom | multiple of `coverScale(frame, imageAspect, angle)` |

Two rules follow and are binding:

- **Sizes use one uniform unit for both axes.** A balloon therefore never stretches when a
  panel's aspect ratio changes, and reshaping a panel never resizes its balloons. A balloon that
  no longer fits its panel is clipped, and the user adjusts it if they want to.
- **Zoom is relative, not absolute.** `zoom >= 1` *is* the invariant that a panel image always
  covers its panel. It holds in any frame, so a layout change requires no re-fitting and rotation
  needs no special handling — `coverScale` already accounts for the angle.

## Alternatives Considered

- **Keep one page-wide coordinate space for all balloons.** Rejected: it cannot express "this
  balloon belongs to this panel", so every panel edit must infer ownership geometrically and
  remap, which is the current source of stretched balloons.
- **Rescale balloon sizes with their panel.** Rejected: non-uniform rescaling distorts balloon
  shapes and text, and the user asked that panel resizing leave balloons alone.
- **Store panel image zoom as an absolute scale.** Rejected: it is only meaningful relative to a
  specific frame, so every layout change would need a re-clamping pass that must not be forgotten
  anywhere.
- **Parent balloons to panels by a stable panel id.** Rejected for now: panels are derived and
  have no identity across layout changes (ADR-0005), so ownership is expressed by index in
  reading order.

## Consequences

### Positive

- Moving, resizing, reshaping, or rotating a panel costs no balloon arithmetic at all. Both
  `remapped*` helpers are deleted.
- Comic balloons can cross gutters and panel boundaries, a standard comic idiom the old model
  could not express.
- The covering invariant is enforced by the unit rather than by a clamp that must be reapplied at
  every call site.

### Negative

- Two coordinate spaces exist for balloons, and converting between them on a scope toggle is a
  conversion that must be correct or the balloon visibly jumps.
- Panel-local coordinates are bounding-box fractions, so for an angled panel some positions fall
  outside the visible shape and the balloon is clipped.
- `coverScale` is new geometry that must handle arbitrary rotation angles, and it is on the hot
  path for every placement gesture.
- Panel balloons are tied to panel index, so any change to reading order moves balloon ownership.

## References

- [Comic creation workflow](../../design/comic-creation-workflow.md)
- [ADR-0004](0004-a-comic-is-a-declarative-document-rendered-on-demand.md),
  [ADR-0005](0005-panel-shapes-are-derived-from-the-layout.md)
