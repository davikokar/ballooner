# ADR-0009: The page takes its height from a reference panel

- Status: Active
- Date: 2026-09-28
- Decision makers: Ballooner maintainers

## Context

Until now a comic stored a `PageShape`: a four-value enum (square, portrait, landscape, strip)
that the user picked in the Layout step, and from which every panel's shape was then derived.

Two things were wrong with it.

The first is conceptual. For a single-panel comic there is no meaningful difference between the
page and the panel — the panel is the page, less the margin — so asking the user to choose a page
shape and *then* showing them a panel is asking the same question twice and inviting the two
answers to disagree. The user thinks about the shape of the picture they are making, not about
the sheet it is printed on.

The second is that the enum could not express what the Layout step needed to offer: an arbitrary
proportion chosen on a slider, and a panel that takes its shape from whatever image is placed in
it. Both require a real number, and one of them is not known until a later step.

The constraint that shapes the answer is that panels alone cannot supply a page height. The page
is always one unit wide, so its height is the single measurement the document has to provide. But
`Grid.rowWeights` and `columnWeights` only divide whatever height they are given, and `Cut`
endpoints are stored in normalized coordinates on both axes. Both describe proportions *between*
panels. Neither can produce an absolute height. Abolishing the page shape therefore means
replacing it with a rule, not simply deleting it.

## Decision

The page's height is **derived, never stored**. A comic stores a `PageSizing` rule instead of a
page shape, and the height is solved for every time it is needed:

```kotlin
sealed interface PageSizing {
    data class Ratio(val value: Float) : PageSizing  // width divided by height
    data object FromImage : PageSizing
}

fun pageHeightOf(sizing, layout, style, panels): Float
```

The rule names the shape of the **reference panel**, which is the first panel of the grid. The
reference panel's *width* is fixed by the margin, the gutters, and the column weights alone —
none of which depend on the page's height — so the height that makes that panel come out at the
wanted ratio can be solved for directly rather than searched for.

`PageShape` is deleted. Every function that took one now takes a plain `pageHeight: Float`.
`Comic.pageHeight` is a computed property.

`FromImage` reads the proportions of the image in the reference panel. So that this stays a pure
domain calculation, `PanelImage` now records a `sourceAspect`, captured when the image is
imported, rather than the geometry waiting on a decoded bitmap.

In the Layout step the user picks a **panel** shape, never a page shape. For a single-panel comic
the choices are square, 2:3, 3:2, a custom ratio, and from the image.

## Alternatives Considered

**Keep the page shape and add cases to the enum.** A custom ratio needs a float and a shape taken
from an image needs a value that is not known yet, so the enum would have had to become a value
type regardless. Having done that, keeping it user-facing would have preserved the original
complaint: two ways to say the same thing about a single-panel comic.

**Justified layout, where every panel carries its own aspect and row heights are summed.** This
derives the page from the panels with no reference and no ratio chosen anywhere, which is the
purest reading of "the page is derived from the panels". Rejected because it takes authority away
from `rowWeights`, which would leave grid-line dragging (ADR-0008) with nothing to mean, and
because a panel cut diagonally has no intrinsic aspect to contribute. It reopens the layout model
to solve a problem in the Layout step's UI.

**Store the derived height as a cached column.** Rejected for the reason given in ADR-0005: a
stored derivation is a thing that can drift from what it was derived from. The arithmetic is a
handful of operations.

**Apply the chosen ratio once and let it drift when the margin or gutter changes.** Rejected
because it breaks the promise the user was given. Solving on every read means a square panel
stays square when the margin slider moves.

## Consequences

### Positive

- The user is asked about the thing they can see. For a single-panel comic there is exactly one
  shape question, and its answer is visibly the answer.
- A panel promised a shape keeps it. Changing the margin, the gutter, or the grid re-solves the
  height rather than invalidating the promise.
- Arbitrary and deferred proportions both fall out of the model rather than being special cases.
- One less piece of stored state that can contradict the rest of the document.

### Negative

- A layout change that reshapes the reference panel now reshapes the whole page. `withLayout`
  therefore has to measure the old and new layouts **on the same page** before matching panels by
  overlap; measured on their own pages, a merge that changes the page's height would slide every
  panel out from under the one it ought to inherit from and silently discard its contents. This
  is subtle and is pinned by a test.
- The reference panel is the first panel of the grid and is not chooseable. A user who wants the
  page shaped by some other panel cannot say so.
- Migrating to DB v9 carries an existing comic's page shape across as its reference panel's
  ratio. That is exact for a single-panel comic, and **reshapes a multi-panel one**, because the
  old column was a page measurement and the new one is a panel measurement.
- `PanelImage.sourceAspect` is a second record of something the bitmap already knows. It is a
  hint for sizing only; the decoded bitmap remains authoritative for drawing. It is null for
  images imported before v9, and such a panel falls back to square under `FromImage`.

## References

- Refines [ADR-0004](0004-a-comic-is-a-declarative-document-rendered-on-demand.md), which listed
  the page shape among the things the document records. It no longer does.
- Refines [ADR-0005](0005-panel-shapes-are-derived-from-the-layout.md). Panel shapes are still
  derived from the layout; the page they are derived on is now derived too.
- [ADR-0008](0008-a-layout-is-a-grid-plus-ordered-cuts.md) — the grid and cuts whose relativeness
  is the reason a reference panel is needed at all.
- `app/src/main/java/com/ballooner/domain/comic/PageSizing.kt`
- `app/src/main/java/com/ballooner/data/Migrations.kt` — `MIGRATION_8_9`
- [docs/design/comic-creation-workflow.md](../../design/comic-creation-workflow.md)
