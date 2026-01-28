# Mystcraft Datapack Showcase (Tech Demo)

This is a **working** datapack that demonstrates the Mystcraft symbol + grammar datapack system.
It is intended as a technical showcase, not for real gameplay balance.

## Install
1. Copy the `docs/datapacks/mystcraft_showcase` folder into your world `datapacks/` directory.
2. Start the world and run `/reload`.
3. You should see the pack loaded in `/datapack list`.

## What It Demonstrates
- Custom symbols with **all symbol categories**.
- All current **symbol logic types** (booleans, ints, floats, strings, ore controls, flags, terrain alterations, populators).
- Custom **grammar rules** and **custom grammar tokens**.
- A **symbol override** using `replace: true`.

## Namespaces
This pack uses two namespaces:
- `example:` for demo symbols and grammar rules.
- `mystcraft:` for overriding an existing Mystcraft symbol (`test_override`).

## Files
Symbols are in:
- `data/example/mystcraft/symbols/*.json`
- `data/mystcraft/mystcraft/symbols/test_override.json` (override example)

Grammar rules are in:
- `data/example/mystcraft/grammar/showcase_rules.json`

## Symbol Index
Each entry is a symbol you can reference by ID (namespace:path):

- `example:terrain_showcase` (terrain + secondary/mix + terrain block)
- `example:biome_controller_showcase` (biome controller)
- `example:biome_showcase` (adds biome)
- `example:sun_showcase`
- `example:moon_showcase`
- `example:stars_showcase` (stars + star type)
- `example:weather_showcase`
- `example:lighting_showcase`
- `example:color_showcase` (push color + gradient)
- `example:visual_showcase` (sky/fog/grass/foliage/water/cloud/horizon/night/sunset + heights)
- `example:environment_showcase` (timescale + flags + instability)
- `example:feature_large_showcase` (register terrain alteration: caves)
- `example:feature_medium_showcase` (register populator: spikes)
- `example:feature_small_showcase` (register populator: dripstone caves)
- `example:structure_showcase` (register populator: villages)
- `example:angle_showcase` (custom grammar token + angle)
- `example:phase_showcase` (phase)
- `example:length_showcase` (length)
- `example:sea_showcase` (sea block + sea level)
- `example:modifier_showcase` (flags + disallow random)
- `example:special_showcase` (star fissure + populator)
- `example:ore_showcase` (ore multiplier + disable single ore + enable all)
- `example:grammar_disabled_showcase` (grammar disabled)

Override example:
- `mystcraft:test_override` (replaces the built-in test symbol and flips a flag)

## Grammar Demo
The grammar file adds a rule so the custom token `example:angle_token` can expand into:
- `example:angle_showcase`
- `example:phase_showcase`
- `example:length_showcase`

This demonstrates custom grammar tokens + rules in datapacks.
