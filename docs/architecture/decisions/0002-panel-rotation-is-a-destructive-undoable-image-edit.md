# ADR-0002: Panel rotation is a destructive, undoable edit to the merged comic image

- Status: Active
- Date: 2026-09-25
- Decision makers: Ballooner maintainers
- Corrected by: [ADR-0003](0003-a-panel-quarter-turn-is-anchored-at-the-panel-top-left-corner.md) — a quarter
  turn is anchored at the panel's top-left corner, not about its centre. Every other decision below
  still governs.

## Context

A Ballooner comic is stored as one merged image plus a list of `RectFraction` panel rects that carve
that image into panels. The Panel rotate handle previously turned the whole content layer with a
`graphicsLayer { rotationZ }`, as recorded in [ADR-0001](0001-view-rotation-is-a-display-only-layer-transform.md).

That made rotation unusable in the app's main case. Because the turn applied to the entire content
layer, every panel turned together. The only way to make it look like a single panel had turned was
to hide the other panels, which meant entering focus mode. In a multi-panel comic the user could
never see a rotated panel sitting among its neighbours, and nothing they did to the view survived
leaving the screen.

The requirement is the opposite of a viewing aid: the user wants the panel itself to turn, in place,
on the Canvas, with the neighbouring panels moving out of the way to accommodate its new shape, and
with the result kept.

Ballooner already has the machinery for exactly this kind of change. Panel moves, resizes and crops
all work by rewriting the merged image and replacing the panel rects; the rearrange pipeline
(`ImageStore.rearrangePanels` → `computeRearrangeLayout` → `repositionPanelsAfterResize`) reflows
neighbours and rebounds the canvas, and image edits are covered by a single-step undo snapshot in
`ProjectViewModel`. Rotation had been the one panel operation that stood outside all of it.

## Decision

Rotating a panel is a destructive, undoable edit to the merged comic image, not a view transform.

1. **The pixels turn.** A rotate turns that panel's pixels by a quarter turn inside the project's
   merged image.
2. **The rect turns with them.** The panel's stored width and height are swapped about the panel's
   centre, so the stored rect already describes the panel's new shape.
3. **Neighbours reflow through the existing pipeline.** The swapped rect is fed through the same
   rearrange pipeline used by move and resize, which repositions neighbouring panels and rebounds
   the canvas around the result.
4. **One undo step.** The edit is captured by the existing single-step image-edit undo snapshot,
   shared with crop and resize.
5. **No rotation state exists anywhere.** There is no display-only rotation layer and no persisted
   rotation angle. Once the edit commits, both the stored image and the stored panel rects are
   already in their final orientation.

The property ADR-0001 was protecting therefore survives: geometry still lives in exactly one
unrotated, axis-aligned frame of reference. Layout, hit-testing, handle placement, crop, export and
the comic thumbnail continue to need no knowledge of rotation.

Supporting decisions:

- **The display-only view rotation is removed entirely**, along with its handle-remapping machinery.
  Keeping both would leave two visually indistinguishable rotations behind one handle. This
  knowingly gives up the "inspect a portrait photo sideways" affordance that ADR-0001 justified. If
  that affordance is wanted later it must return as a separately named control, not as the Panel
  rotate handle.
- **No Room schema change, no migration, no database version bump.** Rotation is expressed entirely
  in the merged image and the existing panel rects.
- **The quality objection in ADR-0001 does not apply here.** ADR-0001 partly rejected pixel rotation
  because rotating pixels loses quality. An exact quarter turn into a swapped-dimension rect
  involves no resampling, and the result is stored as lossless PNG. The stored image is already
  capped at `MAX_DECODED_DIMENSION_PX` (2048px) on decode, so there is no repeated downsampling.
- **Balloons orbit, but stay upright.** A balloon on a rotated panel orbits the panel centre by the
  quarter turn and its tail angle advances 90 degrees, but the balloon body keeps its width, height
  and upright text. Rotating the body would make the text unreadable and overflow its shape.

### Out of scope

- Arbitrary, non-90-degree rotation angles.
- Animating the turn between quarter turns.
- Closing the gap left when a panel shrinks, or re-packing the layout.
- Multi-step undo.
- Rotating balloon bodies or balloon text.

## Alternatives Considered

- **Keep the display-only view rotation of [ADR-0001](0001-view-rotation-is-a-display-only-layer-transform.md).**
  It turns the whole content layer, so a single panel can only appear rotated with the others
  hidden in focus mode. That does not meet the requirement of a panel turning in place among its
  neighbours, and the result is never kept.
- **Persist a rotation angle on the panel entity and render it at draw time.** Rejected: it requires
  a schema migration and database version bump; it forces a new rotation-aware domain type to
  replace the shared `RectFraction` everywhere it is used across the repository and the Canvas; it
  requires per-panel rotated blits in both the draw path and the exporter; and it still leaves the
  comic list thumbnail permanently unrotated, because the thumbnail renders the stored merged image
  and knows nothing of panel metadata.
- **Rotate the panel but leave neighbours where they are.** A quarter turn swaps the panel's width
  and height, so a rotated panel overlaps or is overlapped by its neighbours. Reusing the existing
  rearrange pipeline gets the reflow and the canvas rebound for free.
- **Rotate the balloon bodies along with the panel.** Turning a balloon body sideways makes its text
  unreadable and overflows the shape, which is a strictly worse result than an upright balloon in a
  rotated position.

## Consequences

### Positive

- A panel can be rotated in place on the Canvas, with neighbours accommodating it, and the result is
  kept.
- Geometry keeps the single unrotated, axis-aligned frame of reference that ADR-0001 established, so
  layout, hit-testing, handle placement, crop, export and the comic thumbnail stay rotation-unaware.
- The rotated result is what the user sees everywhere: editor, export and comic list thumbnail all
  render the same merged image.
- No migration, no database version bump, and no new domain geometry type.
- Rotation reuses the rearrange pipeline and the image-edit undo snapshot rather than adding a
  parallel mechanism, and the handle-remapping machinery that existed only to serve view rotation is
  deleted.
- A quarter turn into a swapped-dimension rect is lossless: no resampling, stored as PNG.

### Negative

- Rotation is no longer free or reversible-by-leaving. It writes a new PNG and is undoable exactly
  once. Rotating the only panel in a comic permanently changes the saved image, the export and the
  comic thumbnail.
- The neighbour reflow only pushes panels apart to make room for growth; it never pulls them back in.
  A tall-to-wide rotation therefore leaves a white gap where the panel shrank. This is accepted for
  now, and closing it is explicitly out of scope, because negative-growth reflow can pull panels into
  collisions.
- Panels are identified by value equality on their rect, so a rotation changes the rect and the
  selection drops. This is consistent with how resize already behaves.
- Repeated rotations rebound the canvas each time and can grow the merged image toward the 2048px
  decode cap, progressively shrinking the comic on screen.
- The "inspect a portrait photo sideways" affordance from ADR-0001 is gone.

## References

- [ADR-0001](0001-view-rotation-is-a-display-only-layer-transform.md) — superseded by this record.
- [`ui-vocabulary.instructions.md`](../../../.github/instructions/ui-vocabulary.instructions.md) —
  canonical names for the Panel rotate handle and the other Canvas controls.
- `app/src/main/java/com/ballooner/data/image/ImageStore.kt` and `AppImageStore.kt` — the merged
  image operations, `rearrangePanels`, and `MAX_DECODED_DIMENSION_PX`.
- `app/src/main/java/com/ballooner/data/image/RearrangeLayout.kt` — `computeRearrangeLayout`, the
  reflow and canvas rebound.
- `app/src/main/java/com/ballooner/domain/model/ImagePlacement.kt` — `quarterTurnedPanel`, the
  swapped-dimension turn destination, and `repositionPanelsAfterResize`, the growth-only
  neighbour reflow.
- `app/src/main/java/com/ballooner/domain/model/Balloon.kt` — `remappedByQuarterTurns`, the
  balloon orbit and tail-angle advance.
- `app/src/main/java/com/ballooner/ui/project/ProjectViewModel.kt` — the single-step
  `ImageEditSnapshot` undo shared by rotate, crop and resize.
