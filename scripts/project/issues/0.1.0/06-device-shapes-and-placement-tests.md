Title: Game tests of device shapes and placement
Labels: type: test, area: blocks, priority: high
Milestone: version 0.1.0

Game tests (`./gradlew runGameTestServer`) for every device, in the four orientations and in every state (laptop open and closed). Plan agreed by Max on 2026-11-20.

- [ ] The selection and collision shapes cover the model exactly as the blockstate turns it, read from the model and blockstate files (one texel of tolerance for a model with a rotated element)
- [ ] A device placed by a test player looking toward each direction faces that player (`FACING` toward them)
- [ ] The internet box, the PC tower and the laptop covered; each new device added to the list
