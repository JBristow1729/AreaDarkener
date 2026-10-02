# Huey Darkener

Huey Darkener is a RuneLite external plugin that darkens The Hueycoatl arena terrain and scenery colors to reduce glare.

The Hueycoatl arena can be bright enough that floor hazards and movement cues are difficult to read. This plugin follows the same approach as the Dark Wintertodt plugin: it remaps packed HSL colors during map load, scoped to the Hueycoatl arena region, and exposes one `Darkness Strength` setting from `0` to `100`.

The default darkness strength is `45`.

## Run locally

```powershell
.\gradlew.bat run
```

## Test

```powershell
.\gradlew.bat test
```

## Build

```powershell
.\gradlew.bat shadowJar
```

## Plugin Hub

RuneLite Plugin Hub reviews source by pinning a public repository and commit hash. After pushing a release commit, add a marker file to a fork of `runelite/plugin-hub`:

```text
repository=https://github.com/JBristow1729/HueyDarkener.git
commit=<full 40-character commit hash>
```
