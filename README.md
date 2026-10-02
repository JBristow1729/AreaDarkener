# Darkener

Darkener is a RuneLite external plugin that darkens bright terrain and scenery colors to reduce glare.

It uses the same map-load recolouring approach as the original Huey Darkener build and the Dark Wintertodt & GWD plugin, but exposes separate settings for each bright area. Each supported area has a checkbox and its own strength slider from `0` to `100`.

Supported areas:

- Hueycoatl
- Wintertodt
- God Wars Dungeon
- Vorkath
- Fremennik Hunter Area, including the snowy sapphire glacialis area near DKS
- Penguin Agility Course
- Weiss
- Phantom Muspah
- Asgarnian Ice Dungeon

There is also a `Custom regions` setting. Add comma-separated region IDs there if you find another bright area before the plugin has a named option for it. RuneLite's `Region ID` plugin can show the current region ID in-game.

The default darkness strength is `45` for every supported area.

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
