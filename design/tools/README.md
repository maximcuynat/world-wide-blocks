# Design tools

Scripts that draw the equipment for Blockbench, following [`docs/design-guide.md`](../../docs/design-guide.md). Standard library only (Python 3.9 or later). The `design/` folder is outside `src/main/resources`: nothing here goes into the jar.

## The rule

**Once a `.bbmodel` has been saved from Blockbench, it is the reference: never generate over it again.** Changes are then made in Blockbench, or by hand in the project. A device script refuses to overwrite an existing project; `--force` is only for a project that was never saved from Blockbench.

## Files

- `wwb_design.py`: shared helpers. Palette (only its colours are accepted), texture sheet with face areas at 4 texels per px, side faces with highlight and shadow edges, RJ45 ports (painted, or the bottom of a recessed port), LEDs, `add_slice` (one drawing shared out between the elements that make up a face, such as a front panel split around recessed ports), display settings (guide, section 7), table of the ports and LEDs for the device sheet, Blockbench project with the texture embedded (hidden faces left out).
- `<id>.py`: one script per device: `internet_box.py`, `pc_tower.py`, `laptop.py` (two models sharing one texture), `desktop_switches.py` (the family of desktop switches: 5 and 8 ports, blocks of lot 1d-1).
- `cables.py`: design drafts of the cables (`docs/cable-design.md`): the RJ45 plug, cables on the ground with their crossings and lanes, the two-tone outline of the aimed port, the cord colours, the cord icon; drafts only, under `design/blockbench/drafts/`.
- `rj45_plug.py`: the RJ45 plug of lot 1d-1, one project per cord colour sharing one texture; `--scene` prints the preview of a plug in every port of the devices.
- `port_layout.py`: the port layout of a device (lot 1d-1, `docs/cable-design.md` section 3): holes read from the drawing of the device, mouths, outward directions and aim zones; the "Port layout" table of the device sheet, or `--java` the ports as the Java records of `PortLayouts` print them, to compare with the code; `--zones-image <png>` draws the image of the aim zones of `docs/cable-design.md` (`docs/images/cable-aim-zones.png`).
- `export.py`: exports an approved project to the assets.
- `preview.py`: isometric preview of one or more projects, front and back, with the face shading of the game, to look at a drawing without Blockbench or to compare variants side by side; `--scene` with offsets for several projects together (`<id>@dx,dz`, or `<id>@dx,dy,dz,angle` to turn a project as the `y` of a blockstate), `--centre` and `--views` for a close-up.

Variants that were set aside are kept in `design/blockbench/archive/`.

## Use

```bash
python3 design/tools/internet_box.py --table             # ports and LEDs table, display values; writes nothing
python3 design/tools/internet_box.py --name internet_box_try                    # writes design/blockbench/internet_box_try.bbmodel
python3 design/tools/preview.py <out.png> internet_box internet_box_try        # compares them side by side
python3 design/tools/preview.py <out.png> internet_box@-5,0 desktop_switch_5@5,0 --scene --span 22   # two families on the same desk
python3 design/tools/export.py internet_box --out <dir>  # look at the export first
python3 design/tools/export.py internet_box              # models/block/internet_box.json and textures/block/internet_box.png
```

For a new device: copy `internet_box.py`, place the faces of each element on the sheet with `add_face`, draw with the helpers, and paste the `--table` output into the device sheet `design/blockbench/<id>.md`. After the export, check the blockstate (four orientations and the device states), the item model and the Java shape by hand.
