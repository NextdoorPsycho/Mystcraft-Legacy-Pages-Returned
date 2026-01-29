# Scripted Terrain Overview

Scripted terrain lets datapacks define new terrain generators with JSON.
A symbol can register a scripted generator, and that generator computes a
"density" value for every point in the world. Density rules:

- density > 0.0: place terrain block
- density <= 0.0: air (or sea if below sea level and seas are enabled)

This is intended for custom procedural styles like inverted worlds,
hex tile plateaus, floating islets, cavern lattices, and more.

## How it fits into Ages

1) A symbol JSON registers a scripted terrain generator:
   - logic.type = "register_terrain_generator"
   - id = "mystcraft:scripted"
   - params.density = JSON expression

2) The generator becomes the active terrain generator for the Age.

3) Terrain alterations and populators still run normally after base terrain.

## Where to put files

- Symbols are datapack JSONs under:
  data/<namespace>/mystcraft/symbols/

Example path:
- data/mystcraft/mystcraft/symbols/terrain_scripted_example.json

## Minimal symbol example

{
  "category": "terrain",
  "card_rank": 4,
  "instability_cost": 6.0,
  "display_name": "Scripted Example",
  "logic": [
    {
      "type": "register_terrain_generator",
      "id": "mystcraft:scripted",
      "params": {
        "name": "example",
        "density": {
          "op": "sub",
          "a": 64,
          "b": { "var": "y" }
        }
      }
    }
  ]
}
