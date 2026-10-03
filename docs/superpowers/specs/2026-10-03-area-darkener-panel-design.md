# Area Darkener panel redesign

## Goal

Rename the plugin to **Area Darkener** and move per-area management out of RuneLite's config screen into a dedicated right-side panel. The config screen should stay small, while the panel lets users add, rename, tune, enable, and remove darkened regions without code changes.

The redesign should preserve the current safe recolouring behavior: map colours are remapped during map load, model snapshots are restored before reloads, and non-`Model` renderables are not asked for models during `PreMapLoad`.

## User experience

The RuneLite config for the plugin contains only global/default settings:

- `Global Darken Enabled`: toggles darkening everywhere in the game when no specific area entry matches.
- `Global Darken Strength`: the strength used by global darkening.
- `Default Area Strength`: the starting strength assigned to new entries added from presets or the current region.

The plugin adds a right-side navigation button named **Area Darkener**. The panel has three sections.

### Areas

This section contains a curated list of known bright-area presets, sorted alphabetically. For now the presets are:

- Asgarnian Ice Dungeon: `11925`
- Fremennik Hunter Area: `10810`, `10811`
- God Wars Dungeon: `11345`, `11346`, `11347`, `11601`, `11602`, `11603`
- Hueycoatl: `5939`
- Penguin Agility Course: `10559`
- Phantom Muspah: `11330`
- Vorkath: `9023`
- Weiss: `11325`
- Wintertodt: `6461`, `6462`

Each preset has an `Add` button. Adding a preset creates a normal editable entry in `My Areas`, using the preset name and preset region IDs. After it is added, it is no longer special: the user can rename it, change its strength, toggle it, add or remove region IDs, or remove the whole entry.

A preset `Add` button is disabled when all of its region IDs are already present in `My Areas`. If some, but not all, of its IDs already exist, adding the preset should add only the missing IDs into the new entry and avoid duplicating IDs. If this edge case is awkward to message in the first implementation, it is acceptable to disable the preset when any of its region IDs already exists and show a short explanation.

### Current region

This section has one button: `Add current region`. It does not try to infer or display a human-readable area name. When clicked, it creates a new entry named `My Entry N`, where `N` is the next available positive integer, and sets the entry's region IDs to the player's current region ID. The entry starts enabled and uses `Default Area Strength`.

If the current region already exists in any `My Areas` entry, the button is disabled and the section shows `Region already added to "<entry name>"`.

If the player is not logged in or the current region cannot be determined, the button is disabled and the section shows a short unavailable message.

### My Areas

This section lists all saved entries. Each entry should be compact enough for RuneLite's narrow side panel and should behave like a small editable card rather than a strict table. Each card contains:

- Editable name.
- Region ID list.
- `+ current region` button for adding the player's current region ID to this entry.
- Editable darkness strength number, constrained to `0` through `100`.
- Enabled checkbox.
- Remove button for the whole entry.
- Remove control for individual region IDs.

The `+ current region` button is disabled if the current region is already present in any entry. It should show the same `Region already added to "<entry name>"` explanation so the user understands why it cannot be added.

Duplicate region IDs are not allowed across entries. This keeps the darkening rule deterministic and avoids confusing overlaps.

## Darkening behavior

For each tile, determine the tile's region ID and choose a darkness setting with this priority:

1. If the region ID belongs to an enabled `My Areas` entry, use that entry's strength.
2. Otherwise, if `Global Darken Enabled` is true, use `Global Darken Strength`.
3. Otherwise, do not recolour the tile.

Specific entries therefore override global darkening.

Changing global config, adding/removing entries, changing strength, toggling entries, or editing region IDs should clear the colour-map cache and trigger the same safe map reload path the plugin already uses.

## Data model and persistence

The plugin stores dynamic entries as JSON in RuneLite's `ConfigManager`, under the plugin config group.

A saved entry has this shape:

```json
{
  "name": "GWD",
  "regionIds": [11345, 11346, 11347, 11601, 11602, 11603],
  "darkness": 45,
  "enabled": true
}
```

The runtime model should clamp darkness values to `0..100`, ignore malformed or non-positive region IDs, and remove duplicate IDs when loading from JSON. If the saved JSON is missing, blank, or malformed, the plugin should fall back to seeded defaults from the curated bright-area presets so first-time users get the currently supported areas without using the panel first.

The JSON key should be hidden from the normal config UI if possible. If RuneLite requires it to exist as a config item, it should be named/described as internal data and positioned below the simple public settings.

## Code structure

Recommended production classes:

- `DarkAreaPreset`: curated preset name and region IDs.
- `DarkAreaEntry`: mutable saved user entry with name, region IDs, darkness, and enabled state.
- `DarkAreaEntryStore`: load/save JSON through `ConfigManager`, seed defaults, sanitize entries, prevent duplicates, and expose lookup helpers.
- `AreaDarkenerPanel`: RuneLite `PluginPanel` implementation for Areas, Current region, and My Areas.
- Small Swing components as needed for entry cards and ID chips/rows.

The existing plugin class remains responsible for RuneLite lifecycle events, map reloads, and recolouring. It should delegate area lookup to `DarkAreaEntryStore` rather than hard-coding config methods for every area.

The Java package may remain `com.hueydarkener` for now to avoid unnecessary Plugin Hub churn, but all user-facing names, README text, and metadata should say **Area Darkener**. A later cleanup can rename packages if desired.

## Testing

Automated tests should cover:

- Preset region mappings.
- Default seeding when no JSON exists.
- Loading valid JSON.
- Handling malformed JSON without crashing.
- Clamping strength values.
- Removing duplicate region IDs across entries.
- Preventing duplicate current-region additions.
- Lookup priority: enabled entry first, global fallback second, no match otherwise.
- The existing crash regression: non-`Model` renderables must not call `getModel()` during map load.

Manual test in RuneLite developer mode:

1. Launch with `.\gradlew.bat run` in PowerShell from the repository root.
2. Confirm the plugin appears as **Area Darkener**.
3. Open the side panel and confirm the curated presets appear alphabetically.
4. Add Wintertodt from presets and verify it appears in `My Areas` with IDs `6461, 6462`.
5. Stand in a supported area, add current region, rename the entry, change strength, toggle it, and remove an ID.
6. Confirm duplicate current-region add is disabled with `Region already added to "<entry name>"`.
7. Visit Hueycoatl or another bright area and confirm darkening applies and config changes reload safely without crashing.

## Open decisions

Use simple Java Swing controls first. A polished searchable preset list can be added once the basic panel works. With the current short preset list, a search field is useful but not required for the first implementation.

