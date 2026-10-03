# ADR-0010: A page is a stack of tiers, and a row may divide its own width

- Status: Active
- Date: 2026-10-02
- Decision makers: Ballooner maintainers

## Context

[ADR-0008](0008-a-layout-is-a-grid-plus-ordered-cuts.md) makes a layout one grid plus an ordered
list of cuts. The grid holds a single `columnWeights` list, so a column boundary is one line
running the whole height of the page: every row is divided at exactly the same places.

That is not how comics pages are built. A comics page is a stack of **tiers**, each tier divided
into panels, and the tiers rarely agree about where their panels begin and end — three panels
across the top, one wide panel below it, two beneath that. Panel heights are shared far more
often than panel widths.

The user can already vary the panel *count* per row by merging a row's cells into one panel.
What cannot be expressed is different panel *widths* per row, which is the common case.

Three forces shape the decision:

- The grid is not wrong, it is a special case. A page whose tiers all divide at the same places
  is exactly today's grid, and it stays the thing the Rows and Columns steppers make.
- Freedom on both axes is unsound. If a row may divide its own width *and* a column may divide
  its own height, the cell where a freed row crosses a freed column has two contradictory
  definitions. A layout of this kind has to be a two-level tree with a fixed order.
- A merged span is a rectangle of cells. It only exists if the rows it covers agree about their
  column boundaries.

## Decision

**A page is a stack of rows. Each row divides its own width, and the grid's `columnWeights` is
the division that rows follow unless they have been given one of their own.**

```
Grid(rows, columns, rowWeights, columnWeights, rowSplits, spans)

rowSplits: Map<Int, List<Float>>   // row index -> that row's own column weights
```

1. **A row is free when it has its own weights.** `rowSplits[row]` is the whole state; there is
   no separate locked flag. `columnWeightsAt(row)` resolves it — the row's own weights when it
   has them, the grid's otherwise — and is the single place the question is answered.
2. **Freeing a row copies the grid's weights into it**, so freeing changes nothing on the page
   until the row is dragged. Aligning a row drops them, so it snaps back to the grid.
3. **Only rows are free. Columns are never.** Dividing is vertical first, then horizontal within
   each row. This is a property of the model, not a feature still to be built.
4. **A column boundary belongs to a row.** Every column line is identified by `(index, row)`.
   Dragging one inside a free row moves only that row's weights; dragging one inside a row that
   follows the grid moves `columnWeights`, so every row that follows it moves together. A row
   boundary spans the page and is identified by `(index, null)`.
5. **A row cannot be freed while a merged span crosses it.** Freeing is refused rather than
   silently unmerging: a span covering two rows that no longer agree has no rectangle, and
   destroying a panel the user built is the thing the layout-change warning exists to prevent.
   Equally, cells in different rows cannot be merged unless every row they cover is aligned.
6. **The reference panel is still the top-left cell**, now read through `columnWeightsAt(0)`.
   [ADR-0009](0009-the-page-takes-its-height-from-a-reference-panel.md) is unchanged.
7. **Evening the weights aligns every row.** Choosing a panel shape already promises every panel
   that shape, so it drops every row's own weights too.

## Alternatives Considered

- **A per-row `locked: Boolean` beside shared weights.** Rejected: two fields that must agree,
  and the locked flag would be the only stored thing in the layout that is not geometry. The
  cost of deriving it is that a free row dragged back onto the grid's proportions reads as
  aligned again; that is accepted, since at that moment it *is* aligned.
- **Freedom on both axes, refused where it collides.** Rejected: the collision is not an edge
  case to be refused, it is undefined. A symmetric model that is only sound when half of it is
  unused is worse than an asymmetric model that says so.
- **Leaving it to cuts.** A 3×1 grid with a vertical cut in each row already produces these
  pages, and the Custom preset can draw them today. Rejected as the primary answer: a cut is a
  free line with no notion of equal shares, so tiers cannot be made exactly equal, kept equal as
  the page reshapes, or stepped up and down in count. Cuts remain the answer for diagonals.
- **A recursive split tree.** Rejected again for the reasons in ADR-0008: moving or deleting a
  split cascades to its descendants. Two fixed levels need no re-parenting.

## Consequences

### Positive

- The layout the great majority of comics pages use is reachable by dragging, and is made of
  proportions, so it survives a change of page shape, gutter, or margin.
- The grid is unchanged for anyone who does not free a row: same steppers, same drags, same
  stored weights.
- Panel geometry stays derived ([ADR-0005](0005-panel-shapes-are-derived-from-the-layout.md)).
  Only the weights a row is divided by are new state.

### Negative

- Merging is now conditional on alignment, which is a rule the user has to meet rather than a
  thing that always works.
- The grid model is asymmetric: rows can do something columns cannot, and a user who wants a
  column of varying heights has to turn the page's structure around or reach for cuts.
- The schema gains a table and the database a version.
- `gridBoundaries` returns more lines than before — one per row for every column boundary — so
  callers that assumed a column line spans the page have to be revisited.

## References

- [ADR-0005](0005-panel-shapes-are-derived-from-the-layout.md)
- [ADR-0008](0008-a-layout-is-a-grid-plus-ordered-cuts.md)
- [ADR-0009](0009-the-page-takes-its-height-from-a-reference-panel.md)
- `app/src/main/java/com/ballooner/domain/comic/Layout.kt`
- `app/src/main/java/com/ballooner/domain/comic/GridBoundaries.kt`
- `app/src/main/java/com/ballooner/domain/comic/PanelShapes.kt`
- [The comic creation workflow](../../design/comic-creation-workflow.md)
