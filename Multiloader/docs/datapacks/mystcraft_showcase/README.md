# Mystcraft Datapack Showcase (Tech Demo)

This is a **working** datapack that demonstrates the Mystcraft symbol + grammar datapack system.
It is intended as a technical showcase, not for real gameplay balance.

## Install
1. Copy `docs/datapacks/mystcraft_showcase` into your world `datapacks/` directory.
2. Start the world and run `/reload`.
3. Verify it is loaded with `/datapack list`.

## What It Demonstrates
- All symbol **categories** and most logic types.
- Newer datapack capabilities: `display_name`, `allow_random`, `can_duplicate`.
- Weighted secondary terrain via `set_secondary_terrain_weighted`.
- Stack-based color application via `set_color_from_stack`.
- Datapack-defined biome controllers via `register_biome_controller`.
- The new random weather controller (`weather_random`).
- Custom grammar tokens + rules.
- A symbol override using `replace: true`.

## Namespaces
- `example:` demo symbols + grammar rules.
- `mystcraft:` an override example for `test_override`.

## Files
Symbols:
- `data/example/mystcraft/symbols/*.json`
- `data/mystcraft/mystcraft/symbols/test_override.json`

Grammar:
- `data/example/mystcraft/grammar/showcase_rules.json`

## Symbol Index (IDs)
- `example:terrain_showcase` (terrain mix + weighted secondary terrain)
- `example:biome_controller_showcase` (register biome controller)
- `example:biome_showcase` (add biome + average ground level)
- `example:sun_showcase`
- `example:moon_showcase`
- `example:stars_showcase` (star type)
- `example:weather_showcase` (random weather)
- `example:lighting_showcase`
- `example:visual_showcase` (color stack + cloud/horizon heights)
- `example:color_showcase` (grass/foliage/water colors, can_duplicate)
- `example:environment_showcase` (timescale + lightning/meteors flags)
- `example:feature_large_showcase` (floating islands terrain alteration)
- `example:feature_medium_showcase` (spikes populator)
- `example:feature_small_showcase` (dripstone caves populator)
- `example:structure_showcase` (villages + ruined portals)
- `example:angle_showcase` (custom grammar token + angle)
- `example:phase_showcase` (phase)
- `example:length_showcase` (length)
- `example:sea_showcase` (sea block + sea level)
- `example:modifier_showcase` (terrain block + horizon hidden)
- `example:special_showcase` (star fissure populator)
- `example:ore_showcase` (ore multiplier + disable ore)
- `example:grammar_disabled_showcase` (grammar disabled)

Override example:
- `mystcraft:test_override` (replaces the built-in test symbol)

## Grammar Demo
The grammar file adds a rule so the custom token `example:angle_token` can expand into:
- `example:angle_showcase`
- `example:phase_showcase`
- `example:length_showcase`

## Notes
- All demo symbols set `allow_random: false` so they do not pollute random book generation.
- `register_biome_controller` uses the `controller` field (e.g., `grid`, `tiled`, `huge`, `tiny`).

## Quick Reference (logic type → fields)
Strings:
- `set_terrain_type` → `value`
- `set_terrain_mix_mode` → `value`
- `set_secondary_terrain_type` → `value`
- `set_biome_controller` → `value`
- `set_weather_type` → `value`
- `set_lighting_type` → `value`
- `set_star_type` → `value`

Ints:
- `set_average_ground_level` → `value`
- `set_sea_level` → `value`
- `set_sky_color` → `value`, `natural` (optional)
- `set_fog_color` → `value`, `natural` (optional)
- `set_grass_color` → `value`, `natural` (optional)
- `set_foliage_color` → `value`, `natural` (optional)
- `set_water_color` → `value`, `natural` (optional)
- `set_cloud_color` → `value`, `natural` (optional)
- `set_horizon_color` → `value`, `natural` (optional)
- `set_night_sky_color` → `value`
- `set_sunset_color` → `value`
- `push_color` → `value`
- `push_gradient` → `value`

Floats:
- `set_cloud_height` → `value`
- `set_horizon_height` → `value`
- `set_timescale` → `value`
- `push_angle` → `value`
- `push_phase` → `value`
- `push_length` → `value`

Booleans:
- `set_has_sea` → `value`
- `set_sun_visible` → `value`
- `set_moon_visible` → `value`
- `set_stars_visible` → `value`

Special types:
- `set_flag` → `flag`, `value`
- `set_terrain_block` → `block`
- `set_sea_block` → `block`
- `register_populator` → `id`, `params` (optional)
- `register_terrain_alteration` → `id`, `params` (optional)
- `register_biome_controller` → `controller` (or `controller_type` / `biome_controller`)
- `add_biome` → `biome`
- `set_ore_disabled` → `ore`, `disabled`
- `set_ores_disabled` → `disabled`
- `set_ore_multiplier` → `ore`, `multiplier`
- `set_secondary_terrain_weighted` → `options`, `weights` (optional), `salt` (optional)
- `set_color_from_stack` → `target`, `palette`, `random_instability` (optional), `salt` (optional)

Allowed values:
- Terrain types (`set_terrain_type` / `set_secondary_terrain_type`): `normal`, `amplified`, `flat`, `void`, `cave`, `skylands`, `end`, `nether`, `checkerboard`, `blend`, `stripes`
- Terrain mix modes (`set_terrain_mix_mode`): `blend`, `checkerboard`, `stripes`, `none`
- Biome controllers (`register_biome_controller`): `single`, `native`, `tiny`, `small`, `medium`, `large`, `huge`, `tiled`, `grid`
- Weather types (`set_weather_type`): `normal`, `off`, `always`, `rain`, `snow`, `storm`, `thunder`, `cloudy`, `fast`, `slow`, `blizzard`, `random`
- Lighting types (`set_lighting_type`): `normal`, `bright`, `dark`, `nether`
- Star types (`set_star_type`): `sparse`, `dense`, `normal`
- Color targets (`set_color_from_stack`): `sky`, `fog`, `grass`, `foliage`, `water`, `cloud`, `horizon`, `sunset`, `night_sky`
- Color palettes (`set_color_from_stack`): `vibrant`, `sky`, `fog`, `grass`, `foliage`, `water`, `sunset`, `night_sky_random`

Known IDs:
- Terrain alterations (`register_terrain_alteration`):
  - `mystcraft:caves`, `mystcraft:ravines`, `mystcraft:floating_islands`
- Populators (`register_populator`):
  - Features: `mystcraft:huge_trees`, `mystcraft:deep_lakes`, `mystcraft:surface_lakes`, `mystcraft:spikes`,
    `mystcraft:spheres`, `mystcraft:tendrils`, `mystcraft:vertical_tendrils`, `mystcraft:perlin_worms`,
    `mystcraft:dripstone_caves`, `mystcraft:lush_caves`, `mystcraft:deep_dark`, `mystcraft:star_fissure`,
    `mystcraft:dense_ores`, `mystcraft:single_ore`, `mystcraft:block_shuffle_terrain`, `mystcraft:block_shuffle_global`,
    `mystcraft:standard_ores`, `mystcraft:biome_decoration`
  - Structures: `mystcraft:villages`, `mystcraft:dungeons`, `mystcraft:mineshafts`, `mystcraft:strongholds`,
    `mystcraft:nether_fortress`, `mystcraft:pillager_outposts`, `mystcraft:ruined_portals`, `mystcraft:ocean_monuments`,
    `mystcraft:witch_huts`, `mystcraft:desert_temples`, `mystcraft:jungle_temples`, `mystcraft:woodland_mansions`,
    `mystcraft:trail_ruins`, `mystcraft:ancient_cities`, `mystcraft:bastion_remnants`, `mystcraft:end_cities`,
    `mystcraft:igloos`, `mystcraft:shipwrecks`, `mystcraft:ocean_ruins`, `mystcraft:buried_treasure`,
    `mystcraft:nether_fossils`

Params reference (selected IDs):
- `mystcraft:floating_islands` (terrain alteration)
  - `style`: `mixed`, `skylands`, `archipelago`, `shards`, `ruins`
  - `density`: number (lower = more islands)
  - `structure_block`: block id (default: `minecraft:stone`)
  - `surface_block`: block id (default: `minecraft:grass_block`)
- `mystcraft:caves` (terrain alteration)
  - `rate`: number
  - `size`: number
  - `fill_block`: block id (default: `minecraft:air`)
- `mystcraft:ravines` (terrain alteration)
  - `rate`: number
  - `fill_block`: block id (default: `minecraft:air`)
- `mystcraft:single_ore` (populator)
  - `ore_block`: block id
  - `deepslate_block`: block id (optional)
  - `vein_size`: number
  - `veins_per_chunk`: number
  - `min_y`: number
  - `max_y`: number
  - `identifier`: string (unique id)

## Usage Cookbook (copy/paste snippets)

### Symbol Skeleton
```json
{
  "category": "terrain",
  "display_name": "My Custom Symbol",
  "card_rank": 2,
  "instability_cost": 1.0,
  "allow_random": true,
  "can_duplicate": false,
  "poem": ["Word", "Word", "Word", "Word"],
  "grammar": true,
  "logic": []
}
```

### Grammar Options
Disable grammar for a symbol:
```json
"grammar": false
```

Custom token + optional rank:
```json
"grammar": { "token": "example:my_token", "rank": 2 }
```

Grammar rules file (`data/<ns>/mystcraft/grammar/*.json`):
```json
{
  "replace": false,
  "rules": [
    {
      "parent": "example:my_token",
      "children": [
        "example:my_symbol_a",
        "example:my_symbol_b"
      ],
      "rank": 1
    }
  ]
}
```

### Core Terrain + Mixing
```json
{ "type": "set_terrain_type", "value": "amplified" }
```

```json
{ "type": "set_terrain_mix_mode", "value": "blend" }
```

Weighted secondary terrain (randomly picks one):
```json
{
  "type": "set_secondary_terrain_weighted",
  "options": [
    "mystcraft:terrain_checkerboard",
    "mystcraft:terrain_blend",
    "mystcraft:terrain_stripes"
  ],
  "weights": [1, 1, 1],
  "salt": 1234
}
```

### Biome Control
Add a biome:
```json
{ "type": "add_biome", "biome": "minecraft:cherry_grove" }
```

Set average ground level:
```json
{ "type": "set_average_ground_level", "value": 92 }
```

Register a datapack biome controller:
```json
{ "type": "register_biome_controller", "controller": "grid" }
```

Supported controllers: `single`, `native`, `tiny`, `small`, `medium`, `large`, `huge`, `tiled`, `grid`.

### Weather / Lighting / Celestials
```json
{ "type": "set_weather_type", "value": "random" }
```

```json
{ "type": "set_lighting_type", "value": "bright" }
```

```json
{ "type": "set_sun_visible", "value": true }
```

```json
{ "type": "set_moon_visible", "value": true }
```

```json
{ "type": "set_stars_visible", "value": true }
```

```json
{ "type": "set_star_type", "value": "dense" }
```

### Colors / Visual Effects
Push a color onto the stack:
```json
{ "type": "push_color", "value": 12345678 }
```

Apply a stacked color to a target:
```json
{ "type": "set_color_from_stack", "target": "sky", "palette": "sky", "random_instability": 1.0 }
```

Targets: `sky`, `fog`, `grass`, `foliage`, `water`, `cloud`, `horizon`, `sunset`, `night_sky`.

Palettes: `vibrant`, `sky`, `fog`, `grass`, `foliage`, `water`, `sunset`, `night_sky_random`.

Heights:
```json
{ "type": "set_cloud_height", "value": 192.0 }
```
```json
{ "type": "set_horizon_height", "value": 64.0 }
```

### Flags (features / structures / specials)
```json
{ "type": "set_flag", "flag": "floating_islands_enabled", "value": true }
```

Examples: `caves_enabled`, `ravines_enabled`, `tendrils_enabled`, `vertical_tendrils_enabled`,
`spheres_enabled`, `perlin_worms_enabled`, `deep_dark_enabled`, `dripstone_caves_enabled`,
`lush_caves_enabled`, `huge_trees_enabled`, `spikes_enabled`, `surface_lakes_enabled`,
`deep_lakes_enabled`, `villages_enabled`, `strongholds_enabled`, `dungeons_enabled`,
`mineshafts_enabled`, `ruined_portals_enabled`, `ancient_cities_enabled`, `end_cities_enabled`,
`bastion_remnants_enabled`, `ocean_monuments_enabled`, `witch_huts_enabled`, `desert_temples_enabled`,
`jungle_temples_enabled`, `woodland_mansions_enabled`, `shipwrecks_enabled`, `ocean_ruins_enabled`,
`buried_treasure_enabled`, `igloos_enabled`, `trail_ruins_enabled`, `nether_fossils_enabled`,
`star_fissure_enabled`, `obelisks_enabled`, `rainbow_enabled`, `horizon_hidden`, `pvp_enabled`,
`accelerated_enabled`, `meteors_enabled`, `lightning_enabled`, `scorched_enabled`,
`explosions_enabled`, `dense_ores_enabled`, `skylands_enabled`, `floating_islands_enabled`.

### Terrain Alterations (large-scale)
```json
{
  "type": "register_terrain_alteration",
  "id": "mystcraft:floating_islands",
  "params": {
    "style": "skylands",
    "density": 8,
    "structure_block": "minecraft:stone",
    "surface_block": "minecraft:grass_block"
  }
}
```

### Populators (features / structures)
```json
{ "type": "register_populator", "id": "mystcraft:spikes" }
```

```json
{ "type": "register_populator", "id": "mystcraft:villages" }
```

### Sea Control
```json
{ "type": "set_has_sea", "value": true }
```

```json
{ "type": "set_sea_level", "value": 70 }
```

```json
{ "type": "set_sea_block", "block": "minecraft:lava" }
```

### Ores
Disable all ores:
```json
{ "type": "set_ores_disabled", "disabled": true }
```

Disable a specific ore type:
```json
{ "type": "set_ore_disabled", "ore": "coal", "disabled": true }
```

Multiply an ore type:
```json
{ "type": "set_ore_multiplier", "ore": "diamond", "multiplier": 2.5 }
```

Add extra ore veins (populator):
```json
{
  "type": "register_populator",
  "id": "mystcraft:single_ore",
  "params": {
    "ore_block": "minecraft:gold_ore",
    "deepslate_block": "minecraft:deepslate_gold_ore",
    "vein_size": 8,
    "veins_per_chunk": 4,
    "min_y": -64,
    "max_y": 64,
    "identifier": "extra_gold_ore"
  }
}
```

### Instability
```json
{ "type": "add_instability", "value": 3.0 }
```

### Override an Existing Symbol
```json
{
  "replace": true,
  "category": "special",
  "display_name": "Override Example",
  "logic": [
    { "type": "set_flag", "flag": "pvp_enabled", "value": false }
  ]
}
```
