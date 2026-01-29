# Grammar-Heavy Template

This template emphasizes grammar-driven selection.
Symbols are bound to grammar tokens with ranks, and the grammar rules
combine them in multiple ways.

## Contents

- `pack.mcmeta`
- `data/mystcraft/mystcraft/symbols/terrain_scripted_plateaus.json`
- `data/mystcraft/mystcraft/symbols/feature_scripted_caverns.json`
- `data/mystcraft/mystcraft/symbols/feature_populator_mix.json`
- `data/mystcraft/mystcraft/grammar/world.json`

## Notes

- All symbols allow random generation and have explicit grammar tokens.
- Tweak ranks in the symbol files to bias selection.
- Adjust productions in `grammar/world.json` to change how often features appear.
