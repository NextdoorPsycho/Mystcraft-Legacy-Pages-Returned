# Scripted Terrain Reference

## Expression primitives

Numbers can be raw values or objects:
- 12.5
- {"value": 12.5}

Variables:
- {"var": "x"}
- {"var": "y"}
- {"var": "z"}
- {"var": "seed"}

## Math ops

Binary ops:
- add, sub, mul, div, min, max, pow, mod

Form:
- {"op":"add","a":X,"b":Y}

Unary ops:
- neg, abs, sqrt, floor, ceil, fract, sin, cos, tan

Form:
- {"op":"abs","input":X}

## Shaping ops

Clamp:
- {"op":"clamp","value":X,"min":A,"max":B}

Lerp:
- {"op":"lerp","a":A,"b":B,"t":T}

Step:
- {"op":"step","edge":E,"value":X}

Smoothstep:
- {"op":"smoothstep","edge0":A,"edge1":B,"value":X}

## Noise ops

2D noise:
- {"op":"noise2","scale":0.01,"octaves":4,"seed":42}

3D noise:
- {"op":"noise3","scale":0.02,"octaves":5,"seed":1337}

Ridged variants:
- ridged2 / ridged3

FBM variants:
- fbm2 / fbm3

Optional fields:
- lacunarity (default 2.0)
- gain (default 0.5)

Advanced inputs:
- x, y, z inputs can be overridden:
  {"op":"noise2","x":X,"z":Z,"scale":0.01}

## Cell ops (tiling)

Cell height (random per cell):
- {"op":"cell","mode":"square","size":64,"min":0,"max":1,"seed":9001}
- mode can be "square" or "hex"

Cell distance (0 at center, 1 at edge):
- {"op":"cell_distance","mode":"hex","size":48}

## Generator params

Fields under params for register_terrain_generator:
- name: string (used for debugging)
- density: expression (required)
- x_scale, y_scale, z_scale: per-axis scales (defaults 1.0)
- seed_offset: long (defaults 0)
- params: object of numeric named values (available as {"var":"<name>"})

## Common density patterns

Flat height:
- {"op":"sub","a":64,"b":{"var":"y"}}

Upside-down:
- {"op":"sub","a":{"var":"y"},"b":64}

Floating layer around y=100:
- {"op":"sub","a":{"op":"abs","input":{"op":"sub","a":{"var":"y"},"b":100}},"b":5}
