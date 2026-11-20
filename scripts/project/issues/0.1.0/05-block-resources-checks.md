Title: Static checks of the block resources
Labels: type: test, area: blocks, priority: high
Milestone: version 0.1.0

A unit test, run by `./gradlew build test` without the game, that catches what the client would only report while loading the models. Plan agreed by Max on 2026-11-20.

- [x] Every blockstate variant and every item model points to an existing model
- [x] Rotations of the blockstates: `y` 0, 90, 180 and 270 for `facing` north, east, south and west, so that the front of a model faces `FACING`
- [x] Every texture of a model exists, its size is the `texture_size` of the model; element coordinates between -16 and 32; every face has a defined texture and UVs inside 0 to 16
- [x] Every colour of every texture of the mod belongs to the palette of the design guide, read from `design/palette.json` (single source, also read by `design/tools/wwb_design.py`), except the textures listed there with their reason
- [x] Every block has its name in both language files
- [x] Gson as a test dependency, at the version the game ships
