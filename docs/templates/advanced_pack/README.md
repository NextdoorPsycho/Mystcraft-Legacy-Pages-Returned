# Advanced Datapack Template

This template includes multiple symbols (scripted terrain + feature pool)
and a grammar file that can select them.

## Contents

- `pack.mcmeta`
- `data/mystcraft/mystcraft/symbols/terrain_scripted_hex.json`
- `data/mystcraft/mystcraft/symbols/terrain_scripted_inverted.json`
- `data/mystcraft/mystcraft/symbols/feature_scripted_noise_islets.json`
- `data/mystcraft/mystcraft/symbols/feature_populator_shuffle.json`
- `data/mystcraft/mystcraft/grammar/world.json`

## Usage

1) Copy this folder into your world datapacks:
   `saves/<world>/datapacks/advanced_pack`

2) Reload datapacks in game:
   `/reload`

3) Generate a random age or craft pages for any of the symbols above.

Notes:

- If you want grammar to ignore a symbol, remove its `grammar` field or set
  `"grammar": false`.
- The "Sky Islets (Feature)" symbol registers a scripted generator; if you want
  it
  to act as a true feature instead of base terrain, leave it out or convert it
  to
  a populator/alteration instead.
