# ADR-0011: The editor works on a copy, and the comic is written only when it is saved

- Status: Active
- Date: 2026-10-02
- Decision makers: Ballooner maintainers

## Context

Every edit in the comic editor has been written to the database as it was made: `commit` updated
the state and persisted in the same breath. That gave the user a document that could never be
lost, at the cost of one that could never be left alone — there was no way to try a layout, dislike
it, and walk away, because trying it had already replaced the saved comic. Undo was the only route
back, and it does not survive leaving the editor.

Creating a comic wrote a row before the editor opened, so abandoning a new comic left an empty one
in the list.

The Save button on the Balloon step exported a PNG. That conflated two different things: keeping
the comic, and producing a picture of it. A user who wanted to keep their work had no button that
said so, and a user who pressed Save got a file picker.

## Decision

**The editor edits a copy. The database is written only when the user saves.**

1. **Loading takes a copy.** The editor reads the comic once and works on it in memory. Nothing it
   does reaches the repository until a save.
2. **Save writes the document and nothing else.** The Save button on the Balloon step asks for a
   title, then writes the comic. It no longer exports a PNG; exporting lives on the comic's own
   bar and in the comic list, where it is named for what it does.
3. **A new comic is not written until its first save.** Creating one navigates to the editor with
   no id; the editor starts from a blank document and inserts a row the first time it is saved.
   Abandoning a new comic therefore leaves nothing behind, with nothing to clean up.
4. **Leaving with unsaved changes asks.** Going back from the editor while the document differs
   from what was last saved offers **Save**, **Discard**, or to carry on editing. Discard leaves
   the saved comic as it was, and leaves a never-saved comic unwritten.
5. **Unsaved means unsaved.** The working copy lives in the ViewModel, so it survives rotation but
   not the process. That is the plain meaning of the word and the user is told, by the prompt,
   whenever work is at stake.
6. **Edits outside the editor still write at once.** Renaming, duplicating, setting a cover, and
   deleting are single acts on a saved comic made from the list, not an editing session, so they
   are applied immediately.

## Alternatives Considered

- **Keeping continuous saving and adding an explicit Save.** Rejected: the button would be a lie,
  since the comic would already be saved, and leaving could not offer to discard anything.
- **Continuous saving plus a snapshot restored on discard.** This keeps work safe across a crash
  and still allows discarding. Rejected for now because it makes every edit a write, which is
  what the user asked to stop, and because "saved" would mean two different things depending on
  who was asking. Worth revisiting if losing work to a crash proves to matter.
- **Writing the row for a new comic up front and deleting it on discard.** Rejected: the list
  would show a comic that does not exist yet, and a crash would leave it there for ever. Not
  writing it at all has neither problem.
- **Autosaving to a draft table.** Rejected as a larger mechanism than the problem: it needs its
  own schema, its own lifecycle, and a rule for what happens when a draft and a saved comic
  disagree.

## Consequences

### Positive

- A comic can be experimented with and abandoned, which is what makes the editor safe to explore.
- Save means what it says, and export is named for what it does.
- The database is written once per session rather than once per gesture.
- An abandoned new comic leaves nothing behind.

### Negative

- Work in progress is lost if the process dies. This is the accepted cost of the decision and the
  reason the leave prompt exists.
- The ViewModel now holds state the database does not, so "the comic" means the working copy
  inside the editor and the saved document everywhere else.
- Tests that read the repository to see what the editor did must save first, which is a more
  honest thing to assert anyway.

## References

- [ADR-0004](0004-a-comic-is-a-declarative-document-rendered-on-demand.md)
- [ADR-0007](0007-the-comic-editor-is-a-three-step-workflow.md)
- `app/src/main/java/com/ballooner/ui/comiceditor/ComicEditorViewModel.kt`
- [The comic creation workflow](../../design/comic-creation-workflow.md)
