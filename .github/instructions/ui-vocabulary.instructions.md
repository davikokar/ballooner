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
- **Editor toolbar**: The band below the Editor top bar, on its own paper surface, containing the
  Step switch and — under a divider — the **Editor action row**.
- **Editor action row**: The row beneath the Step switch. The Undo button sits at its right end;
  the Step back button and Step next button sit at its left wherever there is somewhere to go.
- **Step next button**: The "Next" button at the left of the Editor action row. Moves to the step
  after this one, exactly as tapping that step does. Shown on the Preset picker, where it instead
  opens the active preset's options; while Preset options are open, where it also takes the
  preset; and in the Placement step. In the Balloon step, which has no step after it, it is
  labelled **Save** and exports the comic exactly as the Save button does.
- **Step back button**: The "Back" button beside it. Shown in the same places as the Step next
  button, and in the Balloon step, where it returns to the Placement step. In the Placement step
  it returns to the Preset options of the comic's current Layout kind, which is where the preset
  was chosen, rather than to the Preset picker. While Preset options are open it returns to the
  Preset picker *without choosing the preset*, throwing away everything the options changed, so
  the comic and its active preset are as they were before the options opened; the Breadcrumb does
  the same.
- **Workspace**: Everything below the Editor toolbar. It sits on a tinted ground so the toolbar
  reads as a separate surface above it rather than as the top of the Canvas.
- **Canvas**: The editing area that displays the Page. The Editor toolbar and Comic kit are
  outside the Canvas.
- **Page**: The fixed surface a comic is drawn on. One comic is exactly one page. The page is
  always one unit wide; its height is derived from the Reference panel and is never chosen
  directly, so "page shape" is not a term the user is shown.
- **Comic kit** (`ComicKit`): The collapsible bottom area containing balloon creation and
  text controls. Aliases: **balloon buttons area**, **bottom bar**

### Steps

Comic creation is divided into three steps. Each step edits a different part of the comic, and
the parts owned by later steps are shown dimmed and are not interactive.

- **Layout step**: The step for choosing the Layout kind, the Panel shape, and how the Page is
  divided into panels.
- **Placement step**: The step for choosing each panel's image and adjusting it in its frame.
- **Balloon step**: The step for adding and editing balloons. Its heading is "Add balloons".
- **Step switch**: The persistent control that moves between the three steps. It replaces the
  former Edit / View mode toggle. Drawn as a **pill switch**: a flat tinted track in which the
  active segment is lifted out in paper, rather than filled with colour.
- **Step heading** (`StepHeading`): The line naming the step, at the top of the Workspace:
  "Select panel preset", "Select and place images", "Add balloons", or the Breadcrumb while
  Preset options are open. Every step draws it the same way and in the same place, so moving
  between steps never moves it. Anything it carries at its end, such as the Hide handles button,
  comes and goes without shifting it.
- **Preview**: The comic shown without any editing affordances.

### Editor toolbar controls

- **Options button**: The gear at the right end of the Breadcrumb, so it sits on the same line as
  the heading it belongs to. Opens the Comic style controls in a bottom sheet. Offered only while
  Preset options are open — Single, Strip, Grid, or Custom — since that is the only place the
  comic is being styled as a whole. It is not on the Preset picker or in any other step.
- **Comic style button**: Opens the Comic style controls.
- **Save button**: Exports the comic as a PNG selected by the user.
- **Undo button**: Reverses the most recent edit, in any step. An icon at the right end of the
  Editor action row, faded out when there is nothing to undo. It is not shown on the Preset
  picker, which changes nothing until a preset is opened.

### Layout step

- **Preset picker**: The Layout step's landing view. A two-by-two grid of **Layout preset cards**
  under the heading "Select panel preset", filling the step in place of the Canvas.
- **Layout preset card**: One card in the Preset picker. Shows the arrangement it makes, its name,
  and a one-line description. The card matching the comic's current Layout kind is the **active
  preset** and carries an ACTIVE badge.
- **Layout kind**: The choice a Layout preset card stands for: **Single**, **Strip**, **Grid**, or
  **Custom**. Read back off the comic rather than stored.
- **Preset options**: The second view of the Layout step, opened by tapping a Layout preset card.
  It is a continuation of the Preset picker, not an overlay: same ground, no frame of its own.
  Preset picker, and left by the **Step next button**, the **Step back button**, or the
  **Breadcrumb**, which continues the picker's heading — "Select panel preset / Single" — with
  the leading part tappable to go back and the **Options button** at its far end.
- **Single panel shape**: The Single preset's options. Four **Shape tiles** — **Square**,
  **Ratio**, **Custom**, **Auto** — over a **Panel preview**. Its preview carries no Panel
  options button: the comic is one panel, so the Comic style is already its panel style.
- **Strip options**: The Strip preset's options. An **Orientation** pair of icon buttons and a
  **Panels** stepper, then the Shape tiles and the Panel preview.
- **Grid options**: The Grid preset's options. A **Rows** stepper and a **Columns** stepper, then
  the Shape tiles and the Panel preview. Cells are chosen by tapping them in the preview, and
  **Merge** / **Unmerge** float over it when the selection allows.
- **Custom options**: The Custom preset's options. Nothing to choose: the layout is drawn. A drag
  across a panel in the preview cuts that panel in two, and **Undo** floats over it.
- **Stepper**: A whole number nudged one at a time (− n +). It has a floor but no ceiling.
- **Shape tile**: One shape choice. Shows a miniature of the shape it makes, its name, and its
  numbers; the chosen one carries an ACTIVE badge. The Ratio tile carries a **rotate control**
  that turns the shape on its side (3:2 becomes 2:3).
- **Custom dimensions**: The Custom tile's two sliders, **Width (W)** and **Height (H)**, in whole
  units from 1 to 24. The ratio between them is the shape.
- **Panel preview**: The shape drawn on a drafting ground, showing the panel as it will really be
  rather than the number that was asked for. Once the panel has an image it is drawn inside the
  frame and can be pinched, dragged, and twisted. That is **looking, not placing**: nothing there
  is written to the comic, and the Placement step remains the only place an image is positioned.
  Its top-right corner carries the **Panel options button**.
- **Panel options button**: The gear in the top-right corner of the Panel preview, drawn as the
  Options button is because it opens the same kind of thing for one panel. Offered in **Strip**,
  **Grid**, and **Custom**, and dimmed until exactly one panel is chosen. Not offered in
  **Single**, where the comic is one panel and so styling it is styling the comic — the Options
  button already does that. Opens the **Panel flyout**.
- **Selected panel**: The one panel the Preset options are working on, which is the panel the
  Panel flyout restyles. Chosen by tapping a panel in the Panel preview, except in **Grid**,
  where it is the cell already picked out for merging.
- **Reference panel**: The panel that gives the Page its height, which is the first panel of the
  Grid. For a Single comic that is the only panel.
- **Panel shape**: The proportions the Reference panel is held at. Offered as **Square**, **2:3**,
  **3:2**, **Custom** (a ratio slider), and **Auto** (the shape of the image chosen in the
  Placement step).
- **Grid**: The rows and columns a page is divided into. A single panel is a 1×1 grid and a
  strip is a 1×n or n×1 grid.
- **Cell**: One row-and-column position in the Grid.
- **Grid line**: A draggable divider that changes row or column proportions.
- **Merged panel**: One panel spanning a rectangular block of cells.
- **Cut**: A straight line traced over the Page that splits the regions it crosses into separate
  panels. A cut may be horizontal, vertical, or diagonal at any angle. A cut whose two **cut
  handles** have both been dragged clear of the page divides nothing and is deleted, which is how
  a cut is removed.
- **Page cut**: A Cut that crosses the whole Page and splits every panel in its path.
- **Panel cut**: A Cut that crosses a single panel and splits only that one.
- **Layout preset**: A starting layout offered to the user, such as single panel, strip, grid, or
  blank page.
- **Layout change warning**: The confirmation shown before a layout change that would delete
  panels along with their images and balloons.

### Comic style

Comic-level settings that restyle the whole comic at once. Each is a fraction of the page width,
and each slider reads its value out as that percentage beside the track.

- **Gutter**: Space between adjacent panels, and the space round the outside of the page too.
- **Page margin**: The gap between the page edge and the outermost panels. It is the Gutter, not a
  setting of its own, so there is no margin control.
- **Border thickness**: The panel outline weight.
- **Corner radius**: How rounded panel corners are.

### Panel style

- **Panel flyout** (`PanelOptionsSheet`): The sheet the Panel options button opens from the
  bottom. It offers only **Border thickness** and **Corner radius**, and only for the Selected
  panel, overriding the Comic style for that panel alone. The Gutter is not here: it is the space
  between panels and so can never belong to one of them.
- **Panel frame**: What the Panel flyout sets — one panel's own border and corners. A panel has
  none until it is given one, and every panel loses the one it was given the moment any Comic
  style control moves, since restyling the comic as a whole has the last word.

### Placement step

- **Placement step heading**: "Select and place images", the step's Step heading.
- **Hide handles button**: The eye at the right of a step heading, offered only while a panel is
  focused. Takes the Panel handles off the panel so it can be seen whole, and puts them back. It
  stays behind when they are hidden, since nothing else could bring them back, and it forgets
  itself when the focused view is left.
- **Panel image**: The image placed in a panel.
- **Panel image transform**: How a Panel image sits inside its panel: its centre, zoom, and
  rotation angle.
- **Empty panel**: A panel with no Panel image yet.
- **Panel loading spinner**: The progress ring drawn in the middle of a panel while its picked
  image is still being copied into the app or decoded for display. It appears on every panel one
  trip to the picker is filling, not only the Selected panel.
- **Panel handles**: The round controls that appear on the Selected panel, so what can be done to
  a panel is offered on the panel itself. Which ones appear depends on the step and on what is in
  the panel. They sit the same distance inside the panel wherever it has room for them, on the
  first run across it wide enough to hold them rather than on the corners of the box around it,
  because a Cut can leave a panel a shape with no usable corners. A panel with handles along its
  bottom as well as its top measures those from the bottom in the same way.
  The Balloon step offers only the Expand button, since images belong to the Placement step.
- **Add image button**: The Panel handle at the left of an Empty panel. Opens the photo picker,
  which takes several images at once: the first fills the Selected panel and the rest fill the
  Empty panels that follow it, wrapping round the page.
- **Expand button**: The Panel handle at the left of a filled panel. Focuses the panel so it fills
  the Canvas, and gives the Canvas back when it is already focused. A comic of one panel is not
  offered it: that panel is already the whole page.
- **Remove image button**: The Panel handle at the right of a filled panel. Empties the panel
  without removing the panel itself.
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
- **Focus navigation buttons** (`FocusNavigation`): Round arrow handles along the bottom of the
  focused panel, used to move between panels. They are Panel handles like the others and sit the
  same distance inside the panel, so everything over the page reads as one family. Leaving focus
  is the Expand button's job, not theirs, and a comic of one panel has neither.
- **Balloon**: A speech, thought, whisper, yell, or caption shape placed over the comic.
- **Selected balloon**: The balloon currently showing balloon editing controls.
- **Balloon scope**: Whether a balloon belongs to one panel or to the whole comic.
- **Panel balloon**: A balloon belonging to one panel. It is clipped to that panel, moves with
  it, and is deleted with it. This is the default scope.
- **Comic balloon**: A balloon belonging to the Page. It draws above every panel, may cross panel
  boundaries and gutters, and is unaffected by layout changes.

### Balloon controls

- **Balloon type buttons** (`BalloonTypeBar`): The row of five controls under the Balloon step's
  Step heading that add each balloon type. Each is drawn as the balloon it makes, by the same
  renderer that draws the page, so a button cannot advertise something the comic does not do.
  A new balloon goes into the Selected panel.
- **Comic kit toggle**: Expands or collapses the Comic kit.
- **Balloon scope toggle**: The icon at the right end of the Balloon type buttons that switches
  the selected balloon between Panel balloon and Comic balloon. Greyed out when no balloon is
  selected, since there is nothing to say.
- **Balloon style flyout** (`BalloonStyleSheet`): The sheet the balloon's **Balloon style handle**
  opens from the bottom, holding everything about how that balloon is drawn. It is split into a
  **Text tab** and a **Balloon tab**, chosen with the same **pill switch** the Step switch uses so
  a tab reads the same wherever it is. All of it belongs to the one balloon.
- **Text tab**: Holds the Font selector and **autosize** checkbox on one line, then the Text size
  slider. Its labels sit in the same column the sliders put theirs in, so the tab reads down one
  edge.
- **Balloon tab**: Holds the Shape slider, the Border size slider, and the **match panel border**
  checkbox.
- **Auto size**: When set, the balloon sizes its own text to fill itself, and the Text size slider
  has nothing to choose.
- **Font selector**: A compact dropdown of the typefaces, each shown in its own letters. A new
  balloon is lettered in Anime Ace.
- **Text size slider**: Changes the selected balloon's text size, unless it is sizing itself.
  Like the other style sliders it is drawn as a plain filled bar with no thumb, which costs less
  height than Material's.
- **Shape slider** (`ShapeSlider`): Changes the selected speech or whisper balloon's
  roundness.
- **Border size slider**: Changes how thickly the selected balloon is outlined, unless it is
  matching the panel border.
- **Match panel border**: When set, the balloon is outlined at exactly the Border thickness the
  panels are, including none at all when the panels have no border, and the Border size slider has
  nothing to choose. A new balloon matches.
- **Balloon move handle**: The round handle in the middle of the selected balloon's top edge.
  Dragging it carries the balloon.
- **Balloon resize handle**: The round handle on its bottom-right corner, which changes its width
  and height.
- **Balloon delete handle**: The round handle on its top-right corner, which removes it.
- **Balloon style handle**: The round handle on its top-left corner, which opens the Balloon style
  flyout.
- **Tail handles**: Two solid blue dots on the selected balloon's tail: one at its tip, which aims
  and lengthens it, and a smaller one beside the tail half way down, which widens or narrows it.
  The width one is carried clear of the body on purpose — the balloon's words are typed into a
  field laid over the body, and that field takes any touch landing on it, so a handle against the
  body's edge cannot be grabbed. When both are under the finger the nearer one is taken, since a
  short tail puts them close together.
- **Balloon text entry**: The selected balloon's words are typed into the balloon itself, in the
  letters and at the size they will be read in. An empty balloon invites them with "Say
  something". There is no text box anywhere else.
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