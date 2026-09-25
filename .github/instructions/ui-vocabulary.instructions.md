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
- **Editor toolbar** (`Toolbar`): The row below the Editor top bar containing panel and file
  actions plus the Mode toggle.
- **Canvas** (`Editor`): The dotted editing area containing the comic image, panels,
  balloons, and their handles. The Editor toolbar and Comic kit are outside the Canvas.
- **Comic kit** (`ComicKit`): The collapsible bottom area containing balloon creation and
  text controls. Aliases: **balloon buttons area**, **bottom bar**

### Editor toolbar controls

- **Focus panel button**: Focuses the selected panel, or the first panel when none is
  selected.
- **Show all panels button**: Leaves the focused-panel view and displays the whole comic.
- **Mode toggle** (`ModeToggle`): The segmented **Edit / View** control.
- **Edit mode**: The mode in which panels and balloons can be changed.
- **View mode**: The mode that shows the comic without editing controls.
- **Add panel button**: Opens image selection to add another image panel.
- **Save button**: Exports the comic as a PNG selected by the user.

### Canvas concepts

- **Panel**: One image region in the comic. A panel is not a Comic card or generic visual
  container.
- **Selected panel**: The panel currently showing panel editing controls.
- **Focused panel**: The panel temporarily filling the Canvas for closer viewing. Selection
  and focus are distinct states.
- **Balloon**: A speech, thought, whisper, yell, or caption shape placed over a panel.
- **Selected balloon**: The balloon currently showing balloon editing controls.
- **Focus navigation buttons** (`FocusNavigation`): Directional edge buttons used to move
  between panels while a panel is focused.

### Panel controls

- **Panel move handle** (`ImageMoveHandle`): Drags a panel to a new layout position.
- **Panel resize handle** (`ImageResizeHandle`): Changes a panel's outer bounds.
- **Panel crop handle** (`ImageCropHandle`): Changes the visible crop of a panel image.
- **Panel rotate handle** (`ImageRotateHandle`): Appears in the top-left corner after
  long-pressing the sole panel and rotates it by 90 degrees.
- **Panel delete button** (`ImageDeleteHandle`): Removes a panel after confirmation.
- **Add-panel edge button** (`ImageAddEdgeButton`): Adds a panel beside a specific edge of
  an existing panel.
- **Add panel dialog** (`ImagePositionDialog`): The dialog for choosing a new panel's
  position.
- **Panel position picker** (`ImagePositionPicker`): The draggable panel-layout preview
  inside the Add panel dialog.
- **Delete panel dialog**: The confirmation dialog shown before a panel is removed.
- **Image processing overlay** (`ImageProcessingOverlay`): The blocking progress overlay
  shown while panel images are being composed.

### Balloon controls

- **Balloon type buttons** (`BalloonTypeButton`): The Comic kit controls that add each
  balloon type.
- **Comic kit toggle**: Expands or collapses the Comic kit.
- **Undo button**: Reverses the most recent panel image edit.
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
- **Move**: Reposition an item without changing its size.
- **Resize**: Change an item's outer bounds.
- **Crop**: Change which part of an image is visible inside a panel.
- **Add**: Create a new comic, panel, or balloon.
- **Delete**: Permanently remove an item, including any required confirmation.

When a request uses an approximate name, map it to the closest canonical term. Ask for
clarification only when multiple terms would lead to materially different behavior.