# Design tools

Scripts that draw the equipment for Blockbench, following [`docs/design-guide.md`](../../docs/design-guide.md). Standard library only (Python 3.9 or later). The `design/` folder is outside `src/main/resources`: nothing here goes into the jar.

## The rule

**Once a `.bbmodel` has been saved from Blockbench, it is the reference: never generate over it again.** Changes are then made in Blockbench, or by hand in the project. A device script refuses to overwrite an existing project; `--force` is only for a project that was never saved from Blockbench.

## Files

- `wwb_design.py`: shared helpers. Palette (only its colours are accepted), texture sheet with face areas at 4 texels per px, side faces with highlight and shadow edges, RJ45 ports, LEDs, display settings (guide, section 7), table of the ports and LEDs for the device sheet, Blockbench project with the texture embedded.
- `<id>.py`: one script per device, for example `internet_box.py`.
- `export.py`: exports an approved project to the assets.

## Use

```bash
python3 design/tools/internet_box.py --table             # ports and LEDs table, display values; writes nothing
python3 design/tools/internet_box.py --preview <dir>     # writes design/blockbench/internet_box.bbmodel (and the PNG into <dir>)
python3 design/tools/export.py internet_box --out <dir>  # look at the export first
python3 design/tools/export.py internet_box              # models/block/internet_box.json and textures/block/internet_box.png
```

For a new device: copy `internet_box.py`, place the faces of each element on the sheet with `add_face`, draw with the helpers, and paste the `--table` output into the device sheet `design/blockbench/<id>.md`. After the export, check the blockstate (four orientations and the device states), the item model and the Java shape by hand.
