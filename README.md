# WaypointPlus

Quality-of-life teleports plugin for Paper 1.21.x. Provides homes, warps, spawn, back, RTP, and TPA with warmups, cooldowns, and optional Vault economy costs. Safe RTP uses heightmaps and the WorldBorder API.

## Features
- Homes: set, delete, list, and teleport to named homes; per-player limits via permissions
- Warps: admin-defined global waypoints
- Spawn: set and teleport to server spawn
- Back: return to previous location (on death and teleports, configurable)
- RTP: safe random teleport within WorldBorder or configurable radius
- TPA: player-to-player teleport requests with expiry
- Teleport rules: warmups, cooldowns, cancel-on-move, and economy cost per action
- Economy: optional Vault integration for charging teleport costs

## Requirements
- Java 21 or newer
- Paper 1.21.x (api-version: 1.21)
- Optional: Vault + an economy plugin (if you enable costs)

## Install
1. Build the plugin (see Build below) or download the JAR from your builds.
2. Drop the JAR into your Paper server's `plugins/` directory.
3. Start or restart the server. Configuration and message files will be generated under `plugins/WaypointPlus/`.

## Build
```bash
mvn -q -e -DskipTests package
```
The built JAR will be in `target/waypointplus-1.0.0.jar`.

## Configuration
`src/main/resources/config.yml` controls warmups, cooldowns, and costs per feature, plus RTP and back behavior. Key sections:

- teleport.<action>:
  - `warmup` (seconds)
  - `cooldown` (seconds)
  - `cost` (double) – requires Vault if > 0
  - `cancel-on-move` (boolean)
- homes:
  - `default-limit` – overridden by permission `waypointplus.homes.<n>`
- rtp:
  - `use-worldborder`: prefer WorldBorder bounds
  - `radius`: used when not using WorldBorder
  - `max-attempts`: attempts to find a safe spot
  - `min-y`: minimum Y for landing
  - `safe-blocks-blacklist`: disallowed landing blocks (e.g., WATER, LAVA)
  - `use-heightmap-no-leaves`: avoid landing on leaves
- back:
  - `save-on-death`
  - `save-on-teleport` (stores /back before plugin teleports)
- economy:
  - `require-vault`: if true and Vault missing, plugin disables
  - `format`: fallback currency format when Vault absent

Messages are in `src/main/resources/messages.yml`. Prefix and all user-facing texts are configurable.

## Data Storage
- Homes: `plugins/WaypointPlus/data/homes/<uuid>.yml`
- Warps: `plugins/WaypointPlus/warps.yml`
- Spawn: `plugins/WaypointPlus/spawns.yml`
- Back locations: `plugins/WaypointPlus/data/playerdata.yml`

## Commands & Permissions
- `/home [name]` – `waypointplus.home.use`
- `/sethome [name]` – `waypointplus.home.set`
- `/delhome <name>` – `waypointplus.home.delete`
- `/homes` – `waypointplus.home.list`
- `/warp <name>` – `waypointplus.warp.use`
- `/setwarp <name>` – `waypointplus.warp.set`
- `/delwarp <name>` – `waypointplus.warp.delete`
- `/warps` – `waypointplus.warp.list`
- `/spawn` – `waypointplus.spawn`
- `/setspawn` – `waypointplus.setspawn`
- `/back` – `waypointplus.back`
- `/rtp [world]` – `waypointplus.rtp`
- `/tpa <player>` – `waypointplus.tpa.send`
- `/tpaccept` – `waypointplus.tpa.accept`
- `/tpdeny` – `waypointplus.tpa.deny`
- `/tpacancel` – `waypointplus.tpa.cancel`

Permission bundles:
- `waypointplus.*` – all features (default: op)
- `waypointplus.home.*` – all home-related (default: true)
- `waypointplus.warp.*` – all warp-related (default: op)
- `waypointplus.tpa.*` – all TPA-related (default: true)
- `waypointplus.homes.*` – grant per-player home limits via `waypointplus.homes.<n>`

## Safe RTP Details
- Uses `World#getHighestBlockAt(..., HeightMap.MOTION_BLOCKING_NO_LEAVES)` when `rtp.use-heightmap-no-leaves: true`.
- Prefers WorldBorder bounds when available; otherwise uses a configurable radius.
- Skips unsafe blocks and ensures air space for the player.

## Notes
- Async teleports are used to keep the server responsive.
- If `economy.require-vault: true` and no Vault provider is found, the plugin disables on startup.
