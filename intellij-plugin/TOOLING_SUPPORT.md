# IntelliJ Plugin — Tooling Support

Status of the inventory preview feature in the `intellij-plugin` module. The preview is built on
**static analysis** (PSI/UAST) of the source file being edited — it never compiles or runs any of
the user's code, so anything that depends on runtime state can only ever be approximated.

## Supported

**Detection** (`ViewDetector`)
- Classes extending `me.devnatan.inventoryframework.View`.
- Inline view chains rooted at `Views.rows(...)` / `Views.type(...)` / `Views.builder()`.
- Works across both Java and Kotlin source files (via UAST).
- Dumb-mode safe: returns no match while the project is still indexing instead of throwing, and
  the initial preview extraction retries automatically once indexing finishes.

**Editor integration**
- Single editor tab per file (the plain text editor is hidden, not duplicated) with a split
  view: source on the left, preview on the right. The standard top-right icons switch between
  editor-only / split / preview-only.
- Live refresh: re-extracts and repaints ~300ms after each edit to the open file (debounced).

**Config extraction** (`ViewExtractor`)
- `ViewType` from `.type(ViewType.X)` or the `Views.rows(n)` shorthand (implies `CHEST`).
- Size from `.size(n)`/`Views.rows(n)`.
- Title from `.title("literal string")`.
- `layout(...)` char grid.
- Per-type slot geometry (rows/columns/max slot count) for `CHEST`, `HOPPER`, `DROPPER`,
  `DISPENSER`, `FURNACE`, `BLAST_FURNACE`, `CRAFTING_TABLE`, `BREWING_STAND`, `BEACON`, `ANVIL`,
  `SHULKER_BOX`, `SMOKER`, `VILLAGER_TRADING`, `PLAYER`. Irregular types (e.g. furnace's 2x2 grid
  with only 3 real slots) are trimmed to their real slot count instead of showing a phantom slot.

**Item extraction** (`ItemExtractor`)
- `withItem(new ItemStack(Material.X))`, resolved directly or through a local variable/parameter
  initializer, for: `slot(index, item)`, `slot(index).withItem(item)`, `firstSlot(item)`,
  `lastSlot(item)`, `layoutSlot(char, item)` (both the direct and chained-builder forms).
- `row(n)` / `firstRow()` / `lastRow()` / `column(n)` / `firstColumn()` / `lastColumn()`, both the
  chained (`.withItem(item)`) and `BiConsumer` factory (`(pos, slot) -> slot.withItem(item)`)
  forms — see limitations below for the heuristic these rely on.
- `renderWith(...)` / `onRender(...)` render as a distinct "dynamic" marker rather than being
  evaluated.

**Rendering**
- `CHEST`-type views render on top of bundled sprite frames (`resources/assets/sprites/chest-1.png`
  through `chest-6.png`, one per row count) instead of hand-drawn rectangles, scaled 2x with
  nearest-neighbor interpolation to keep the pixel art crisp. Slot content (material color+label,
  dynamic marker, layout fill) is overlaid at the sprite's real slot positions.
- Every other view type still renders as a plain drawn grid — there's no sprite for them.
- **Real item icons** (`ItemIconProvider`), opportunistically: if the machine running the IDE has
  a vanilla Minecraft client installed, icons are read directly from that client jar's
  `assets/minecraft/textures/{item,block}/<material>.png` at render time — nothing is bundled or
  redistributed by the plugin itself, since Mojang's usage guidelines prohibit that for
  third-party tools. Animated textures are cropped to their first frame and anything above 16x16
  is downscaled. If a material has no same-named texture file (e.g. a stained glass pane's icon is
  really just its plain glass block's texture), the reference is read out of the item's own
  `assets/minecraft/models/item/<material>.json` instead - one model deep, without following
  parent chains or resolving `"#variable"` texture substitution, so composite block-shaped items
  whose icon only inherits a texture from a parent model (fences, walls, carpets, stairs, ...)
  still fall back to the placeholder. When no client jar can be found, slots fall back to the
  original colored square + 3-letter material abbreviation. (The bundled chest frame sprites are
  original/generic art, not extracted Mojang textures, so they don't carry the same restriction and
  are unaffected either way.) The `.minecraft` directory used for auto-detection can be overridden
  per-machine in **Settings > Tools > Inventory Framework** (`MinecraftIconSettings`), for setups
  the platform default guess can't find (portable/custom launchers, an install on another drive,
  etc.).

## Known limitations / not supported

- **Nothing dynamic is ever evaluated.** Non-literal titles, `renderWith`/`onRender` lambdas,
  `displayIf`/state-driven conditions, and any item expression that isn't a literal
  `new ItemStack(Material.X)` (directly or via a simple local variable) all show as a generic
  placeholder rather than their real runtime value. This is a static-analysis tool, not a
  sandboxed execution environment.
- **`row()`/`column()`/`firstRow()`/`lastRow()`/`firstColumn()`/`lastColumn()` use a "fill the
  whole row/column" heuristic.** These APIs actually mean "next available slot in that
  row/column," and their real effect depends on how many times they're called (typically in a
  loop) and what else has already filled slots. There's no loop-iteration analysis, so every call
  site is treated as if it fills the entire row/column. This matches the common idiom (see
  `RowColumnSample.java`) but is wrong for genuine single-slot usage.
- **`availableSlot()` and `resultSlot()` are not placed.** Their position isn't statically knowable
  (or, for `resultSlot()`, varies per `ViewType`), so items placed through them don't appear in the
  preview at all.
- **Extraction isn't scoped to a single view's methods.** The extractor walks the whole file's UAST
  tree for recognizable config/item calls rather than precisely following `onInit`/`onFirstRender`
  boundaries. In practice this only matters for files with multiple unrelated views or stray calls
  to similarly-named methods.
- **Java and Kotlin only** (via UAST) — matches the languages the framework itself targets.
- **No interaction simulation.** The preview is read-only; `onClick`/`onSlotClick` behavior is
  never simulated.
- **Not published anywhere.** Development/testing only, via `./gradlew :intellij-plugin:runIde`.
  There's no JetBrains Marketplace listing.
- **Build constraint:** the IntelliJ Platform Gradle Plugin is pinned to 2.11.0 because 2.12+
  requires Gradle 9.0.0, and this repo's wrapper is on Gradle 8.14.5.
