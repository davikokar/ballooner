---
description: Canonical names for Ballooner screens, areas, controls, and UI interactions.
applyTo: "app/src/**/ui/**/*.kt"
---

# UI vocabulary

Use these canonical names when discussing, implementing, and testing Ballooner's UI.
Prefer the product-facing name in prose and include the Kotlin symbol when extra precision
is useful. Preserve these meanings when adding new UI terms.
Treat aliases as input synonyms only. Use the canonical bold name in responses,
implementation notes, and tests.

## Screens

- **Comic list screen** (`ProjectListScreen`): The start screen that lists saved comics. Aliases: **project list screen**, **projects screen**, **comic library**
- **Comic editor screen** (`ProjectScreen`): The screen for assembling and editing one comic. Aliases: **project editor screen**, **editor screen**
- **Settings screen** (`SettingsScreen`): The screen containing app preferences, support,
  and legal information. Aliases: **preferences screen**, **app settings screen**

Use **screen** only for a full navigation destination. Use **dialog** for modal content and
**area** for a region within a screen.

## Shared navigation

- **Top bar**: The bar at the top of a screen containing its title and navigation actions.
- **Back button**: The top-bar control that returns to the previous screen.
- **Overflow menu**: The three-dot top-bar menu. Qualify it by screen when needed, such as
  **editor overflow menu**.
- **Settings menu item**: The overflow-menu item that opens the Settings screen.

## Comic list screen

- **Comic list**: The scrolling collection of saved comics.
- **Comic card** (`ProjectRow`): One saved comic, including its thumbnail, title, and last
  edited time. Do not call this a panel; **panel** has a separate editor meaning.
- **Comic thumbnail** (`ProjectThumbnail`): The image preview at the top of a Comic card.
- **Create comic button** (`ComicFab`): The floating plus button that creates a comic.
- **Empty state** (`EmptyState`): The message and create action shown when no comics exist.
- **Comic delete button**: The close badge revealed by long-pressing a Comic card.
- **Delete comic dialog**: The confirmation dialog shown before a comic is deleted.

## Comic editor screen

### Main areas

- **Editor top bar**: The top bar containing the Back button, Comic title field, and Editor
  overflow menu.
- **Comic title field** (`EditableTitle`): The editable comic name in the Editor top bar.
- **Editor toolbar**: The row below the Editor top bar containing the Step switch and file
  actions.
- **Canvas**: The editing area that displays the Page. The Editor toolbar and Comic kit are
  outside the Canvas.
- **Page**: The fixed surface a comic is drawn on. One comic is exactly one page.
- **Page shape**: The page's proportions: square, portrait, landscape, or strip.
- **Comic kit** (`ComicKit`): The collapsible bottom area containing balloon creation and
  text controls. Aliases: **balloon buttons area**, **bottom bar**

### Steps

Comic creation is divided into three steps. Each step edits a different part of the comic, and
the parts owned by later steps are shown dimmed and are not interactive.

- **Layout step**: The step for choosing the Page shape and dividing the Page into panels.
- **Placement step**: The step for choosing each panel's image and adjusting it in its frame.
- **Balloon step**: The step for adding and editing balloons.
- **Step switch**: The persistent control that moves between the three steps. It replaces the
  former Edit / View mode toggle.
- **Preview**: The comic shown without any editing affordances.

### Editor toolbar controls

- **Comic style button**: Opens the Comic style controls.
- **Focus panel button**: Focuses the selected panel, or the first panel when none is selected.
- **Show all panels button**: Leaves the focused view and displays the whole Page.
- **Save button**: Exports the comic as a PNG selected by the user.
- **Undo button**: Reverses the most recent edit, in any step.

### Layout step

- **Grid**: The rows and columns a page is divided into. A single panel is a 1×1 grid and a
  strip is a 1×n or n×1 grid.
- **Cell**: One row-and-column position in the Grid.
- **Grid line**: A draggable divider that changes row or column proportions.
- **Merged panel**: One panel spanning a rectangular block of cells.
- **Cut**: A straight line traced over the Page that splits the regions it crosses into separate
  panels. A cut may be horizontal, vertical, or diagonal at any angle.
- **Page cut**: A Cut that crosses the whole Page and splits every panel in its path.
- **Panel cut**: A Cut that crosses a single panel and splits only that one.
- **Layout preset**: A starting layout offered to the user, such as single panel, strip, grid, or
  blank page.
- **Layout change warning**: The confirmation shown before a layout change that would delete
  panels along with their images and balloons.

### Comic style

Comic-level settings that restyle the whole comic at once.

- **Page margin**: Space between the page edge and the outermost panels.
- **Gutter**: Space between adjacent panels.
- **Border thickness**: The panel outline weight.
- **Corner radius**: How rounded panel corners are.

### Placement step

- **Panel image**: The image placed in a panel.
- **Panel image transform**: How a Panel image sits inside its panel: its centre, zoom, and
  rotation angle.
- **Empty panel**: A panel with no Panel image yet.
- **Cover**: The rule that a Panel image always fills its panel completely. Zoom, pan, and
  rotation are constrained so no empty area can appear.
- **Swap**: Exchange the images of two panels by dragging one onto the other.

### Canvas concepts

- **Panel**: One region of the Page, produced by the Grid and the Cuts. A panel is not a Comic
  card or generic visual container.
- **Selected panel**: The panel currently showing editing controls.
- **Focused panel**: The panel temporarily filling the Canvas for closer work, available in the
  Placement and Balloon steps. Selection and focus are distinct states, and focus is a view
  state that is never saved with the comic.
- **Focus navigation buttons** (`FocusNavigation`): Directional edge buttons used to move between
  panels while a panel is focused.
- **Balloon**: A speech, thought, whisper, yell, or caption shape placed over the comic.
- **Selected balloon**: The balloon currently showing balloon editing controls.
- **Balloon scope**: Whether a balloon belongs to one panel or to the whole comic.
- **Panel balloon**: A balloon belonging to one panel. It is clipped to that panel, moves with
  it, and is deleted with it. This is the default scope.
- **Comic balloon**: A balloon belonging to the Page. It draws above every panel, may cross panel
  boundaries and gutters, and is unaffected by layout changes.

### Balloon controls

- **Balloon type buttons** (`BalloonTypeButton`): The Comic kit controls that add each
  balloon type.
- **Comic kit toggle**: Expands or collapses the Comic kit.
- **Balloon scope toggle**: Switches the selected balloon between Panel balloon and Comic
  balloon.
- **Font selector**: Selects the typeface of the selected balloon's text.
- **Text size slider**: Changes the selected balloon's text size in manual sizing mode.
- **Shape slider** (`ShapeSlider`): Changes the selected speech or whisper balloon's
  roundness.
- **Balloon move handle**: Moves the selected balloon.
- **Balloon resize handles**: Change the selected balloon's width and height.
- **Tail handle**: Changes a balloon tail's direction and length.
- **Tail-width handle**: Changes the width of a balloon tail.
- **Balloon delete button**: Removes the selected balloon.

## Settings screen

- **Settings section**: A labeled group of related Settings rows, such as General or Legal.
- **Settings row** (`SettingsRow`): A tappable navigation row within a Settings section.
- **Text settings dialog** (`TextSettingsDialog`): Controls the default font, visibility of
  the Font selector, and manual or automatic text sizing.
- **Layout settings dialog** (`LayoutSettingsDialog`): Controls the number of layout columns.
- **Information dialog** (`InformationDialog`): Displays About, Privacy, or Terms content.

## Interaction language

- **Open**: Navigate to a screen or reveal a menu or dialog.
- **Select**: Make a panel or balloon the current editing target.
- **Focus**: Temporarily show one panel as the Canvas viewport.
- **Trace**: Draw a Cut across the Page or across one panel.
- **Merge**: Combine adjacent panels into one Merged panel. **Unmerge** reverses it.
- **Place**: Choose the image that fills a panel.
- **Move**: Reposition an item without changing its size.
- **Resize**: Change an item's outer bounds.
- **Zoom** and **Pan**: Change a Panel image's scale and position inside its panel.
- **Rotate**: Turn a Panel image inside its panel, at any angle, snapping near quarter turns. It
  never changes the panel's shape or the page layout.
- **Add**: Create a new comic, cut, or balloon.
- **Delete**: Permanently remove an item, including any required confirmation.

Panels are not added or deleted directly. They are produced by the Layout step, so the ways to
change how many there are is to change the Grid, merge, unmerge, or add and remove Cuts.

When a request uses an approximate name, map it to the closest canonical term. Ask for
clarification only when multiple terms would lead to materially different behavior.