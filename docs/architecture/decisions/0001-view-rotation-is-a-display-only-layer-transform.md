# ADR-0001: View rotation is a display-only layer transform

- Status: Active
- Date: 2026-09-25
- Decision makers: Ballooner maintainers

## Context

The Comic editor screen lets the user turn the view by quarter turns with the Panel rotate handle.
The turn exists so a portrait photo can be inspected sideways, or a wide panel read comfortably on a
tall phone; it is a viewing aid, not an edit to the comic.

That single affordance touches a surprising amount of the Canvas:

- Panel geometry is stored as `RectFraction` values in image space, and every layout, hit-test,
  magnetic-snap, crop and export calculation is written in that space.
- Panel handles and balloon handles are Composables that sit inside the rotated content, so the
  transform changes where they land, which way their glyphs point, and what a drag on them means.
- The content frame is sized to fit the viewport, and handle badges deliberately overhang the panel
  edge, so any clipping interacts with the turn.

The convention covering all of this was arrived at incrementally, one handle at a time, and several
of its rules are non-obvious enough that they were re-derived (and re-broken) more than once. It now
governs every panel handle, so it needs a dated record rather than a comment in one file.

## Decision

View rotation is a display-only transform on the presentation layer. Specifically:

1. **Geometry stays unrotated.** Panel and balloon geometry is always stored and computed in
   unrotated panel space. Rotation is ephemeral view state, is never persisted, and is never applied
   to the exported image.
2. **One transform, applied twice.** Rotation is applied solely as `graphicsLayer { rotationZ }` on
   the content layer and on the balloon-handle layer. Nothing else rotates.
3. **Drag deltas are never rotated by hand.** Jetpack Compose delivers pointer deltas to nodes
   beneath a rotated `graphicsLayer` already mapped into that layer's unrotated local space. Gesture
   math therefore consumes deltas as-is; rotating them again double-applies the turn.
4. **Handle nodes are never counter-rotated — only their glyphs.** A glyph is kept upright with
   `Modifier.uprightIn(rotationDegrees)`, which counter-rotates the icon alone. Counter-rotating the
   handle node would change the node's local pointer space, which is exactly the space the gesture
   math in rule 3 depends on.
5. **Screen-anchored handles remap their semantics per quarter turn.** A handle pinned to a fixed
   *screen* anchor, so it holds its corner through a turn, must resolve which panel-space corner or
   edges it actually manipulates for the current quarter turn. `HandleAnchor` plus `quarterTurns` and
   `panelHandleCenter` map a screen anchor back into layer space for positioning; the gesture
   the handle performs must be remapped the same way. Handles anchored to the layer instead — the
   balloon handles, which travel with the content — need no remapping.
6. **The content frame is clipped only in focus mode.** The unfocused rotated layer fits its frame
   exactly, so clipping it while rotated amputates handle overhang. The failure is draw-only: the
   badges stay tappable while invisible, which makes it easy to misdiagnose.

## Alternatives Considered

- **Persist the rotation on the panel and bake it into the export.** This would make rotation a real
  edit, requiring a schema change with a migration, a rotation-aware exporter, and rotation-aware
  layout, snapping and crop math throughout. The user need is inspection, not a rotated comic, so the
  cost is not justified.
- **Rotate each panel's bitmap instead of the view layer.** Rotating pixels loses quality, costs a
  re-composite per turn, and still leaves handle placement and gesture mapping to solve.
- **Counter-rotate the handle nodes so glyphs stay upright.** Simpler to write, but it re-orients the
  node's pointer space, so pointer deltas arrive in a different frame than the gesture math expects.
  Counter-rotating the glyph alone gets the same visual result without touching pointer space.
- **Convert drag deltas into panel space by hand.** Redundant given Compose's pointer mapping, and
  actively wrong: a quarter turn then makes a rightward drag move the panel downward.
- **Anchor panel handles to the content layer, like balloon handles.** Then handles travel with the
  content and swap screen corners on each turn — the rotate handle walks around the panel, and the
  delete badge can land under the user's grip. Screen anchoring keeps the controls where the hand
  expects them, at the cost of the per-turn remapping in decision 5.
- **Always clip the content frame.** Uniform and simpler to reason about, but it removes the handle
  overhang that makes badges reachable at panel edges.

## Consequences

### Positive

- Storage, export and all geometry math have a single, rotation-free frame of reference; nothing
  downstream of the Canvas needs to know rotation exists.
- Rotation costs nothing to apply or undo: it is a layer property, so turning the view never touches
  the database or the merged bitmap, and abandoning the editor discards it.
- Gesture code stays written in panel space, and pointer handling has one rule — trust Compose's
  deltas — instead of a per-handle correction.
- Panel controls keep their screen positions through a turn, so muscle memory survives rotation.
- The rules are testable without a device: `quarterTurns`, `panelHandleCenter` and `focusLayout` are
  pure functions covered by unit tests.

### Negative

- Adding a new panel handle is not free. A contributor must: place it with `panelHandleCenter` and a
  `HandleAnchor` rather than raw panel coordinates; keep its glyph upright with `Modifier.uprightIn`
  while leaving the node itself unrotated; consume drag deltas unrotated; and, if the handle is
  screen-anchored, resolve per quarter turn which panel-space edges or corner the gesture actually
  moves. Missing any one of these produces a handle that looks right but behaves wrongly only while
  rotated.
- A resized panel still grows from its stored top-left corner, so the screen corner under the finger
  is not pinned once the view is turned. This is an accepted limitation, not a bug to be fixed by
  rotating deltas.
- The unfocused content frame is deliberately unclipped, so handle overhang can extend past the
  nominal comic bounds.
- Rotation resets rather than restores: because it is not persisted, leaving the Comic editor screen
  or changing the focused panel returns the view upright.
- Only exact quarter turns are supported. Intermediate angles never render today, and the placement
  and remapping rules assume that.

This convention would need revisiting if rotation ever became persisted, applied to the export, or
animated between quarter turns. Any of those changes should be recorded as a new ADR that supersedes
this one rather than an edit to this record.

## References

- [`ui-vocabulary.instructions.md`](../../../.github/instructions/ui-vocabulary.instructions.md) —
  canonical names for the Panel rotate handle and the other Canvas controls.
- `app/src/main/java/com/ballooner/ui/project/ProjectScreen.kt` — rotation state, the content and
  balloon-handle `graphicsLayer` transforms, `HandleAnchor`, `quarterTurns`, `panelHandleCenter`,
  `cropHandleCenter`, `focusLayout` and `Modifier.uprightIn`.
- `app/src/test/java/com/ballooner/ui/project/BalloonDrawingTest.kt` — unit tests pinning the
  quarter-turn normalisation and handle placement rules.
