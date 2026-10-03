# Area Darkener

A plugin for darkening any region in RuneScape. Created as an accessibility plugin but useful for all.

The normal RuneLite config stays small:

- `Global Darken` darkens every region that is not handled by one of your saved areas.
- `Global Darken Strength` controls the global fallback strength.

Area management happens in the plugin's right-side panel. The panel lets you add curated bright-area presets, add the region you are currently standing in, rename entries, add multiple region IDs to one entry, set per-entry strength, toggle entries, and remove entries or individual IDs.
New areas start with a darkness strength of `50`.

Curated presets currently include:

- Asgarnian Ice Dungeon
- Fremennik Hunter Area
- God Wars Dungeon
- Hueycoatl
- Penguin Agility Course
- Phantom Muspah
- Vorkath
- Weiss
- Wintertodt

If you find another bright area, stand there and use `Add current region` from the panel.

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
repository=https://github.com/JBristow1729/AreaDarkener.git
commit=<full 40-character commit hash>
```
