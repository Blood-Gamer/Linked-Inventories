# Shared Inventory

Fabric `1.21.1` mod that makes all players on a server use the same survival inventory.

## What it does

- Main inventory contents are shared by every player.
- Each player keeps their own open GUI state and their own selected hotbar slot.
- Shared inventory contents are saved with the world.
- When one player changes the inventory, everyone else sees the update.
- If one player dies and drops inventory, the shared items are lost for everyone.

## Build

```bash
./gradlew build
```

The built mod jar will be in `build/libs/`.

## Install

Put the remapped jar from `build/libs/` into the `mods` folder on your Fabric `1.21.1` server and clients.
