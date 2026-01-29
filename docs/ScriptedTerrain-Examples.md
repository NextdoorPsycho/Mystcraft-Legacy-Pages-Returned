# Scripted Terrain Examples

These are full symbol examples you can drop into a datapack.
Paths:
- data/mystcraft/mystcraft/symbols/

## Inverted Terrain

{
  "category": "terrain",
  "card_rank": 4,
  "instability_cost": 8.0,
  "display_name": "Inverted Terrain",
  "logic": [
    {
      "type": "register_terrain_generator",
      "id": "mystcraft:scripted",
      "params": {
        "name": "inverted",
        "density": {
          "op": "sub",
          "a": { "var": "y" },
          "b": {
            "op": "add",
            "a": 64,
            "b": {
              "op": "mul",
              "a": { "op": "noise2", "scale": 0.01, "octaves": 5, "seed": 1337 },
              "b": 30
            }
          }
        }
      }
    }
  ]
}

## Hex Tile Terrain

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

## Floating Islets

{
  "category": "terrain",
  "card_rank": 4,
  "instability_cost": 9.0,
  "display_name": "Scripted Sky Islets",
  "logic": [
    {
      "type": "register_terrain_generator",
      "id": "mystcraft:scripted",
      "params": {
        "name": "sky_islets",
        "density": {
          "op": "mul",
          "a": {
            "op": "sub",
            "a": { "op": "noise3", "scale": 0.02, "octaves": 4, "seed": 7717 },
            "b": 0.35
          },
          "b": { "op": "step", "edge": 80, "value": { "var": "y" } }
        }
      }
    }
  ]
}

## Cavern Lattice

{
  "category": "terrain",
  "card_rank": 4,
  "instability_cost": 7.0,
  "display_name": "Cavern Lattice",
  "logic": [
    {
      "type": "register_terrain_generator",
      "id": "mystcraft:scripted",
      "params": {
        "name": "cavern_lattice",
        "density": {
          "op": "sub",
          "a": 0.45,
          "b": {
            "op": "abs",
            "input": { "op": "noise3", "scale": 0.03, "octaves": 5, "seed": 4242 }
          }
        }
      }
    }
  ]
}

## Voronoi Mesas

{
  "category": "terrain",
  "card_rank": 4,
  "instability_cost": 6.0,
  "display_name": "Voronoi Mesas",
  "logic": [
    {
      "type": "register_terrain_generator",
      "id": "mystcraft:scripted",
      "params": {
        "name": "voronoi_mesas",
        "density": {
          "op": "sub",
          "a": {
            "op": "add",
            "a": 58,
            "b": {
              "op": "mul",
              "a": { "op": "cell", "mode": "square", "size": 64, "seed": 13331, "min": 0.0, "max": 1.0 },
              "b": 50
            }
          },
          "b": {
            "op": "add",
            "a": { "var": "y" },
            "b": {
              "op": "mul",
              "a": { "op": "cell_distance", "mode": "square", "size": 64 },
              "b": 18
            }
          }
        }
      }
    }
  ]
}
