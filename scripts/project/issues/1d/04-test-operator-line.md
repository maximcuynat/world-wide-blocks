Title: Test operator line block
Labels: type: feature, area: blocks, priority: low
Milestone: lot 3b

Lot 3b (`docs/lot-plan.md`, lot 3), moved out of lot 1d-1 on 2026-11-21: a creative mode block that stands for the upstream operator, so that the line input port of the box can be tried long before the street and the ISP exist. It is replaced by the real chain over lots 4 to 6. Still to decide before coding: its model, approved by the maintainer in Blockbench (design guide, section 9), and the RJ11 line cord that links it to the line input of the box (the telephone cord of lot 3b, `docs/cable-design.md`, section 4).

- [ ] Block (model, texture, blockstate, item, loot table, pickaxe tag, both language files), creative tab only
- [ ] How it connects to the line input port of the box: to decide when the lot is designed (the Ethernet cord does not fit a line port)
- [ ] Status panel of the box: the line port shows whether the test line is connected; it carries no traffic before lot 6c-1
- [ ] Unit and game tests
