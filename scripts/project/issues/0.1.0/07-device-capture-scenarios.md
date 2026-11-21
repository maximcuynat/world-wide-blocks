Title: Capture scenarios of the devices
Labels: type: test, area: tooling, priority: medium
Milestone: version 0.1.0

For each device, automatic screenshots to look at instead of a check by hand, built on the capture runs of lot 1c-1 (#52). To do after the merge of `lot/1c-1` into `developement` and the rebase of the design branch. Plan agreed by Max on 2026-11-20.

- [x] Model check in the client at the start of every capture run: no block state or item of the mod uses the missing model, no texture is the missing texture; otherwise the run fails with the reason (and the build with it)
- [x] One scenario per device (`device_box`, `device_tower`, `device_laptop`), one window size and one language: front, back, three-quarter front and back, top
- [ ] The switches added to the device scenarios at lot 1d-1, once their blocks exist (`device_switch_5`, `device_switch_8`)
- [x] The four orientations side by side with the selection outline (an orientation added to the devices of `TrialScenario`)
- [x] Inventory (the item drawn large with its real inventory settings), first and third person in hand, on the ground, in an item frame
- [x] Screenshots in `run-capture/screenshots/device_<id>/`, read and reported by the assistant
