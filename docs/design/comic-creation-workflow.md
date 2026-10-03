# Comic creation workflow and supporting architecture

- Status: Implemented
- Date: 2026-09-26

This document describes the redesigned Ballooner comic creation workflow from the user's
perspective, and the architecture that supports it. It replaces the current editor, in which a
comic is a single flattened bitmap and every structural edit re-renders pixels.

The product intent is unchanged: let a user build a comic of one or more panels, with several
types of balloons, from images already on the device. A comic is always a single page;
multi-page comics are out of scope.

## Why the redesign

Today a comic *is* a bitmap. Panels are rectangles expressed as fractions of that bitmap, and
balloons live in the same space. Consequently:

- Every structural edit (add, move, resize, crop, rotate a panel) re-renders the whole page.
- The coordinate space itself changes on every edit, so "this panel was not touched" cannot be
  expressed, and every balloon must be remapped after every edit.
- Edits are destructive and lossy, so undo needs a whole-bitmap snapshot and repeated edits
  degrade image quality.

Rotating a panel is the worst case: it changes the panel's aspect ratio, which changes the page,
which forces neighbouring panels to reflow, which forces every balloon to be remapped — on top of
rotating pixels. Four coupled problems for what should be one field.

The redesign inverts the dependency: **the layout is chosen first and is independent of the
images**, and **the comic is a document that is rendered on demand** rather than a bitmap that is
edited in place.

---

# Part 1 — Workflow

## The three steps

Comic creation is divided into three steps:

1. **Layout** — choose the shape of the panels and how the page is divided into them.
2. **Placement** — choose an image for each panel and adjust how it sits in its frame.
3. **Balloons** — add and edit balloons.

On first creation the steps are presented in order, as a guided flow. Afterwards the editor shows
a persistent three-way step switch, and the user moves between steps freely. The step switch
replaces today's Edit / View toggle; a preview (no editing affordances) is available from any
step.

Each step writes a different part of the comic, so moving between steps never loses work. Moving
between steps never prompts either; the only prompts are a layout change that deletes panels,
described under [Changing the layout later](#changing-the-layout-later), and leaving the editor
with unsaved work, described under [Saving](#saving).

While a step is active, the parts of the comic owned by *later* steps are shown dimmed and are
not interactive:

| Active step | Panels and frames | Images | Balloons |
|-------------|-------------------|--------|----------|
| Layout | editable | dimmed | dimmed |
| Placement | visible, not editable | editable | dimmed |
| Balloons | visible, not editable | visible, not editable | editable |

## Step 1 — Layout

### Layout kind

The Layout step opens on a preset picker: four cards — **Single**, **Strip**, **Grid**,
**Custom** — each showing the arrangement it makes, filling the step in place of the canvas. The
card matching what the comic already is carries an ACTIVE badge, read back off the document
rather than stored alongside it. Tapping a card continues into that preset's options on the same
ground — no frame, no overlay — with a breadcrumb that continues the heading, "Select panel
preset / Single", whose leading part goes back.

Opening a preset's options is not choosing it. The options are taken as a whole by moving on to
the Images step — with **Next**, or by tapping the step itself — and thrown away as a whole by
going back, with **Back** or with the breadcrumb, which leaves the comic as the picker found it
and the preset it already was still active. So a user can look inside every preset in turn
without changing anything. The Images step carries the same pair, where they simply step between
steps.

### Panel shape, and why there is no page shape

The user never chooses a page shape. The page is always one unit wide, and its height is derived
from the shape of the **reference panel** — the first panel of the grid.

This is because a page shape is the wrong question for a single-panel comic: the panel *is* the
page, less its margin, so asking for a page shape and then showing a panel asks the same thing
twice and invites the two answers to disagree. Something still has to supply the one measurement
the page needs, though, because grid weights only divide whatever height they are given and cuts
are stored in normalized coordinates — both describe proportions *between* panels, never an
absolute size. So the reference panel supplies it.

For a **Single** comic the choices are the shape of the one panel, shown as four tiles over a
live preview:

- **Square** — 1:1
- **Ratio** — 3:2, with a control that turns it on its side to 2:3
- **Custom** — two sliders, width and height in whole units from 1 to 24
- **Auto** — the shape of the image chosen in the Placement step

The preview draws the panel as it will really be rather than the number that was asked for, so a
ratio the page cannot reach shows what it actually becomes.

Because the height is solved for every time rather than stored, a panel promised a shape keeps
it: moving the margin or gutter slider re-solves the page instead of quietly breaking the
promise. See [ADR-0009](../architecture/decisions/0009-the-page-takes-its-height-from-a-reference-panel.md).

Resizing is the one edit that reshapes the reference panel itself. Dragging the grid line beside
it would otherwise re-solve the page and move every other panel, so the shape is instead read
back off the panel the drag has just made: the reference panel still supplies the page height, it
has simply been given a new shape. A resize therefore changes only the two panels either side of
the line, and none of the shape tiles is shown active afterwards, because the panels no longer
share one shape. Choosing a tile puts them all back to it.

### Layout type

A layout is **a grid plus an ordered list of cuts**. Every preset the user picks is a starting
point in that single model. This is deliberate: a horizontal or vertical cut produces exactly
what a grid line already produces, so keeping them as two separate layout kinds would be two ways
to express the same thing.

**The grid** divides the page into rows and columns:

- **Single panel** — a 1×1 grid.
- **Strip** — a 1×n row or an n×1 column.
- **Grid** — n×m cells.
- **Blank page** — a 1×1 grid, the starting point for a freely drawn layout.

In any grid the user can:

- **Resize** — drag a grid line to change row or column proportions.
- **Free a row** — a comics page is really a stack of tiers, and tiers rarely agree where their
  panels begin and end. Each row carries a lock: unlocked, that row divides its own width and its
  vertical gutters move independently of the rest of the grid; locked, it follows the grid's own
  division along with every other locked row. Freeing a row changes nothing until it is dragged.
  Only rows can be freed, never columns — see
  [ADR-0010](../architecture/decisions/0010-a-page-is-a-stack-of-tiers.md).
- **Merge** — tap panels to select two or more adjacent ones, then combine them into a single
  panel. Merging works horizontally and vertically, so a 3×3 grid can hold a wide panel spanning
  the top row, a tall panel spanning two rows of the left column, and so on. The selection must
  form a rectangle; an L-shaped merge is refused, and so is one spanning a freed row, which no
  longer agrees with its neighbours about where the merged panel's sides would be.
- **Unmerge** — split a merged panel back into the cells it covers.

**Cuts** are straight lines the user traces over the page, and the regions they separate become
panels. A cut can be horizontal, vertical, or **diagonal at any angle**. Diagonals are the reason
cuts exist: they are the one thing a grid cannot express.

Each cut has a scope, determined by how it is drawn:

- **Page cut** — crosses the whole page and splits every panel in its path.
- **Panel cut** — crosses a single panel and splits only that one, leaving its neighbours
  untouched.

Both scopes are needed. A page cut is the natural way to slash a diagonal across an entire page;
a panel cut is the only way to angle one panel without disturbing the rest, and it is what makes
layouts like "two panels on top, three below" possible at all.

Cuts can be moved and re-angled after they are drawn, and deleting a cut merges the panels on
either side of it back together. Cuts do not depend on each other: removing one never invalidates
another, however they were drawn.

A cut is deleted by dragging it away rather than by a control of its own. Each end has a handle,
and a cut whose two ends have both been swung clear of the page divides nothing: it is dropped
instead of being kept as a line that cannot be seen. Because the ends only give the line its
direction, one end left on the page always keeps the cut alive, so this cannot happen by
accident.

A trace does not have to begin on the panel it cuts. It can start anywhere on the ground the
page is drawn on, which is the only way to aim a line at the very edge of a panel without the
finger covering what it is aiming at.

Cuts are the only source of non-rectangular panels; a grid on its own always produces rectangles.

### Comic style

Gutter, border, and corners are comic-level settings the user can change at any time. Every one of
them is a fraction of the page's width, so the same number means the same gap on both axes and at
any export size:

- **Gutter** — space between adjacent panels, and the space round the outside of the page as well.
  There is no separate page margin: one number keeps every gap in the comic the same width.
- **Border thickness** — the panel outline weight.
- **Corner radius** — how rounded panel corners are, from nothing up to half the page's width.
  Every corner rounds, including the ones a diagonal cut leaves, and a corner never takes more
  than half of either edge meeting it — which is what lets the radius be turned all the way up
  until a square panel closes into a circle.

Changing any of these restyles the whole comic at once. It moves and resizes panel frames, but it
never deletes anything and never disturbs image placement or balloons.

### Changing the layout later

When the user switches preset or changes a grid's dimensions, panels are matched between the old
and new layout **by index, in reading order**:

- **Same panel count, different shape** (for example 1×4 to 2×2) — every image and balloon is
  kept. Images stay centred where the user put them, keep their rotation and zoom, and continue
  to fill their new frames.
- **More panels** (for example 1×4 to 2×3) — existing panels are unchanged and the new panels are
  empty.
- **Fewer panels** (for example 1×4 to 1×2) — the trailing panels are removed. The user is warned
  first, naming how many panels, images, and balloons will be deleted, and can cancel.

Edits that change one part of a layout follow local rules instead, so unrelated panels are never
disturbed:

| Edit | Result |
|------|--------|
| Merge panels | The merged panel keeps the first panel's image and balloons. The other panels' content is discarded, with a warning if any of them is not empty. |
| Unmerge a panel | Its image and balloons stay with the first of the cells it covered; the rest become empty. |
| Add a page cut | Every panel the cut crosses becomes two, and each one's image and balloons stay with the first of its two pieces. |
| Add a panel cut | Only the crossed panel becomes two, and its image and balloons stay with the first of the two. |
| Remove a cut | The panels on either side become one, which keeps the first panel's content. |
| Drag a grid line, or move or re-angle a cut | Panels change shape. Nothing is added, removed, or emptied. |

Comic-scoped balloons (see [Step 3](#step-3--balloons)) are never affected by a layout change.

## Step 2 — Placement

An empty panel invites the user to add an image. The user can fill one panel at a time, or
multi-select images and fill the empty panels in reading order.

A newly placed image is **fitted to cover** its panel: scaled so it fills the frame entirely,
centred, unrotated. No panel ever shows empty space where an image should be.

The user can then adjust the image within its frame:

- **Pan** — drag.
- **Zoom** — pinch. Zooming out stops when the image exactly fills the panel.
- **Rotate** — two-finger twist, at any angle, with magnetic snapping near 0°, 90°, 180°, and
  270°. Zoom increases automatically if a rotation would otherwise expose an empty corner.

Rotation never changes the panel's shape or the page layout. It turns the picture inside a fixed
frame, and the parts that fall outside the frame are simply not shown.

Other placement actions:

- **Swap** — drag an image from one panel onto another to exchange them.
- **Replace** — choose a different image for a panel, resetting its placement.
- **Remove** — empty the panel.

## Step 3 — Balloons

The user adds balloons of each supported type (speak, thought, whisper, yell, caption) and edits
their text, font, text size, shape, position, size, and tail — as in the current editor.

Every balloon has a **scope**:

- **Panel balloon** (default) — belongs to one panel. It is clipped to that panel, so any part
  that would overflow, including its tail, is not drawn. It moves, resizes, and rotates with its
  panel, and is deleted with it.
- **Comic balloon** — belongs to the page. It is drawn above every panel and clipped only by the
  page, so it can span the gutter and cross panel boundaries. It survives layout changes.

Scope is a property the user can toggle on a selected balloon. Toggling it keeps the balloon
exactly where it appears on screen; only the space its position is measured in changes.

Balloons are drawn in the order they were added, so a newer balloon overlaps an older one. Comic
balloons always draw above every panel balloon.

Reshaping a panel never resizes its balloons. A panel balloon keeps its size, tail, and text
size, and stays in the same relative position within its panel. If the panel becomes too small
for it the balloon is clipped, and the user adjusts it if they want to.

## Focusing a panel

In the Placement and Balloon steps the user can focus a panel, filling the Canvas with that panel
alone for closer work, and step between panels without leaving focus. Focus is a view state: it
is never saved with the comic and it changes nothing in the document.

While a panel is focused:

- Only that panel's image and its panel balloons are editable.
- A comic balloon that overlaps the panel is still drawn, so the user can see what covers what,
  but it is edited only in the unfocused view where its whole shape is visible.
- A panel with angled sides is focused by its bounding box.

## Focusing a panel

In the Placement and Balloon steps the user can focus a panel, filling the Canvas with that panel
alone for closer work, and step between panels without leaving focus. Focus is a view state: it
is never saved with the comic and it changes nothing in the document.

While a panel is focused:

- Only that panel's image and its panel balloons are editable.
- A comic balloon overlapping the panel is still drawn, so the user can see what covers what, but
  it is edited in the unfocused view, where its whole shape is visible.
- A panel with angled sides is focused by its bounding box.

## Saving

The editor works on a copy of the comic. Edits change only that copy; the library is rewritten
when the user asks for it (see [ADR-0011](../architecture/decisions/0011-the-editor-works-on-a-copy.md)).

- **Save** sits in the balloons step, at the end of the guided flow. It asks for a title, writes
  the comic to the library, and confirms. It does not produce a PNG.
- A comic started from the library's **+** button exists only in the editor until it is first
  saved. Abandoning it leaves the library exactly as it was.
- Leaving the editor with unsaved work — by the back arrow or the system back gesture — asks
  **Keep your changes?** and offers *Save* or *Discard*. With nothing unsaved, leaving is silent.

## Export

Exporting is a separate action, offered as **Save as PNG** in the editor's top bar and as
**Share as PNG** in the library. It renders the comic at the source images' native resolution and
writes a PNG. Because editing never touches pixels, export quality is independent of how much
editing was done.

---

# Part 2 — Architecture

## Core principle

**A comic is a declarative document. Pixels are produced only for display and export.**

The document records the layout, which image is in which panel, how each image is transformed
inside its frame, and the balloons. It never records composited pixels. Imported images are
copied into app storage once and are never modified.

Everything the user does in steps 1–3 is a small edit to disjoint parts of that document, which
is what makes free movement between steps possible with no state to save or restore.

## Document model

The model lives in `domain/` and has no Android dependencies.

```
Comic
    pageShape: PageShape                  // square | portrait | landscape | strip
    style: ComicStyle                     // margin, gutter, borderThickness, cornerRadius
    layout: Layout
    panels: List<Panel>                   // index-aligned with the layout's shapes, reading order
    balloons: List<Balloon>               // z-order is list order

Layout
    grid: Grid(rows, columns, rowWeights, columnWeights, rowSplits, spans)
    cuts: List<Cut>                       // applied in order, after the grid

Span(firstRow, firstColumn, rowCount, columnCount)   // a merged panel; an unmerged cell is 1x1
Cut(a, b, scope)                          // the line through page-normalized points a and b
                                          // scope: WholePage | AtPoint(anchor)

Panel
    id
    image: PanelImage?                    // null while the panel is empty

PanelImage
    sourceUri                             // an untouched imported image
    centre: (u, v)                        // image point shown at the panel centre, image-local
    zoom: Float                           // multiple of the minimum covering scale, >= 1
    angleDegrees: Float

Balloon
    id
    scope: Panel(panelId) | Comic
    ...type, text, font, shape, tail, geometry
```

Single-panel and strip layouts are grids, and a freely drawn layout is a 1×1 grid with cuts, so
the model has one layout kind to support even though the user picks from several presets.

### Panel shapes are derived, never stored

```
panelShapes(layout, pageHeight, style): List<Polygon>
```

is a pure function returning one convex polygon per panel, in reading order. `pageHeight` is
itself derived, from the sizing rule and the reference panel, so nothing about the page's
geometry is stored either. Panels are produced in two stages:

1. **The grid.** The gutters are taken out of the content area first, and the row and column
   weights divide what is left, so equal weights produce equal cells however many there are. A
   merged span covers the gutters it straddles.
2. **The cuts, in the order they were drawn.** A page cut replaces every polygon it crosses with
   the two pieces on either side; a panel cut does the same to its target alone. Each piece is
   inset by half the gutter from the cut line.

Clipping a convex polygon against a half-plane is a small, well-understood routine; it behaves
identically at any angle, which is what makes diagonal cuts no harder than horizontal ones.
Insetting during the split is what produces the gutters, so no general polygon offsetting is
needed, and every panel stays convex however many cuts are made.

No panel shape is ever persisted. This is the single most important
consequence of the redesign:

- Changing the gutter, margin, panel shape, or panel proportions is a one-field edit. Nothing
  cascades, because there is nothing stored to keep in sync.
- A layout change cannot corrupt panel geometry, because panel geometry is recomputed from
  scratch every time.
- The reflow, magnetic snapping, and rearrange machinery disappear entirely.

### What diagonal cuts cost

The splitting is angle-agnostic, so diagonals cost nothing there. Three things elsewhere become
approximations once a panel is no longer a rectangle:

- **Covering the panel.** `coverScale` uses the panel's bounding box. A triangular panel's box is
  bigger than the panel, so its image is zoomed somewhat more than strictly necessary. This is
  always safe — covering the box covers the panel — and it avoids testing containment against an
  arbitrary polygon at every gesture.
- **Panel-local coordinates.** Balloon positions are fractions of the bounding box, so some
  positions fall outside the visible panel and the balloon is clipped. Balloons are positioned by
  dragging, so the user sees this as they do it.
- **Reading order.** Panels formed by diagonals do not fall into clean rows, so ordering them
  needs a tolerance band rather than an exact rule. Reading order is load-bearing because it
  drives panel index matching.

A panel cut is stored in its target panel's own coordinates, so it follows that panel when a grid
line is dragged or the page is reshaped, instead of drifting away from it.

### Cuts are anchored by position, not by index

A cut is the line through two page-normalized points, plus a scope. A page cut splits every panel
it crosses. A panel cut carries an **anchor**: the point where the traced line first passes
inside a panel, which is where the finger went down when the trace began on a panel and the edge
it crossed when the trace began beside one. When the layout is evaluated, the cut splits
whichever panel currently contains that anchor.

Resolving the target by position rather than by index is what makes cuts independent of each
other. Deleting or moving an earlier cut merges or reshapes panels, but a later cut's anchor
simply lands in whatever panel now occupies that spot, so it still has something sensible to
split. There is no dependency graph to maintain and no cascading deletion.

If an anchor ends up in a gutter or off the page, its cut is skipped and the Layout step marks it
so the user can move or delete it. That is a policy choice, not a geometric necessity: it lives
in a single function that maps an anchor to a target panel, so it can be changed to something
else — nearest panel, for instance — without touching anything around it.

### Reading order

Panels are ordered by centroid: top to bottom, then left to right, with a tolerance band that
treats nearby centroids as belonging to the same row. Reading order defines the panel list, so it
drives panel index matching after a layout change and the order in which panels are filled during
placement. The tolerance is the only tunable part and needs tests covering diagonally cut pages.

### Coordinate spaces

Choosing the right units is what makes layout changes non-destructive.

| Value | Space | Rationale |
|-------|-------|-----------|
| Panel balloon position | panel-local `(u, v)` in `[0, 1]` | Stays proportionally placed when its panel is resized or reshaped. |
| Comic balloon position | page-local `(u, v)` in `[0, 1]` | Independent of panels. |
| All balloon sizes, tail lengths, text sizes | fraction of **page width** | A single uniform unit for both axes, so a balloon never stretches when a panel's aspect ratio changes, and reshaping a panel never resizes its balloons. |
| Panel image centre | image-local `(u, v)` | Independent of the frame it is shown in. |
| Panel image zoom | **multiple of the minimum covering scale** | See below. |

Panel-local coordinates are measured in the panel's bounding box, so they remain well defined for
the non-rectangular panels that cuts produce.

### Zoom is relative to the covering scale

`coverScale(frameSize, imageAspect, angleDegrees)` is the smallest scale at which the rotated
image still covers the frame. For a non-rectangular panel the frame is the panel's bounding box,
which covers the polygon as well. Storing `zoom` as a multiple of it means:

- `zoom >= 1` *is* the "image always fills its panel" invariant — enforced by the type, not by a
  clamp that has to be reapplied after every edit.
- A layout change needs no re-fitting. The same `zoom` value still covers the new frame, whatever
  its size or aspect ratio.
- Rotation needs no special handling. `coverScale` already accounts for the angle, so turning an
  image simply makes the effective scale larger.

The panel image centre is the only value that still needs clamping, and only on read: the visible
pan range depends on the current zoom, angle, and frame.

`coverScale` is a pure function and the one genuinely new piece of geometry in the redesign. It
deserves thorough unit tests.

## Rendering

Panels and balloons are drawn directly by Compose from the document:

1. Compute panel shapes.
2. For each panel, clip to its shape and draw its image with the transform derived from
   `centre`, `zoom`, and `angleDegrees`; then stroke its outline.
3. Draw each panel's balloons, in the order they were added, clipped to that panel's shape.
4. Draw comic balloons above everything, in the order they were added, clipped to the page.

Export uses the same pure layout and transform functions at full resolution, so the preview and
the exported PNG cannot drift apart.

## Persistence

Room stores the document. Tables: comics (sizing rule, style, layout), panels (image reference,
its own proportions, and transform), balloons (scope and geometry). A layout persists as its grid
dimensions, weights, and merged spans, plus its ordered list of cuts. No bitmap is ever stored for
a comic; only the imported source images, which are reference-counted and deleted when no panel
uses them.

`ImageStore` shrinks to two responsibilities: import a source image, and export a rendered comic.

Existing saved comics are not migrated. The app is pre-release, so the schema is recreated.

## Undo

Undo becomes a stack of document snapshots. A document is a small immutable object, so this is
cheap, supports many levels, and works uniformly across all three steps — replacing the current
single-slot, whole-bitmap undo.

## What this removes

- `ImageStore.composeImages`, `removeRegion`, `rearrangePanels`, `cropPanel`.
- `ComposeLayout`, `RearrangeLayout`, `InitialGridLayout`.
- Magnetic drag and resize destinations, and panel reflow after resize.
- `Balloon.remappedBetween` and `Balloon.remappedByQuarterTurns`.
- The single-slot image edit undo snapshot.
- ADR-0002 and ADR-0003, which exist only to manage the reflow cascade.

## Decisions recorded

The decisions in this document are recorded as ADRs:

1. [ADR-0004](../architecture/decisions/0004-a-comic-is-a-declarative-document-rendered-on-demand.md)
   — a comic is a declarative document rendered on demand. Supersedes ADR-0002 and ADR-0003.
2. [ADR-0005](../architecture/decisions/0005-panel-shapes-are-derived-from-the-layout.md) — panel
   shapes are derived from the layout, page height, and style.
3. [ADR-0006](../architecture/decisions/0006-balloon-scope-and-coordinate-spaces.md) — balloon
   scope, z-order, and coordinate spaces.
4. [ADR-0007](../architecture/decisions/0007-the-comic-editor-is-a-three-step-workflow.md) — the
   three-step editor workflow.
5. [ADR-0008](../architecture/decisions/0008-a-layout-is-a-grid-plus-ordered-cuts.md) — a layout
   is a grid with merged spans plus an ordered list of cuts.
6. [ADR-0009](../architecture/decisions/0009-the-page-takes-its-height-from-a-reference-panel.md)
   — the page takes its height from a reference panel, so there is no page shape to choose.

The UI vocabulary in
[.github/instructions/ui-vocabulary.instructions.md](../../.github/instructions/ui-vocabulary.instructions.md)
also needs new canonical terms: Page, Layout kind, Reference panel, Panel shape, Step switch,
Layout step, Placement step, Balloon step, Gutter, Cut, Merged panel, Panel image transform,
Panel balloon, and Comic balloon. It has been updated alongside this document.

## Deferred

**Curved cuts.** The intent is for the user to trace lines freely, and a curve is a natural thing
to want to trace. It is deferred because straight cuts keep every panel convex, and convexity is
what makes three separate things easy and exact: splitting a panel, producing gutters by
insetting during the split, and covering a panel with a rotated image. A curved cut breaks all
three at once — panels become non-convex, gutters need general polygon offsetting, and finding
the regions a set of curves carves out is a substantially harder problem than clipping against a
line. Worth revisiting once straight cuts are working, as an addition rather than a replacement.

## Open questions

- **How wide is the reading-order tolerance band?** A value has to be chosen and tested against
  pages cut at shallow angles, where "same row" is genuinely ambiguous.
