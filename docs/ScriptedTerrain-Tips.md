# Scripted Terrain Tips

## Performance
- Keep expressions simple; they run per noise grid point, not per block.
- Prefer noise2 over noise3 when you can.
- Use lower octaves when possible (3-5 is usually enough).

## Height control
A common pattern:
- density = height - y

Example:
{
  "op": "sub",
  "a": 64,
  "b": { "var": "y" }
}

## Floating layers
Use distance from a target Y to create bands:
{
  "op": "sub",
  "a": 6,
  "b": { "op": "abs", "input": { "op": "sub", "a": { "var": "y" }, "b": 100 } }
}

## Hex vs square
- Use cell.mode = "hex" for hex tiling
- Use cell.mode = "square" for grid tiling

## Combine noise and cells
You can modulate cell heights with noise:
{
  "op": "add",
  "a": { "op": "cell", "mode": "hex", "size": 48, "min": 0, "max": 1 },
  "b": { "op": "mul", "a": { "op": "noise2", "scale": 0.02 }, "b": 0.2 }
}

## Debugging
- Start with a simple heightmap, then layer in noise.
- If a world is empty, your density is likely <= 0 everywhere.
