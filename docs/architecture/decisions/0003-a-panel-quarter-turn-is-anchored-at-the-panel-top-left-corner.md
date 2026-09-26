# ADR-0003: A panel quarter turn is anchored at the panel's top-left corner

- Status: Active
- Date: 2026-09-26
- Decision makers: Ballooner maintainers
- Corrects: ADR-0002 (the "about the panel's centre" clause of its Decision 2)

## Context

[ADR-0002](0002-panel-rotation-is-a-destructive-undoable-image-edit.md) decided that a quarter turn
swaps the panel's stored width and height **about the panel's centre**. The implementation expressed
that on a `RectFraction` as `left += (width - height) / 2` and `top -= (width - height) / 2`.

That arithmetic is unsound, and the reason generalises well beyond rotation:

> **`RectFraction.width` is a fraction of the canvas width and `RectFraction.height` is a fraction of
> the canvas height.** The two are in different units and must never be added to or subtracted from
> one another. The same holds for `left` against `top`. Any cross-axis arithmetic on a
> `RectFraction` is wrong unless the canvas happens to be square.

The observable damage was not subtle:

- The resulting pixel offset is wrong by a term proportional to the canvas aspect ratio. The canvas
  aspect ratio itself changes on every rotation, because the rearrange pipeline rebounds the canvas
  around the new layout. Panels therefore appeared to jump around the canvas with no apparent logic,
  and the jump differed from one turn to the next.
- The bogus offset was typically negative. A negative coordinate triggers the origin shift in
  `computeRearrangeLayout` (`minLeft`/`minTop` coerced at most to `0`, then subtracted from every
  rect). That shift translated *untouched neighbouring panels* sideways and inflated the canvas,
  which in turn made the whole comic fit-scale smaller on screen. A rotation of one panel visibly
  disturbed panels the user never touched.

The float-drift repair already in `quarterTurnedPanel` — snapping the turned `left`/`top` to the
`COORDINATE_PRECISION` grid so that four turns restore the original rect — exists only to patch the
symptom of that same formula. It does nothing about the unit error.

## Decision

A quarter turn is anchored at the panel's top-left corner.

1. **`left` and `top` are carried through unchanged; only `width` and `height` are exchanged.** The
   panel's top-left corner stays fixed in the comic's coordinate space, and the panel extends only
   rightwards and downwards into its new shape.
2. **No cross-axis arithmetic occurs.** Because `left` and `top` no longer derive from `width` and
   `height`, the turn never mixes a horizontal fraction with a vertical one.
3. **The turn is exact under repetition by construction.** Four quarter turns restore the original
   rect because nothing is computed — the fields are copied and the pair is swapped. No
   precision-grid snapping or other float-drift repair is needed.
4. **Coordinates can never go negative.** A turn starting from a non-negative rect produces a
   non-negative rect, so the origin shift in `computeRearrangeLayout` is a guaranteed no-op for a
   rotation, and neighbouring panels are moved only by the reflow that genuinely needs to move them.
5. **The general rule for all future geometry work:** never add or subtract a `RectFraction`'s
   `width` against its `height`, or its `left` against its `top`. If a cross-axis relationship is
   genuinely needed, convert to pixels with the canvas dimensions first, do the arithmetic there,
   and convert back.

Every other decision in ADR-0002 remains correct and governing: rotation is still a destructive,
undoable edit to the merged image; it still reuses the rearrange pipeline and the single image-edit
undo slot; there is still no rotation state and no persisted angle; and balloons still orbit while
staying upright. Only the centre-anchor clause is corrected here.

### Out of scope

- Everything ADR-0002 placed out of scope, unchanged: arbitrary angles, animation, closing the
  shrink gap, multi-step undo, rotating balloon bodies.
- Auditing the rest of the codebase for other cross-axis `RectFraction` arithmetic. The rule above
  governs new and changed code; a sweep is a separate piece of work.

## Alternatives Considered

- **Keep the centre anchor, but do the arithmetic in pixels.** Convert the rect to pixels with the
  canvas dimensions, swap about the pixel centre, convert back. Rejected: `quarterTurnedPanel` lives
  in `domain/`, which has no access to canvas dimensions and must not acquire an Android or
  image-store dependency to get them. It also still produces coordinates that can go negative, so
  the origin shift keeps disturbing untouched neighbours, and the centre it preserves is a centre in
  a canvas that the rebound is about to resize anyway.
- **Keep the fraction-space centre formula and only harden the float drift.** This is the status quo
  — the `COORDINATE_PRECISION` snap. Rejected: it repairs the symptom (a 1-ulp drift over four
  turns) and leaves the cause (an aspect-ratio-sized positional error on every single turn) intact.
- **Apply the centre anchor only when the canvas is square.** Rejected: a special case that is
  almost never taken, cannot be exercised by ordinary use, and leaves the unsound formula in the
  codebase as a model for the next contributor to copy.
- **Introduce an aspect-ratio-aware geometry type so cross-axis arithmetic becomes well defined.**
  Rejected for the same reason ADR-0002 rejected a rotation-aware geometry type: `RectFraction` is
  shared across the repository, the Canvas, crop and export, and replacing it is a large change to
  buy a property that a one-line rule already delivers.
- **Anchor at the panel's bottom-right or at a corner chosen per turn direction.** Rejected: any of
  these are equally free of cross-axis arithmetic, but top-left is the only one that cannot produce
  negative coordinates, and it matches how panel resize already grows a panel from its stored
  top-left.

## Consequences

### Positive

- A rotation moves only the rotated panel and whichever neighbours the reflow genuinely displaces.
  Untouched panels stay put, the canvas is not spuriously inflated, and the comic's fit-scale on
  screen no longer shrinks for no visible reason.
- Four quarter turns return the panel to its original rect exactly, with no precision-grid snapping
  or other drift-repair machinery to maintain or explain.
- The origin shift in `computeRearrangeLayout` is a guaranteed no-op for a rotation, so that branch
  needs no reasoning about rotation at all.
- The cross-axis rule is now written down once and governs every future contributor touching panel
  geometry, rather than being re-derived and re-broken per feature.

### Negative

- The growth-only reflow gap that ADR-0002 accepted still exists. It has only changed shape: it now
  sits wholly below (or to the right of) the turned panel instead of being split around the panel's
  former centre. Same limitation, different shape.
- The panel's position is pinned in the comic's own coordinate space, not on screen. The merged
  image is fit-scaled into the viewport, so when a turn grows the canvas the whole comic scales down
  and the turned panel's on-screen position still drifts. Stability is a property of the stored
  geometry, not of what the user's eye tracks.
- A tall panel turning wide now always extends rightwards and downwards rather than expanding evenly
  about its centre, so the turn looks asymmetric. This is the cost of an anchor that cannot go
  negative.
- `AppImageStore.rearrangePanels` derives the turned panel's pixel size from the rotated bitmap's own
  dimensions, not from the destination rect's `width`/`height`, which is what guarantees a
  resample-free blit. For a rotation only the destination's `left` and `top` are consumed. The
  destination rect's fractions and the blitted size can therefore disagree by a rounding step; this
  is deliberate and must not be "fixed".

## References

- [ADR-0002](0002-panel-rotation-is-a-destructive-undoable-image-edit.md) — the record this one
  corrects. Only its centre-anchor clause is superseded; the rest still governs.
- [ADR-0001](0001-view-rotation-is-a-display-only-layer-transform.md) — the disabled display-only
  rotation, for background.
- `app/src/main/java/com/ballooner/domain/model/RectFraction.kt` — the type the cross-axis rule
  governs.
- `app/src/main/java/com/ballooner/domain/model/ImagePlacement.kt` — `quarterTurnedPanel`, the turn
  destination, and `repositionPanelsAfterResize`, the growth-only neighbour reflow.
- `app/src/main/java/com/ballooner/data/image/RearrangeLayout.kt` — `computeRearrangeLayout` and the
  origin shift that a negative coordinate used to trigger.
- `app/src/main/java/com/ballooner/data/image/AppImageStore.kt` — `rearrangePanels`, which sizes a
  turned panel from the rotated bitmap so the blit resamples nothing.
