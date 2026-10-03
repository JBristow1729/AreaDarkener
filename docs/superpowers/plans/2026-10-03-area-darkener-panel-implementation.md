# Area Darkener Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Area Darkener right-side panel, dynamic area-entry storage, and simplified config described in the approved design.

**Architecture:** Move area selection from fixed config methods into a persisted entry store backed by RuneLite `ConfigManager`. The plugin asks the store for a darkness strength by region ID, while the Swing panel edits the store and triggers safe reloads through the plugin.

**Tech Stack:** Java 11, RuneLite client APIs, Swing, Gson through RuneLite/client dependency if available or ConfigManager typed serialization fallback, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-03-area-darkener-panel-design.md`

## Global Constraints

- User-facing name is **Area Darkener**.
- Normal RuneLite config contains only `Global Darken Enabled`, `Global Darken Strength`, and `Default Area Strength`.
- Side panel contains `Areas`, `Current region`, and `My Areas` sections.
- Do not call `Renderable#getModel()` on non-`Model` renderables during `PreMapLoad`.
- Duplicate region IDs are not allowed across entries.
- Darkness values are constrained to `0..100`.
- Specific enabled entries override global darkening.
- Java package may remain `com.hueydarkener`.

## Review Focus

- Malformed saved JSON: falls back to seeded defaults without crashing. Covered in Task 1.
- Duplicate region IDs across entries: first entry keeps the region, later duplicates are removed. Covered in Task 1.
- Current region unavailable: panel disables add buttons and shows unavailable text. Covered in Task 3 manual verification.
- Config changes and panel changes both reload the map safely. Covered in Task 2 and Task 3.
- Global darken can affect every region, but named entries still override it. Covered in Task 2.

---

## File Structure

- Create `src/main/java/com/hueydarkener/DarkAreaPreset.java`: curated preset enum/data with names and region IDs sorted for display.
- Create `src/main/java/com/hueydarkener/DarkAreaEntry.java`: mutable saved entry model.
- Create `src/main/java/com/hueydarkener/DarkAreaEntryStore.java`: persistence, sanitization, duplicate prevention, lookup helpers.
- Create `src/main/java/com/hueydarkener/AreaDarkenerPanel.java`: RuneLite side panel and Swing event wiring.
- Modify `src/main/java/com/hueydarkener/HueyDarkenerConfig.java`: reduce config to global/default fields and hidden persisted JSON if needed.
- Modify `src/main/java/com/hueydarkener/HueyDarkenerPlugin.java`: rename descriptor to Area Darkener, register navigation button/panel, delegate lookup to store.
- Modify `runelite-plugin.properties`, `README.md`, and tests for the new name and flow.
- Remove or replace `DarkArea.java` once presets/store own region mapping.

---

### Task 1: Dynamic area entry model and persistence

**Files:**
- Create: `src/main/java/com/hueydarkener/DarkAreaPreset.java`
- Create: `src/main/java/com/hueydarkener/DarkAreaEntry.java`
- Create: `src/main/java/com/hueydarkener/DarkAreaEntryStore.java`
- Modify: `src/main/java/com/hueydarkener/HueyDarkenerConfig.java`
- Test: `src/test/java/com/hueydarkener/DarkAreaEntryStoreTest.java`

**Interfaces:**
- Produces: `DarkAreaPreset.getPresets(): List<DarkAreaPreset>`, `getName(): String`, `getRegionIds(): List<Integer>`.
- Produces: `DarkAreaEntry(String name, List<Integer> regionIds, int darkness, boolean enabled)`, getters/setters, `containsRegion(int)`.
- Produces: `DarkAreaEntryStore(ConfigManager configManager, HueyDarkenerConfig config)`, `List<DarkAreaEntry> getEntries()`, `void saveEntries(List<DarkAreaEntry>)`, `Optional<DarkAreaEntry> findEntryContainingRegion(int)`, `OptionalInt findDarknessForRegion(int)`, `String nextEntryName()`, `boolean addCurrentRegionToEntry(DarkAreaEntry entry, int regionId)`, `DarkAreaEntry createCurrentRegionEntry(int regionId)`.
- Consumes: `HueyDarkenerConfig.defaultAreaStrength()` and persisted JSON key from config manager.

- [ ] **Step 1: Write failing store tests**

Add tests for:

```java
loadsSeededDefaultsWhenNoSavedJson()
loadsValidSavedJson()
malformedJsonFallsBackToSeededDefaults()
clampsDarknessToZeroThroughOneHundred()
removesDuplicateRegionsAcrossEntries()
findsEntryContainingRegion()
createsIncrementingMyEntryNames()
```

- [ ] **Step 2: Run store tests and verify failure**

Run: `.\gradlew.bat test --tests com.hueydarkener.DarkAreaEntryStoreTest`
Expected: fail because the new classes do not exist.

- [ ] **Step 3: Implement model/store**

Implement `DarkAreaPreset`, `DarkAreaEntry`, and `DarkAreaEntryStore`. Store JSON under `HueyDarkenerConfig.GROUP` key `areaEntries`. Use `ConfigManager#getConfiguration(group, key)` and `setConfiguration(group, key, value)`. If Gson is unavailable, use ConfigManager typed serialization with `List<DarkAreaEntry>`; otherwise use Gson explicitly.

- [ ] **Step 4: Reduce config interface**

Change `HueyDarkenerConfig` to expose:

```java
boolean globalDarkenEnabled();
int globalDarkenStrength();
int defaultAreaStrength();
```

Keep `GROUP = "areadarkener"`. Use `@Range(min = 0, max = 100)` for both strength fields.

- [ ] **Step 5: Run store tests and full tests**

Run: `.\gradlew.bat test`
Expected: tests pass after old fixed-area tests are updated or removed.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/hueydarkener src/test/java/com/hueydarkener
git commit -m "add area darkener entry store"
```

---

### Task 2: Plugin lookup and lifecycle integration

**Files:**
- Modify: `src/main/java/com/hueydarkener/HueyDarkenerPlugin.java`
- Modify: `src/test/java/com/hueydarkener/DarkenerConfigTest.java`
- Modify: `src/test/java/com/hueydarkener/HueyDarkenerPluginCrashTest.java`
- Test: `src/test/java/com/hueydarkener/AreaDarkenerLookupTest.java`

**Interfaces:**
- Consumes: `DarkAreaEntryStore.findDarknessForRegion(int)` from Task 1.
- Produces: plugin method `requestReload()` package-private for panel/store callbacks.
- Produces: plugin method `OptionalInt currentRegionId()` package-private for panel use.

- [ ] **Step 1: Write lookup tests**

Add tests proving:

```java
entryDarknessOverridesGlobalDarkness()
globalDarknessAppliesWhenNoEntryMatches()
disabledGlobalDarknessDoesNotApplyWithoutEntry()
nonModelRenderableDoesNotCallGetModelDuringMapLoad()
```

- [ ] **Step 2: Run lookup tests and verify failure**

Run: `.\gradlew.bat test --tests com.hueydarkener.AreaDarkenerLookupTest`
Expected: fail because plugin still uses fixed config methods.

- [ ] **Step 3: Refactor plugin lookup**

Rename user-facing descriptor to `Area Darkener`. Replace `settingsForRegion(int)` with logic that asks `DarkAreaEntryStore.findDarknessForRegion(regionId)`, then falls back to `config.globalDarkenEnabled() ? config.globalDarkenStrength() : empty`.

- [ ] **Step 4: Add reload API**

Expose package-private `void requestReload()` that clears `remappedHslByStrength` and sets `nextReloadTick = client.getTickCount() + 1`. Call it from config changes and later panel actions.

- [ ] **Step 5: Add current region API**

Expose package-private `OptionalInt currentRegionId()` using `client.getLocalPlayer()` and `WorldPoint.fromLocalInstance(client, localPlayer.getLocalLocation())` when logged in; return empty when unavailable.

- [ ] **Step 6: Run tests**

Run: `.\gradlew.bat test`
Expected: pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/hueydarkener/HueyDarkenerPlugin.java src/test/java/com/hueydarkener
git commit -m "wire area entries into darkening lookup"
```

---

### Task 3: Area Darkener side panel

**Files:**
- Create: `src/main/java/com/hueydarkener/AreaDarkenerPanel.java`
- Modify: `src/main/java/com/hueydarkener/HueyDarkenerPlugin.java`
- Modify: `README.md`
- Modify: `runelite-plugin.properties`
- Test: compile plus manual RuneLite verification.

**Interfaces:**
- Consumes: `DarkAreaPreset.getPresets()` from Task 1.
- Consumes: `DarkAreaEntryStore` methods from Task 1.
- Consumes: `HueyDarkenerPlugin.currentRegionId()` and `requestReload()` from Task 2.

- [ ] **Step 1: Implement panel class**

Create `AreaDarkenerPanel extends PluginPanel`. Constructor takes `DarkAreaEntryStore store`, `HueyDarkenerConfig config`, `Supplier<OptionalInt> currentRegionSupplier`, and `Runnable changedCallback`. Render sections `Areas`, `Current region`, and `My Areas`.

- [ ] **Step 2: Implement preset add behavior**

Preset `Add` creates an editable entry with preset name, missing preset IDs, default strength, and enabled true. Disable when any preset region is already in entries and show a tooltip or inline label explaining the existing entry name.

- [ ] **Step 3: Implement current region behavior**

`Add current region` creates `My Entry N` with the current region. Disable and show `Region already added to "<entry name>"` when duplicate, or unavailable text when no current region.

- [ ] **Step 4: Implement My Areas cards**

Each card supports editable name, editable/clamped strength, enabled checkbox, whole-entry remove, individual ID remove, and `+ current region` with duplicate prevention. After each edit, call store save, panel refresh, and reload callback.

- [ ] **Step 5: Register navigation button**

Inject `ClientToolbar` if available in RuneLite client API. In `startUp`, create panel and `NavigationButton.builder().tooltip("Area Darkener").panel(panel).priority(...)`. Use a simple generated `BufferedImage` icon if no resource icon exists. Add it to toolbar in startup and remove in shutdown.

- [ ] **Step 6: Update docs and metadata**

Set `runelite-plugin.properties` displayName to `Area Darkener`, update description/tags, and rewrite README usage around config plus side panel.

- [ ] **Step 7: Compile and test**

Run: `.\gradlew.bat test shadowJar`
Expected: build successful.

- [ ] **Step 8: Manual developer-mode verification**

Run: `.\gradlew.bat run`. Verify panel appears, presets are alphabetical, adding Wintertodt creates IDs `6461, 6462`, duplicate current-region add is disabled with the correct message, entry edits reload without crash, and Hueycoatl still darkens.

- [ ] **Step 9: Commit**

```bash
git add README.md runelite-plugin.properties src/main/java/com/hueydarkener src/test/java/com/hueydarkener
git commit -m "add area darkener side panel"
```

---

### Task 4: Final verification and branch preparation

**Files:**
- Modify only if final checks find an issue.

**Interfaces:**
- Consumes all previous tasks.

- [ ] **Step 1: Run full verification**

Run: `.\gradlew.bat test shadowJar`
Expected: build successful.

- [ ] **Step 2: Inspect git state**

Run: `git status --short --branch`
Expected: clean branch ahead of or equal to origin.

- [ ] **Step 3: Push branch**

Run: `git push`
Expected: `codex/generic-darkener` updates on GitHub.

- [ ] **Step 4: Report live-test steps**

Tell the user the exact commit hash and the minimal local test sequence before Plugin Hub submission.
