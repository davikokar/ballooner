# ADR-0007: The comic editor is a three-step workflow over one document

- Status: Active
- Date: 2026-09-26
- Decision makers: Ballooner maintainers

## Context

The current editor presents layout, image, and balloon editing simultaneously on one canvas, with
an Edit / View mode toggle. Panel handles, balloon handles, crop frames, and the add-panel
affordances compete for the same taps, which is why `canEditBalloons` exists to suppress balloon
editing while a panel is selected. Adding a panel requires choosing a position relative to an
existing panel before any image is visible, because the layout is derived from the images.

Users think about a comic in stages: what the page looks like, what is in it, and what is said.
The product intent is to follow that order without making earlier decisions expensive to revisit.

## Decision

**Comic creation is divided into three steps over a single document: Layout, Placement, and
Balloons.**

1. Each step edits a **disjoint** part of the document: the Layout step writes the layout, the
   Placement step writes each panel's image and transform, the Balloon step writes balloons.
   Because the parts are disjoint, moving between steps loses nothing and never prompts to save
   or discard.
2. While a step is active, the parts owned by later steps are drawn dimmed and are not
   interactive. In the Layout step images and balloons are dimmed; in the Placement step balloons
   are dimmed; in the Balloon step everything is live.
3. Dimming is a render flag derived from the active step. The active step is view state and is
   never persisted with the comic.
4. A **step switch** replaces the Edit / View mode toggle. On first creation the steps are
   presented in order as a guided flow; afterwards the user moves between them freely. A preview
   without editing affordances is reachable from any step.
5. **Focus** remains available in the Placement and Balloon steps: one panel fills the canvas for
   closer work, with navigation between panels. Focus is view state, not document state. While
   focused, only that panel's image and panel balloons are editable; an overlapping comic balloon
   is drawn but edited in the unfocused view.
6. The only step transition that can lose work is a layout change that deletes panels, which is
   confirmed first, naming what will be deleted.

## Alternatives Considered

- **Keep one combined editing surface.** Rejected: it is the current design, and it forces
  mutually exclusive gesture handling on a shared canvas, which the `canEditBalloons` workaround
  already demonstrates.
- **A strict linear wizard that locks earlier steps once completed.** Rejected: revisiting the
  layout after placing images is a stated requirement, and locking would make the common case of
  "this panel should be wider" impossible without starting again.
- **Save and restore per-step state on each transition.** Rejected as unnecessary. It is only
  needed if the steps write overlapping state; keeping the parts disjoint removes the problem
  rather than managing it.
- **Separate screens per step.** Rejected: the page must stay visible and in place across steps
  so the user can judge the effect of an edit on the whole comic.

## Consequences

### Positive

- Each step has a small, unambiguous gesture vocabulary, so panel and balloon interactions no
  longer compete for the same taps.
- Revisiting an earlier step is free, which is the behaviour the workflow promises.
- Panels exist before images do, so adding a panel no longer requires positioning it relative to
  an existing one.
- The step model gives each piece of UI an obvious home, which keeps the editor from collapsing
  back into one large composable.

### Negative

- Three steps plus focus and preview is more UI surface than one canvas with a mode toggle.
- The disjointness rule is a constraint on every future feature: any control that would write
  another step's state breaks the "moving between steps loses nothing" guarantee.
- Users who want a single free-form canvas must switch steps to complete one thought, for example
  widening a panel and then re-centring its image.

## References

- [Comic creation workflow](../../design/comic-creation-workflow.md)
- [UI vocabulary](../../../.github/instructions/ui-vocabulary.instructions.md)
- [ADR-0004](0004-a-comic-is-a-declarative-document-rendered-on-demand.md)
