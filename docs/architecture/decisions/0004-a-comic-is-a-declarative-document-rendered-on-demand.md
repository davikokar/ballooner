# ADR-0004: A comic is a declarative document rendered on demand

- Status: Active
- Date: 2026-09-26
- Decision makers: Ballooner maintainers
- Supersedes: [ADR-0002](0002-panel-rotation-is-a-destructive-undoable-image-edit.md),
  [ADR-0003](0003-a-panel-quarter-turn-is-anchored-at-the-panel-top-left-corner.md)

## Context

A Ballooner comic is stored as one merged bitmap plus a list of `RectFraction` panel rects that
carve that bitmap into panels, with balloons expressed in the same fractional space. Every
structural edit — add, move, resize, crop, or rotate a panel — re-renders the whole page through
`ImageStore`, recomputes every panel rect, and remaps every balloon.

Three costs follow from that, and all three have been paid repeatedly:

1. **The coordinate space moves under every edit.** When the merged bitmap grows, all panel
   fractions change even though nothing visually moved, so "this panel was untouched" cannot be
   expressed.
2. **Edits are destructive.** Undo needs a whole-bitmap snapshot, which is why only one undo slot
   exists, and repeated crops or rotations resample pixels.
3. **Simple intentions have large blast radii.** Rotating a panel changes its aspect ratio, which
   changes the page, which forces neighbouring panels to reflow, which forces every balloon to be
   remapped — plus the pixel rotation itself.

ADR-0001, ADR-0002, and ADR-0003 are three successive attempts to tame that third cost. Each
addressed a symptom of the same cause: the document is a bitmap.

The redesign described in
[the comic creation workflow](../../design/comic-creation-workflow.md) removes the cause.

## Decision

**A comic is a declarative document. Pixels are produced only for display and export.**

1. The document records the page shape, the comic style, the layout, which image is in which
   panel, how each image is transformed inside its panel, and the balloons. It never records
   composited pixels.
2. Imported images are copied into app storage once and are never modified afterwards.
3. Rendering is a pure projection of the document. The Compose preview and the PNG export run the
   same layout and transform functions at different resolutions.
4. `ImageStore` is reduced to two responsibilities: import a source image, and export a rendered
   comic.
5. Rotating a panel image is a change to one field, at any angle. It never changes the panel's
   shape, the page, neighbouring panels, or any balloon.
6. Undo is a stack of document snapshots rather than a single bitmap snapshot.

## Alternatives Considered

- **Keep the merged bitmap and fix rotation again.** Rejected. ADR-0001 through ADR-0003 show the
  pattern: each fix is correct in isolation and the next edit type reopens the same class of bug,
  because the coupling between panel geometry, page geometry, and balloon geometry is inherent to
  storing the comic as pixels.
- **Keep the merged bitmap but store panel rects in absolute pixels.** Rejected. It removes the
  "fractions shift when the canvas grows" problem but keeps the destructive re-rendering, the
  whole-bitmap undo, and the reflow cascade.
- **Layer the document over the existing bitmap pipeline as a cache.** Rejected as the worst of
  both: two sources of truth that must agree, and no reduction in the code that has to be correct.

## Consequences

### Positive

- Free-angle rotation becomes cheaper than the quarter-turn machinery it replaces.
- Multi-level undo becomes practical, because a document is a small immutable object.
- Export quality no longer degrades with editing; nothing is resampled until export.
- `composeImages`, `removeRegion`, `rearrangePanels`, `cropPanel`, `ComposeLayout`,
  `RearrangeLayout`, `InitialGridLayout`, the magnetic snapping helpers, and both
  `Balloon.remapped*` helpers all become unnecessary.
- The geometry that remains is pure Kotlin in `domain/`, so it is unit-testable without a device.

### Negative

- The editor, the data model, and the persistence layer are rewritten rather than amended.
- The schema changes incompatibly. Saved comics are not migrated; the app is pre-release, so the
  schema is recreated.
- Rendering cost moves from save time to draw time. Every frame projects the document instead of
  blitting one prepared bitmap.
- Deriving geometry on every frame means geometry bugs surface as visual defects everywhere at
  once, rather than being baked into one stored image.

## References

- [Comic creation workflow](../../design/comic-creation-workflow.md)
- [ADR-0001](0001-view-rotation-is-a-display-only-layer-transform.md),
  [ADR-0002](0002-panel-rotation-is-a-destructive-undoable-image-edit.md),
  [ADR-0003](0003-a-panel-quarter-turn-is-anchored-at-the-panel-top-left-corner.md)
- [ADR-0005](0005-panel-shapes-are-derived-from-the-layout.md),
  [ADR-0006](0006-balloon-scope-and-coordinate-spaces.md),
  [ADR-0007](0007-the-comic-editor-is-a-three-step-workflow.md),
  [ADR-0008](0008-a-layout-is-a-grid-plus-ordered-cuts.md)
