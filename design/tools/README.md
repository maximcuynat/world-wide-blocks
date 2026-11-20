# Design tools

Scripts that draw the equipment for Blockbench, following [`docs/design-guide.md`](../../docs/design-guide.md). Standard library only (Python 3.9 or later). The `design/` folder is outside `src/main/resources`: nothing here goes into the jar.

## The rule

**Once a `.bbmodel` has been saved from Blockbench, it is the reference: never generate over it again.** Changes are then made in Blockbench, or by hand in the project. A device script refuses to overwrite an existing project; `--force` is only for a project that was never saved from Blockbench.

## Files

- `wwb_design.py`: shared helpers. Palette (only its colours are accepted), texture sheet with face areas at 4 texels per px, side faces with highlight and shadow edges, RJ45 ports (painted, or the bottom of a recessed port), LEDs, `add_slice` (one drawing shared out between the elements that make up a face, such as a front panel split around recessed ports), display settings (guide, section 7), table of the ports and LEDs for the device sheet, Blockbench project with the texture embedded (hidden faces left out).
- `<id>.py`: one script per device: `internet_box.py`, `desktop_switch_5.py` (approved draft, starting point of lot 1d-1).
- `export.py`: exports an approved project to the assets.
- `preview.py`: isometric preview of one or more projects, front and back, with the face shading of the game, to look at a drawing without Blockbench or to compare variants side by side.

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
