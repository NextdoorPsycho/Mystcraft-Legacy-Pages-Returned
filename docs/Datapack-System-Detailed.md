# Datapack System (Very Detailed)

This document shows a full, detailed example of the Mystcraft datapack system,
with emphasis on Symbols and Grammar.

Note: Paths below are relative to the datapack root.

---

## 1) Minimal Pack Layout

A functional datapack can be as small as:

- `pack.mcmeta`
- `data/mystcraft/mystcraft/symbols/<your_symbol>.json`

Example tree:

```
my_mystcraft_pack/
  pack.mcmeta
  data/
    mystcraft/
      mystcraft/
        symbols/
          terrain_scripted_example.json
```

### pack.mcmeta

```
{
  "pack": {
    "pack_format": 18,
    "description": "Mystcraft Datapack Example"
  }
}
```

---

## 2) Symbols: Full Reference Example

Symbols define a card/page that can be written into an Age.
A symbol can set flags, register generators, add instability, etc.

Below is a **fully annotated** symbol that registers a scripted terrain
generator
and uses grammar customization.

```
{
  "category": "terrain",
  "card_rank": 4,
  "instability_cost": 8.0,
  "display_name": "Custom Inverted World",

  "poem": [
    "Earth",
    "Reversed",
    "Falling",
    "Sky"
  ],

  "allow_random": true,
  "can_duplicate": false,

  "grammar": {
    "token": "mystcraft:terrain",
    "rank": 4
  },

  "logic": [
    {
      "type": "register_terrain_generator",
      "id": "mystcraft:scripted",
      "params": {
        "name": "inverted",
        "x_scale": 1.0,
        "y_scale": 1.0,
        "z_scale": 1.0,
        "seed_offset": 1337,

        "params": {
          "base_height": 64,
          "height_amp": 30
        },

        "density": {
          "op": "sub",
          "a": { "var": "y" },
          "b": {
            "op": "add",
            "a": { "var": "base_height" },
            "b": {
              "op": "mul",
              "a": { "op": "noise2", "scale": 0.01, "octaves": 5, "seed": 1337 },
              "b": { "var": "height_amp" }
            }
          }
        }
      }
    },

    {
      "type": "add_instability",
      "value": 8.0
    }
  ]
}
```

### Field-by-field explanation

- `category`: Symbol category (e.g., `terrain`, `feature_large`, `biome`, etc).
- `card_rank`: Rarity tier; used in trading and generation weighting.
- `instability_cost`: Base instability added by the symbol itself.
- `display_name`: Optional override for UI name.
- `poem`: 4-word flavor phrase shown in guidebook/pages.
- `allow_random`: Can appear in random ages.
- `can_duplicate`: Can appear more than once in the same age.
- `grammar`: Controls how the grammar system selects this symbol:
    - `token`: the grammar token this symbol binds to.
    - `rank`: weight rank for grammar selection.
- `logic`: List of actions executed when the symbol is applied.

---

## 3) Symbol Logic: Common Actions

These are the most useful `logic.type` entries. Each is a JSON object in
`logic`.

### Flags (feature toggles)

```
{ "type": "set_flag", "flag": "tendrils_enabled", "value": true }
```

Common flags include:

- `floating_islands_enabled`, `tendrils_enabled`, `spheres_enabled`
- `perlin_worms_enabled`, `spikes_enabled`, `crystals_enabled`
- `deep_lakes_enabled`, `surface_lakes_enabled`
- structure flags like `villages_enabled`, `strongholds_enabled`

### Register terrain alteration

```
{
  "type": "register_terrain_alteration",
  "id": "mystcraft:floating_islands",
  "params": { "style": "mixed", "density": 9 }
}
```

### Register populator

```
{
  "type": "register_populator",
  "id": "mystcraft:tendrils",
  "params": { "count": 1, "min_length": 35, "max_length": 90 }
}
```

### Register populator pool (random picks)

```
{
  "type": "register_populator_pool",
  "count": 3,
  "pool": [
    "mystcraft:spikes",
    "mystcraft:spheres",
    "mystcraft:tendrils"
  ]
}
```

### Register terrain generator (scripted)

```
{
  "type": "register_terrain_generator",
  "id": "mystcraft:scripted",
  "params": { "density": { "op": "sub", "a": 64, "b": {"var":"y"} } }
}
```

---

## 4) Grammar: How Random Ages Are Built

The grammar system uses tokens (like `mystcraft:terrain`) and rules to assemble
an age from symbol categories.

There are two components:

1) **Symbol grammar binding** (in the symbol JSON)
2) **Grammar rules** (datapack rules that define how tokens expand)

### 4.1 Symbol Grammar Binding

By default, symbols bind to grammar tokens based on their category.
You can override it with:

```
"grammar": {
  "token": "mystcraft:terrain",
  "rank": 4
}
```

You can also disable grammar binding entirely:

```
"grammar": false
```

### 4.2 Grammar Rules

Grammar rules live under:

```
data/mystcraft/mystcraft/grammar/
```

Each file defines rules for tokens.

**Example rule file**: `data/mystcraft/mystcraft/grammar/world.json`

```
{
  "rules": [
    {
      "token": "mystcraft:age",
      "productions": [
        ["mystcraft:terrain", "mystcraft:biome_controller", "mystcraft:weather"]
      ]
    },
    {
      "token": "mystcraft:terrain",
      "productions": [
        ["mystcraft:terrain"],
        ["mystcraft:terrain", "mystcraft:feature_large"]
      ]
    }
  ]
}
```

How to read it:

- `token`: the grammar token being expanded.
- `productions`: each is a list of tokens.
    - This example says an age is a terrain + biome controller + weather.
    - Terrain can optionally add a large feature.

### 4.3 Rank Weights

Each symbol has a `card_rank` and can optionally override its grammar rank.
This influences weighted selection when the grammar expands tokens.

Higher rank is rarer.

---

## 5) Full Pack Example (Symbols + Grammar)

```
my_mystcraft_pack/
  pack.mcmeta
  data/
    mystcraft/
      mystcraft/
        symbols/
          terrain_scripted_example.json
          terrain_scripted_hex.json
        grammar/
          world.json
```

### terrain_scripted_hex.json

```
{
  "category": "terrain",
  "card_rank": 4,
  "instability_cost": 6.0,
  "display_name": "Hex Tile Terrain",
  "logic": [
    {
      "type": "register_terrain_generator",
      "id": "mystcraft:scripted",
      "params": {
        "name": "hex_tiles",
        "density": {
          "op": "sub",
          "a": {
            "op": "add",
            "a": 60,
            "b": {
              "op": "mul",
              "a": { "op": "cell", "mode": "hex", "size": 48, "seed": 9001, "min": 0.0, "max": 1.0 },
              "b": 45
            }
          },
          "b": { "var": "y" }
        }
      }
    }
  ]
}
```

### world.json

```
{
  "rules": [
    {
      "token": "mystcraft:age",
      "productions": [
        ["mystcraft:terrain", "mystcraft:biome_controller", "mystcraft:weather"]
      ]
    },
    {
      "token": "mystcraft:terrain",
      "productions": [
        ["mystcraft:terrain"],
        ["mystcraft:terrain", "mystcraft:feature_large"],
        ["mystcraft:terrain", "mystcraft:feature_large", "mystcraft:feature_medium"]
      ]
    }
  ]
}
```

---

## 6) Troubleshooting

- If a world is empty: your density expression is <= 0 everywhere.
- If the world is solid: density is always > 0.
- If nothing happens: ensure the symbol is used in the age OR grammar picks it.
- If grammar ignores a symbol: check `allow_random`, `grammar` binding, and
  category.
