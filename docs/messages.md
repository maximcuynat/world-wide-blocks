# Messages

Audit of the messages of lots 1a and 1b-1 (2026-11-18), then design of the message module; section 3 (terminal) added on 2026-11-19. Status: **approved by Max on 2026-11-18, coded the same day** (state and checks by eye in `docs/lot-plan.md`, lot 1b), with these decisions:

- Saturated network: warn the connected operators, plus the world owner in single player.
- Level symbols: agreed, to check by eye, with a plain-text fallback (client setting).
- "Sneak: details" line kept in the compact panel.
- Texts of `/worldwideblocks test` hard-coded and in English.
- The French texts use formal address everywhere, like the French translation of Minecraft.
- Distances rounded to the nearest integer ("4 blocks"), in all languages.
- **Data separated from drawing**: the server sends key, values and level; the client alone decides how it looks (symbol, colour, place). A future polished HUD (finishing lot) must be able to replace the drawing without touching the engine or the messages.

## 1. Audit

### Action bar (above the hotbar)

All sent by `EthernetCableItem.tell()` (`player.sendOverlayMessage`), to the player who makes the gesture only, once per gesture. Each send was also logged with a temporary trial trace (DEBUG, in French, text rendered by the server).

| Key (`message.worldwideblocks.cable.…`) | Text (English) | Trigger |
|---|---|---|
| `start` | %s, port %s: right-click the second device. | First click, cord in hand |
| `no_free_port` | %s: no free port. | First click on a full device |
| `cancelled` | Connection cancelled. | Sneak + click in the air, connection started |
| `start_gone` | The first end no longer exists: connection cancelled. | Second click, or each inventory tick, if the start has disappeared |
| `cancelled_dimension` | Dimension changed: connection cancelled, the cord is free. | Inventory tick after a dimension change |
| `start_taken` | The first end's port is now in use: connection cancelled. | Second click, start port taken in the meantime |
| `connected` | Connected: %s port %s ↔ %s port %s (%s blocks). | Successful second click |
| `unplugged` | %s: port %s unplugged. | Sneak + click, cord in hand; or end removed from a cable placed in creative, in survival |
| `nothing_to_unplug` | %s: no cable plugged in. | Sneak + click on a device without a cable |
| `end_taken` | End removed from %s, the other stays on %s, port %s. | Sneak + click, empty hand |
| `retracted` | %s: cable unplugged, the cord is free. | Click on the device where the other end is plugged in |
| `result.not_a_device` | This is not a network device. | Engine refusal at the second click |
| `result.incompatible_port` | This port does not accept this cable. | Same |
| `result.same_device` | Pick a device other than the first one. | Same |
| `result.other_dimension` | Both devices must be in the same dimension. | Same |
| `result.too_far` | Too far: %s blocks, %s maximum. | Same |
| `result.port_in_use` | (removed) no free port on the device, same meaning as `no_free_port` | Same |
| `result.ok` | (removed) a plain "connected" confirmation | Never shown |

The English column quotes `en_us.json` as it is now. The audit measured the earlier French wording, which was longer for `start`, `end_taken` and `retracted` (they also carried the permanent instructions, see defects 4 and 5).

### Chat

| Key | Text (English) | Trigger, recipient, frequency |
|---|---|---|
| `message.worldwideblocks.network.loop` | Network loop near %s. Remove one cable of the loop. | Frame came back to a switch (`SimulationEvent.LoopDetected`). Players within 32 blocks of the device that sees it. At most once per minute **and per device** |
| `commands.worldwideblocks.links.*` | %s network link(s): … | Reply to `/worldwideblocks links`, to whoever typed the command |
| (hard-coded text) | Scene "…": …, Command reserved for trial worlds: this world is called "…", No scene to remove since you joined., … | Replies to `/worldwideblocks test`, development only (`gametest` source set) |

### Status panel (client, targeted device)

Always detailed: the name, then one line per port with an indicator (green connected, grey free, dark grey inactive), and for a computer a `MAC …` line under each port.

| Key (`hud.worldwideblocks.…`) | Text (English) |
|---|---|
| `port.connected` | Port %s: connected |
| `port.free` | Port %s: free |
| `port.line` | Port %s (line): inactive |
| `port.connected_learned` | Port %s: connected, %s |
| `mac` | MAC %s |
| `learned_more` | %s (+%s) |

### Log

| Level | Text | Where |
|---|---|---|
| INFO | World Wide Blocks loaded | `WorldWideBlocks` |
| WARN | Inconsistent network link ignored on load: {} | `NetworkSavedData` |
| WARN | Network saturated: more than {} frames waiting, new frames were lost. | `NetworkManager`, at most one per minute; no player warned |
| WARN | Network loop: a broadcast frame comes back to {} ({}) through port {}. Copy dropped. | `NetworkManager`, one per device and per minute |
| DEBUG | Temporary trial trace: message sent by the cord | `EthernetCableItem.tell` |
| DEBUG | Temporary trial trace: device registered, cable plugged in, cable unplugged, device removed | `NetworkManager` |
| DEBUG | Temporary trial trace: on load, a summary, then one device and one cable per line | `NetworkSavedData` |
| DEBUG | Temporary trial trace: what each player's indicator shows, at each change | `NetworkSync` |
| DEBUG | Temporary trial trace: placing a block with its facade | `NetworkDeviceBlock` |
| DEBUG | Temporary trial trace: learned and forgotten MACs, read out every second | `TrialLog.compareLearned` |
| INFO | [GameTest] loop messages received … | game tests |
| (exceptions) | Negative port count, Invalid limits, MAC address out of 48 bits, … (about ten) | Programming errors, which end up in the log or a crash report |

### Defects found

1. **Loop: two messages for a single problem.** The report is limited per device: two boxes in a loop give two chat messages, then two per minute.
2. **Log in two languages.** French sentences, device name rendered by the server (in English, or the raw key), French trial traces.
3. **Duplicate**: `no_free_port` and `result.port_in_use` say the same thing. `result.ok` is never shown.
4. **Too long for the action bar**, which fades after 3 s: `end_taken` (about 150 characters), `start` (about 100).
5. **Informal and formal address mixed** in the French texts: some used informal address (the cord "in your hand", "your connection"), others formal address ("Pick", "remove").
6. **Numbers**: lengths are formatted by the server with a point (`4.0 blocks`, even in a French-language game).
7. **No level**: a refusal and a success look the same in the action bar.
8. **Panel always detailed**: 6 lines for the box, one MAC line per port for a computer.
9. **Saturated network**: only in the log, the player does not know.
10. **Device format**: `%s [%s, %s, %s]` copied into each text (loop, `/worldwideblocks links`), written differently in the traces.
11. **Tests**: messages are only captured by overriding the test player (`TestPlayer.sendSystemMessage`). The log cannot be checked, and the loop test reads the coordinates by position in the arguments.

## 2. Proposed design

### Uses, levels and channels

Each message declares a **use** and a **level**; the channel follows from them, the calling code does not choose it.

| Use | Level | Channel |
|---|---|---|
| Gesture (immediate reply to the player who acts) | all | The player's action bar, plus a DEBUG line in the log |
| Network event | warning, error | Chat of the players concerned, plus a WARN or ERROR line in the log |
| Network event | info, success | Log only (INFO) |
| Diagnostic | (none) | Log, DEBUG |
| Targeted device | (indicators) | Status panel, client side |

Accepted exception: the reply to a command (`/worldwideblocks links`, `/worldwideblocks test`) stays in the chat of whoever typed it, like all Minecraft commands. The texts of `/worldwideblocks test` stay hard-coded, in English (`TrialSceneCommand`): the command only exists in development (`gametest` source set, absent from the jar), and its keys do not belong in the distributed language files.

Level of a gesture: success is a **success**; a step or a cancellation wanted by the player is an **info**; a cancellation caused by something else (start disappeared, dimension change, port taken in the meantime) is a **warning**; a refused gesture is an **error**.

| Level | Colour | Symbol |
|---|---|---|
| Info | white | ℹ |
| Success | green | ✔ |
| Warning | yellow | ⚠ |
| Error | red | ✖ |

The symbol and the colour are chosen by the client from the received level: they are neither in the language files nor sent by the server. Fallback: a client setting (`worldwideblocks-client.toml`, `messageSymbols = false`) replaces the symbol with a translated word ("Info", "OK", "Warning", "Error"). Symbol rendering to check by eye.

### Repeat limit

- A network event carries a **problem key**. The module only passes it on once per game minute for a given key (value already approved), whatever the number of devices that see it. Time is the simulation tick (no gap possible with the clock fixed on 2026-11-18).
- The loop key is the **network** where it happens (the set of devices linked by cables, named by its smallest device id). The engine groups all detections of the same tick by network and emits a single event, with the list of devices and ports where the frames came back. The per-minute limit moves from the engine to the module: the engine states the fact, the module decides who to warn and when.
- **Forgetting loop keys**: at each change of a network (device placed or removed, cable plugged in or unplugged), the loop key of that network is forgotten, before and after the change (a removed cable can split it in two, an added cable can join two). Other networks keep their limit: a player tinkering elsewhere does not restart the messages of a loop they do not touch. Removing a cable is precisely the gesture that repairs a loop: if it is still there, the player knows at the next report instead of waiting for the minute. This also covers `/worldwideblocks test clear` followed by `loop` at the same place, and a network whose smallest id changes (which only happens through a change of the network). Maximum frequency: once per minute, plus once per player gesture.
- Saturated network: single key, at most one message per minute, never forgotten before.
- The action bar needs no repeat limit: a message replaces the previous one, and each answers a click.

### Recipients

- Loop: each player within 32 blocks of one of the affected devices receives **one** message, which names the affected device closest to them.
- Saturated network: the problem is global to the server. Proposal: the connected operators, plus the world owner in single player. Checked in the sources (`PlayerList.isOp`): in single player without cheats, the player is not an operator, hence the second case.

### Texts

- The server only sends keys and values: integers, coordinates, and device names as a key (`block.worldwideblocks.…`).
- A device is written everywhere with the same key `message.worldwideblocks.device`: `%s [%s, %s, %s]`, for example "Internet Box [12, 64, -5]".
- Lengths in whole blocks, rounded to the nearest integer (no more decimal separator, in any language).
- The French texts use formal address everywhere, or an impersonal wording.
- Duplicate merged (`result.port_in_use` replaced by `no_free_port`), `result.ok` removed.
- Maximum lengths, longest values included (device name in the longest language, one-digit port, 3-digit length, 5-character coordinates): **80 characters** for the action bar (with the automatic GUI scale on a 1920 x 1080 screen, it fits about 85 characters), **45** for a panel line, **90** for the chat (two chat lines). Checked by a test on both language files.
- The permanent instructions ("sneak + click in the air to cancel", "right-click a device to plug it in again") leave the action bar, where they do not fit, for the cord's tooltip in the inventory.
- Programming errors (exceptions) switch to English: they end up in the log.

Rewritten texts (English, same meaning in French). The "Chars" column is the length of the longest case of the French text, measured at the design; `LangFilesTest` checks the current limits in both languages:

| Key | Proposal | Chars (French) |
|---|---|---|
| `cable.start` | %s, port %s: right-click the second device. | 58 |
| `cable.end_taken` | End removed from %s, the other stays on %s, port %s. | 68 |
| `cable.retracted` | %s: cable unplugged, the cord is free. | 54 |
| `cable.connected` | Connected: %s port %s ↔ %s port %s (%s blocks). | 66 |
| `network.loop` | Network loop near %s. Remove one cable of the loop. | 85 |
| `network.saturated` | Network saturated: frames are being lost. Look for a loop. | 60 |
| `item.worldwideblocks.ethernet_cable.tooltip` (3 lines) | Right-click two devices to connect them. / Sneak + click a device: unplug. / Sneak + click in the air: cancel. | 45 per line |

### Status panel

Compact by default, detailed when the player is sneaking. Example, the box with 3 connected ports and 2 MACs behind port 1:

```
Compact                       Sneaking
┌──────────────────────┐      ┌─────────────────────────────────────────────┐
│ Internet Box         │      │ Internet Box                                │
│ ● 3/4 ports connected│      │ ● Port 1: connected, 02:5e:11:a0:7c:1a (+1) │
│   Sneak: details     │      │ ● Port 2: connected, 02:5e:11:a0:7c:2b      │
└──────────────────────┘      │ ● Port 3: connected                         │
                              │ ○ Port 4: free                              │
                              │ ▪ Port 5 (line): inactive                   │
                              └─────────────────────────────────────────────┘
```

MACs stay displayed in full (42 characters for the longest line).

- Box: compact, the number of connected local network ports; sneaking, one line per port as today.
- Computer: compact, "Connected" or "Not connected"; sneaking, the port and its MAC.
- The (grey) "Sneak: details" line only appears in compact mode: proposal, to remove if it clutters.
- The panel lines are built by a plain Java class, tested without starting the game; the client only draws them.

### Log

- Entirely in English, texts written in the code (the server translates nothing); a device is written there as `worldwideblocks:internet_box [12, 64, -5] minecraft:overworld`.
- Each message sent to a player is logged there at DEBUG: channel, key, values, recipient.
- Network events at WARN (loop: all affected devices and ports, on one line).

### Temporary trial traces (removed)

| Trace | Fate |
|---|---|
| Cord message | Replaced by the DEBUG trace of every message sent |
| Device registered or removed, cable plugged in or unplugged | Kept, in English, DEBUG, by the module |
| On load: summary | Kept (number of devices and cables, generated MACs) |
| On load: one line per device and per cable | Removed (unreadable on a large world; `/worldwideblocks links` exists) |
| Each player's indicator | Removed |
| Placing a block | Removed (the device registration covers it) |
| Learned and forgotten MACs every second | Removed |

`debug/TrialLog` and `NetworkManager.graphForTrial()` disappear.

### Tests

- The module keeps, for the running server, a list of listeners. A test subscribes and receives every message sent: channel, level, key, values, recipient (none for the log), game time. Nothing goes through a player's chat; `TestPlayer` no longer captures messages. The tests of a lot share the server: each filters what it captures by its test player or its devices, and the loop test stays in its own lot.
- The tests of the per-minute limit do not disappear, they follow the limit: the three engine tests on the loop (including the exact gap of 1,200 ticks) become "one event per network and per tick" on the engine side and "one message per minute, exactly" on the module side; the game test keeps its exact instants (0, 1,200, 2,400 ticks), with a single series instead of one per box.
- Unit tests (plain Java):
  - channel choice by use and level;
  - repeat limit: same key within the minute refused, accepted at exactly 1,200 ticks, different keys independent;
  - engine: two boxes in a loop give a single event, which names both boxes;
  - panel: compact and detailed for the box and the computer, "(+N)";
  - language files: same keys in French and English, maximum lengths respected.
- Game tests: the cord tests check their messages through the listener (action bar, recipient); the loop test checks **a single** chat message per minute for two boxes, and a single WARN line.

### Code

Data separated from drawing, so that a future HUD can replace the drawing without touching the rest:

- `message/` in plain Java, without any Minecraft class: levels, uses, channels, values of a message (plain text, number, key to translate, device), message (level, key, values), routing, repeat limit, sent message (for the listeners), status panel lines.
- `message/server/MessageCenter`, carried by the server's data like the simulation: routing, repeat limit, sending, English log, listeners. No static state.
- `network/sync/MessagePayload`: channel, level, key and values, to the client. The server never sends text or formatting (only exception: the output of the commands of the terminal, section 3).
- `client/`: the only place that decides how it looks (symbol, colour, action bar or chat, panel drawing). It goes through the game's `ChatListener` (`gui.chatListener()`), which keeps the player's narration and chat settings.
- Command replies stay Minecraft command texts (exception above).

## 3. Terminal

Decided with Max on 2026-11-19, before lot 1c-1 (`docs/lot-plan.md`, "Decisions taken"). The terminal of a computer is a third way next to the messages above: it is not a message module channel, and it is the only place where the server sends written text.

The console stream carries two kinds of lines, kept strictly apart:

| Kind | Examples | Who writes the text | Language |
|---|---|---|---|
| Output of a command | Output of `ping`, `ip`, later `tcpdump` and `dhclient`; errors of the tools and of the shell (`Destination Host Unreachable`, `bash: foo: command not found`); the prompt | The server, in plain Java, from the real state of the machine and of the network: never a prepared result | Literal English, in the format of the real tool; never translated |
| Line of the game | `help`, advice after a failure, texts of the title bar ("Ctrl+C: stop", how to close) | The client, from a key and values, like every other text of the mod | Both language files |

- The output of a command is what a real Linux would show for the same situation, so that the player can recognise it, and search for it, outside the game. It is built in plain Java and tested in JUnit, like `StatusPanel`.
- A line of the game is always marked as such: the client draws it apart from the output (its look is chosen by the client, like the symbols of the levels). It never imitates the output of a tool, and no translated text is ever inserted into the output of a running command.
- Advice comes only after a command has ended, and only after a failure. A client setting turns it off.
- "Computer busy" answers a gesture (a right-click on a computer already in use). Proposal, to confirm at the start of lot 1c-1: it goes through `MessageCenter.gesture`, as an error, like the other gestures.
- The terminal output has no level and no repeat limit: it belongs to the session of the console, not to the message module.
