# Cables: choosing the port, attaching to the port, cable on the ground

Written on 2026-11-20. Status: **approved by Max on 2026-11-20**; nothing is coded yet. The decisions are recorded in the "Decisions taken" table of `docs/lot-plan.md`:

- choice of the port by aiming, with aim zones that go on over the top face of flat devices;
- plugs drawn by the client from the cables it knows (check in the 26.3 sources how to draw a small model in the cable pass before coding);
- cable on the ground drawn, not placed in the world;
- a wall is gone around, otherwise the cable hangs in the air until the wall sockets;
- lot 1d renamed 1d-1; lot 1d-2 in version 0.2.0;
- the tower and the laptop redrawn on the design guide, with the position of their ports, before lot 1d-1.

## 1. Steps

Each step is playable on its own. Only steps 1 and 2 are designed in detail here; section 6 checks that their architecture carries steps 3 and 4.

| Step | Lot | Playable at the end |
|---|---|---|
| 1 | 1d-1 | The player aims at the port they want: it is outlined and the status panel names it before the click. The cable leaves from the port itself, with an RJ45 plug pushed into it |
| 2 | 1d-2 (new lot, right after 1d-1, version 0.2.0) | The cable has a thickness, comes down from the plug, lies on the floor following the terrain and goes up to the other device; the player walks over it |
| 3 | 2b | RJ45 wall sockets; a cable fixed along the wall |
| 4 | 2b, then 8a | Trunking along the walls (2b), then cable trays (8a) |

## 2. What exists today (code read on 2026-11-20)

- `EthernetCableItem`: the first click takes `NetworkManager.firstFreePort`, the second click too; unplugging (sneak + cord) and taking an end off (sneak + empty hand, `takeCableEnd`) take the first occupied port (`disconnectFirst`). `CableStart` already keeps the index of the start port.
- The click reaches the server with the point aimed at: `UseOnContext.getClickLocation()` and `getClickedFace()` (checked in the 26.3 sources). Resolving the aimed port on the server needs no new packet. With an empty hand, `NetworkDeviceBlock.useWithoutItem` already has the `BlockHitResult` and only has to pass it on to `takeCableEnd`.
- The client already knows the position and the port of both ends of each cable it receives (`ClientLink`): attaching the cable to its port, and naming the port at the other end in the panel, need no packet change. The name of the device at the other end is known only if its chunk is loaded on the client.
- `CableRenderer`: a line (`RenderTypes.lines`) of 16 segments along a parabola, attached at the centre of the block at a fixed height of 0.3 block (`ATTACH_HEIGHT`) for every device; a held cable goes from the device to the hand.
- `PortStatusLayer` reads `Minecraft.hitResult` and draws the lines built in plain Java by `StatusPanel`.

## 3. Step 1a: choosing the port

### Options

| | Aim at the port (Max's idea) | Menu | Scroll wheel or sneak to cycle |
|---|---|---|---|
| Gesture | The same two clicks as today | A screen opens at each click: 4 clicks instead of 2 | Scroll (or sneak + click) to pick a number, then click |
| Discovery | Natural: you click where the port is; outline and panel confirm | Explicit list of the ports and their state | Hidden: nothing says that scrolling chooses a port |
| Small ports (switch, 0.5 px) | Needs a tolerance (below) | Not a problem | Not a problem |
| Big switches later (24 or 48 ports, lot 8b) | May become too fine at rack scale | Scales well | Long to cycle |
| Conflicts | None: the cord in hand goes through `onItemUseFirst` before the block | Screen opening in the middle of a gesture | The wheel changes the hotbar slot; sneak is already "unplug" |
| Code | Port layout per device, outline, one panel line | A screen, a packet for the choice | Client input event, a packet or an item component |

**Recommendation: aim at the port, with aim zones.** Elsewhere on the device, the first free port (first occupied port to unplug), as today. The menu stays an idea for the dense switches of lot 8b, not designed now.

### Port layout

One plain Java description per device, `PortLayout`, for a front facing north and turned by `FACING`: for each port, the face that carries it, its **aim zone**, its **hole**, its **mouth** (point and outward direction, where the plug goes in) and its **size** (large 3 x 3 texels or small 2 x 2). It is the single source of truth for the server (which port was clicked) and for the client (outline, panel, attachment, plug). Its values come from the device sheets (`design/blockbench/<id>.md`); a unit test checks them: as many ports as the block's `PORTS`, zones inside the shape and disjoint, each hole inside its zone.

### Aim zones (tolerance)

On the face that carries the ports, each port owns the strip of the face up to halfway to its neighbours (the ports at the ends go to the edge of the face), over a band around the ports. **On a flat device, the strip goes on over the top face**, where the port LEDs already are: a player standing 1.5 blocks from a switch on the floor looks down at about 47 degrees and sees its top (3 px deep) about 3 times taller than its front (1 px high); with zones on the front only, most clicks would fall back to the first free port.

![Aim zones](images/cable-aim-zones.png)

Box front on the left (zones over the lower band of the front). Switch on the right: its top as seen from the front (LEDs along the front edge), then its front; each port's zone runs over both. Holes in black, one tint per zone.

| Device | Hole | Aim zone | Gain |
|---|---|---|---|
| Internet box (4 LAN) | 0.75 x 0.75 px | about 1 px wide (2.4 px at the ends) x 2.5 px high (y 0.75 to 3.25) | x 4 or more |
| 5-port switch | 0.5 x 0.5 px | 0.75 px wide (1 px at the ends) x the whole front (1 px) and the whole depth of the top (3 px) | x 12 |

The click point is turned into the north frame of the device, then looked up in the zones of the clicked face. It lies on the Java shape of the block, not on the model: on the box, the shape's front is the glass band plane, 0.25 px in front of the port panel. The zones are wide enough for this gap not to matter. A click on another face, or outside the zones: no port is aimed, fallback to the first free port. A device with a single port (tower, laptop) needs no zone, but gets the outline so that the player sees where the cable goes.

**Port positions still missing.** Step 1 needs the position of every port. The box and the switch have theirs (device sheets). The tower has an RJ45 port at the back of its current model; the laptop has no port in its model at all. Lot 1d-1 therefore needs either the rework of the tower and the laptop on the design guide (planned next in the design work) or a provisional layout for both.

### Feedback before the click

- Outline of the aimed port: a thin light frame around the hole, drawn by the client like the block outline, only with a cord in hand or when sneaking with an empty hand.
- Status panel, an added line: compact "Port 3: free" or "Port 3: connected"; detailed (sneaking) also the device at the other end and its port. New language keys in both files, sample values in `LangFilesTest` (45 characters for the panel).
- Click on an occupied aimed port: refusal "Internet Box, port 3: already in use." (new key), instead of "no free port".

### Gestures (all of them follow the aimed port)

| Gesture | Aimed port | Elsewhere on the device |
|---|---|---|
| First click, cord in hand | Starts from this port | First free port |
| Second click | Ends on this port | First free port |
| Sneak + click, cord in hand (unplug) | Unplugs this port | First occupied port |
| Sneak + click, empty hand (take an end off) | Takes off this end | First occupied port |

## 4. Step 1b: the cable attached to its port, with its plug

- **Attachment:** the end of the cable is the mouth of its port (`PortLayout`, turned by `FACING`, read from the block state on the client). `ATTACH_HEIGHT` disappears. The cable leaves along the outward direction of the port over the length of the plug.
- **Plug:** drawn in Blockbench like the devices (`design/blockbench/rj45_plug.bbmodel`), two sizes, for the large and the small holes. Its body fills the hole: 0.5 px inside, 0.5 px out; then a boot in the colour of the cord, then the cable. Its colours: highlight tone of light plastic for the clear plastic body, colour of the cord for the boot (cords are a colour family of their own, guide section 4).
- **Thick cable:** square section of 0.25 px (a 6 mm cord x 2.4 is 0.23 px), four faces per segment, lit by the light of the world where it passes, in the colour of the cord. It replaces the line of `RenderTypes.lines`. The held cable goes from the plug to the hand.
- **In step 1 the cable keeps today's hanging curve**, between the ends of the two plugs. Step 2 replaces this curve.
- Mock (axis-aligned boxes, not the final rendering): plugs in LAN 1 and LAN 3 of the box and in port 3 of the switch, port 4 of the box outlined, cables already laid on the floor as in step 2.

![Plugs and cable on the ground](images/cable-mock.png)

### How to draw the plugs

| | From the cables known to the client (recommended) | Block states per port with multipart models |
|---|---|---|
| Source | `ClientLinkCache`, the same as the cables | A copy of "port connected" in each block state |
| Consistency | Always the same as the drawn cable | A second copy of the state of the links, stale in a chunk that is not loaded when the other end is unplugged |
| Cost | Drawn each frame with the cables | Baked with the chunk, but 2^ports x 4 block states and a chunk rebuild at each change |

**Recommendation: from the cables known to the client.** To check in the 26.3 sources before coding: how to submit a small baked model or textured quads in `SubmitCustomGeometryEvent`.

## 5. Step 2: the cable on the ground (sub-lot 1d-2)

### Drawn, or placed in the world?

| Consequence | Drawn (route computed by each client) | Placed in the world (cable blocks, like redstone) |
|---|---|---|
| Walking over it | Yes, nothing to collide with | Yes, with an empty collision shape; but each cell it crosses is taken: no carpet, torch or flower there, and no other block |
| Walls | The route goes around (doors) or, failing that, hangs in the air; the player does not choose it | The player lays the route by hand, through an opening; realistic, but long |
| Wall sockets, trunking, trays (steps 3 and 4) | Real blocks that become fixed points of the route (section 6): the player controls the route where it matters | Natural: everything is blocks |
| Save | Nothing: the route is derived from the terrain. Later, the fixed points in the link (an optional field, old worlds still load) | Blocks or block entities in every chunk crossed, plus the link in the engine: two copies to keep in agreement, also when a cable is cut in a chunk that is not loaded |
| Multiplayer | No cost on the server; each client computes the routes it sees; the same blocks give the same route | Block updates for each cell, cables that can be cut by another player, more data per chunk |
| Length of a cable | Straight distance (100 blocks), as today, until there are fixed points: the drawn cable can be longer than the length used for the 100-block limit and for the latency shown by `ping` | Real length of the route |
| The route changes | When a block changes on it: a block placed on the cable makes it move aside; doors count as always passable, so that opening and closing one does not move the cable | Only when the player moves it |
| Code | A route computed on the client from the terrain, cached | Cable blocks, placement and cutting, agreement between the engine and the world |

**Recommendation: drawn.** It follows the rule "the server decides, the client displays", keeps a single copy of the network (the engine), adds nothing to the save, costs nothing on the server, and the player walks over it. The control that a world cable would give comes with steps 3 and 4, through real carrier blocks (sockets, clips, trunking, trays).

### The route

A route is a chain: **port A, span, fixed points (none in step 2), span, port B.** Step 2 has two kinds of span:

- **On the ground:** out of the plug, down to the surface below (the top of the first collision shape under it), then along the floor to below the other plug, then up into it. Along the floor, a path search on the cells of a corridor around the straight line (8 blocks on each side): it goes around walls (through a door), climbs up to one block (steps, slabs), may come down a drop. The path is then straightened where nothing is in the way, and laid on the real height of the surfaces, sampled every quarter of a block.
- **Hanging:** today's curve, between two points. Used when no route is found on the ground (wall without an opening, closed room, a part of the terrain not received by this client). Step 3 (wall sockets) gives the clean answer to a wall.

Rules:

- **Several cables in the same place:** each cable is shifted sideways by a fixed amount taken from its identifier (steps of 0.25 px), and laid a quarter of a pixel higher where it crosses another, so that two cables never fight for the same pixels. Cables of the same device fan out from their ports anyway.
- **Recomputed only when needed:** the client keeps the route of each visible cable and recomputes it when a block changes in its corridor or when a chunk it crosses arrives, with a budget per frame so that a large change does not freeze the game.
- **Performance:** nothing on the server; on the client, a search over at most a few thousand cells per cable, once per change, and a mesh rebuilt only when the route changes. Hundreds of cables (lot 8a) will need the meshes baked once; to look at then.
- **Testable:** the route is computed in plain Java from a terrain interface (height of the surface, can a cable pass), with fake terrains in `src/test`.

### What stays for lot F

The polish: rounded bends, a little slack, the cable lights of the ports (idea "Port lights in the finishing lot").

## 6. Steps 3 and 4: does the architecture hold?

- **Wall socket (2b):** a passive device with ports: it gets a `PortLayout`, the aim, the plugs and the attachment of steps 1 and 2 without change. The cord from the computer to the socket is an ordinary cable.
- **Cable fixed to the wall (2b):** wall clips are fixed points of the route; between two of them, a third kind of span, "along the wall".
- **Trunking (2b) and cable trays (8a):** real blocks placed by the player. The route of a cable is searched on the server through the network of carriers between the entries closest to its two ends, and stored as fixed points in the link (optional field in `NetworkSavedData`). A fourth kind of span, "carried", runs straight inside the carrier. The length of the cable becomes the length of its route.
- Nothing of steps 1 and 2 is thrown away: port layout, plug, thick cable, chain of spans, route cache. Steps 3 and 4 add kinds of span and fixed points.

### Needs of step 3 noted on 2026-11-20 (not designed yet)

1. **Preview before laying a cable through a wall:** the route is shown before the click that confirms it.
2. **Finding hidden cables again** (offices): a view through the walls while holding a cable, each cable in the colour of its cord. Later, a detection tool of lot 9a could show the same.

What steps 1 and 2 must keep possible, and do:

| Need | What it requires | Provided by steps 1 and 2 |
|---|---|---|
| Preview | Computing a route before it exists, on the client, from the pending end (`CableStart`) to the aimed point or port | The route is computed in plain Java from a terrain interface, without the network: the same code computes a candidate route for the preview. It must live in common code, not in `client/`, so that the server can also compute and check routes from step 3 on |
| Route known to the client | The whole route of a hidden cable, including the parts inside walls and carriers | Free spans are computed by the client; fixed points (step 3) are stored in the link and sent with it (`LinkAddedPayload` gains them then): the client knows the whole chain |
| Drawing through blocks | A second drawing of the routes without the depth test, only while the player holds a cable (or the 9a tool) | The thick cable mesh is built once per route and kept: drawing it a second time with a render type that ignores depth costs no new computation. The render type to use is to check in the 26.3 sources at step 3 |
| Colour of the cord | A colour per cable | Today every cord is blue; the colour becomes a property of the link, sent with it, when cords of several colours exist |
| Detection tool (9a) | The same view, triggered by an item | Same code as the view through the walls, another trigger |

## 7. Impacts on the existing code (steps 1 and 2)

| Part | Change |
|---|---|
| Blocks (`PcTowerBlock`, `LaptopBlock`, `InternetBoxBlock`, then the switches) | Each declares its `PortLayout` |
| `PortLayout` (new, plain Java) | Ports, zones, holes, mouths, rotation by `FACING`; unit tests |
| `EthernetCableItem` | Start, finish, unplug and take an end off use the aimed port, fallback as today; refusal "port in use" |
| `NetworkDeviceBlock` (empty hand) | Passes the click point to `takeCableEnd` |
| `StatusPanel`, `PortStatusLayer` | An optional aimed port, one more line; new keys in `en_us.json` and `fr_fr.json`, samples in `LangFilesTest` |
| `CableRenderer` | Attachment at the mouth of the port, plugs, thick cable; step 2: routes on the ground, cache, hanging fallback |
| Route (new, plain Java, step 2) | Chain of spans, search on the ground, straightening, lanes; tests on fake terrains |
| Packets, engine, save | Unchanged in steps 1 and 2 |
| Device sheets | A "Port layout" table (zone, hole, mouth, direction, size) for each device |
| `design/tools` | Prints the port layout of a device for the Java code; the plug model |
| Capture runs (`docs/automated-checks.md`) | Scenes for the outline and the panel line, plugs, cables on flat and uneven ground, a wall, several cables |
