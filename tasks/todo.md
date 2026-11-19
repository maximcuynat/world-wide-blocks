# Session notes

Session notes only. Tasks are GitHub issues, grouped by milestone (one lot, one version): `scripts/dev status` and `docs/project-management.md`. The open tasks and the ideas that used to be here became issues on 2026-11-18 (`scripts/project/issues/`, created by `scripts/dev project sync`).

## Lot 1c-1: terminal and ping (session of 2026-11-19)

Branch `lot/1c-1`. Issues #10 to #16, #30 (closed) and #52.

Step 0 (done, commit a25b047): findings of the prototype (#30) applied to `docs/lot-plan.md` and to the issue files 1c/02 and 1c/08; real Linux outputs copied to `src/test/resources/linux-real-outputs.txt` without the `docker pull` lines.

Facts checked for the two decisions (sources fetched on 2026-11-19):

- iproute2 `ip/iproute.c:878`: `proto` is printed unless it is `boot` (hidden without `-d`); `lib/rt_names.c` and `etc/iproute2/rt_protos`: 16 = `dhcp`. `ip/ipaddress.c`: `dynamic` printed for an address without `IFA_F_PERMANENT`, lifetimes as `valid_lft %usec`. systemd-networkd `src/network/networkd-dhcp4.c:390`: DHCP routes get `RTPROT_DHCP`, metric from `dhcp_route_metric`. Netplan (Ubuntu): default renderer networkd. Debian 13 release notes: `dhcpcd-base` replaces `isc-dhcp-client` for ifupdown. Debian `isc-dhcp` `debian/dhclient-script.linux:250-253, 277`: `ip -4 addr add <address>/<mask> ... valid_lft <lease> preferred_lft <lease>` and `ip -4 route add default via <router> dev <interface>` without `proto` (so `boot`, hidden) and without metric for a single router. All of the above read in the fetched files, not run. Not found: a real output line with `proto dhcp` or with `dynamic`.
- RFC 1812, 4.3.3.1: no route at all, default included: the router MUST send Destination Unreachable code 0 (Network Unreachable). Linux `net/ipv4/route.c:976` `ip_error`: silent when forwarding is off, `ICMP_NET_UNREACH` for `ENETUNREACH` (no route). iputils `ping/ping.c:1297` "Destination Net Unreachable", `ping/ping_output.c:50` `From %s icmp_seq=%u `. Not found: what a consumer box does with its line down.

Decisions of Max (2026-11-19), recorded in `docs/lot-plan.md` ("Decisions taken"):

- `ip addr` and `ip route` in the format of `dhclient` on Debian (the real reference output): no `proto dhcp`, no metric; the Docker check (dnsmasq and `dhclient`) moves to lot 1c-2 (issue file 1c-2/02).
- The box answers `Destination Net Unreachable` (code 0) to a destination outside its local network, RFC 1812 4.3.3.1; no rate limit on ICMP errors.
- #52 before #11. The closed laptop stays on the network until lot 1e. "Computer busy" through `MessageCenter.gesture`, as an error. Without the presence frame, MACs leave the box panel between 300 and 600 s (accepted; lot 1b procedures and #52 scenarios updated). Issue file 1c/07: capture scenario `terminal` instead of a `runClient` procedure.

Plan approved by Max. One commit per step after Max's green build (`./gradlew build test`, `./gradlew runGameTestServer`); every existing test that changes is shown to Max before and after. Every Minecraft or NeoForge name checked in the 26.3 sources (file and line) at its step.

- [x] 0. Findings of #30 applied to the docs (a25b047); decisions recorded (docs commit after the approval)
- [x] 1. IPv4 stack per interface and `ip addr`, `ip link`, `ip route` (#12): green build reported by Max on 2026-11-19 (263 unit tests, 30 game tests)
  - [x] `Ipv4Packet` gets a `ttl` (64 by default)
  - [x] `network/ip/`: interfaces `lo` and `eth0` (MAC, MTU, carrier, address with lease lifetime), routing table (`proto kernel` route of the local network, default route), UDP demultiplexing by port
  - [x] DHCP client reading before the IP layer, like `dhclient` through its packet socket (deviation from the plan, told to Max): a lease sets the address and the routes, losing it removes them; the box gets its own stack (192.168.1.1 on its service MAC), DHCP server bound to UDP 67
  - [x] Plain-Java console core: session, shell, command registry, processes advanced by the tick, cap per machine
  - [x] `ip addr` (`a`, `show eth0`), `ip link`, `ip route`, errors of iproute2 read in its sources
  - [x] Output tests against the format of `src/test/resources/linux-real-outputs.txt`; DHCP tests still green
Review of step 1 (2026-11-19):

- Checked for real: Max's `./gradlew build test` (263 unit tests, 0 failure) and `./gradlew runGameTestServer` (30 game tests); before that, the plain-Java code compiled and tested locally with JDK 25 and JUnit 5.13.4. No existing test changed; one test added to `DhcpClientTest` (mask not contiguous).
- Outputs compared with the real reference, each adaptation named in `IpCommandTest`; lines the reference cannot show (`dynamic`, lifetimes, `NO-CARRIER`) taken from the sources of iproute2 6.15.0 and of the kernel, not from a real machine.
- Choices reported to Max: usage texts written for the mod (iproute2 is GPL-2.0-or-later), changes answered with EPERM without checking arguments, unsupported real objects and options answered as unknown, `fq_codel` to check on a real machine, address removed at unplugging (to check at lot 1c-2), bash fork message from memory.

- [x] 2. ARP (#13): RFC 826 packet, neighbour table with the Linux states and fixed delays, pending packets, box answers for 192.168.1.1, DHCP renewal through ARP, `ip neigh`: green build reported by Max on 2026-11-19 (286 unit tests, 30 game tests)
Review of step 2 (2026-11-19):

- Checked for real: Max's `./gradlew build test` (286 unit tests, 0 failure) and `./gradlew runGameTestServer` (30 game tests). Existing tests only got the new `schedule` method of the stack context (`Ipv4StackTest`, `ConsoleSessionTest`); no assertion changed.
- Neighbour table read in `net/core/neighbour.c` and `net/ipv4/arp.c`; 17 unit tests to the tick. A bug found by them: the third unicast probe left as a broadcast (Linux sends the probe before counting it, `neigh_probe`).
- `ip neigh` compared with the real reference (sections "ip neigh" and "ip neigh after").
- Choices reported to Max: `REACHABLE` fixed at 30 s (Linux: 15 to 45 s); entries listed in creation order; `get` and `flush` unknown; in the `chain` scene the renewal at T1 goes to the nearer box (same 192.168.1.1) and gets no answer, the broadcast at T2 extends the lease (simulation test, no game test reaches T1).
- [ ] 3. ICMP, `ping` and latency (#14): echo and destination unreachable (original packet quoted), box with routing table and code 0, latency carried by each delivery and inherited by replies, `ping` and `ping -c n` in the iputils formats, advice line after a failure
- [ ] 4. Permanent capture runs (#52): `capture` source set absent from the jar (checked by a task), `runClientCapture -Pscenario=`, throwaway `run-capture/`, world created by the run, values file, exit code; scenarios of lots 1b-1, 1b-2 and the message module
- [ ] 5. Terminal screen (#11): gestures, keyboard owner, checked packets, screen with Monocraft, own text size, shell commands, language keys; capture scenario `terminal`
- [ ] 6. Presence frame removed (#15)
- [ ] 7. Ping game tests (#16)

## Tooling: Projects board automation (session of 2026-11-19)

Branch `chore/project-board`. Design approved by Max on 2026-11-19.

Facts read from the real board (`gh project field-list` and `item-list` run by Max): Status has the options `No status` (a real option, not the empty value), `Todo`, `In Progress`, `Done`; 37 items, all issues of this repository, 17 in `No status` (the ideas), 20 in `Todo`.

Decisions:

- `scripts/dev [--dry-run] project board [--start n,m] [--review n,m]`. Board owner and number in `scripts/project/board.conf` (`owner=`, `number=`); repository from `origin`.
- Reads with `gh api graphql`: the project and its Status field (`repositoryOwner(login:) { ... on ProjectV2Owner { projectV2(number:) } }`), then the issues of the repository with their `projectItems` and `fieldValueByName(name: "Status")`, paginated. Only issues of this repository are touched.
- Options: keep the GitHub names `Todo`, `In Progress`, `Done`; create `Ideas` and `In review`. Wanted order Ideas, Todo, In Progress, In review, Done, then any other existing option (here `No status`) in its current order. `updateProjectV2Field` replaces the whole list: every existing option is sent again with its `id`, `color` and `description`; the command stops if one would be missing. The field is only updated when an option is missing. Options re-read after the update, before moving items.
- Sorting, first rule that applies: closed issue -> Done; `--start` -> In Progress, `--review` -> In review; open issue missing from the board -> added; `type: idea` with no status or `Todo` -> Ideas (the label decides); no status -> Todo; any other status is left alone. The option `No status` counts as no status.
- `lot start`: after creating the branch, asks on the terminal which open issues of the milestone go to In Progress (numbers, `all`, empty = none; no terminal = none). Board not configured or gh not ready: warning only.
- `lot finish`: after `gh pr create`, the issues of its `Closes #` list go to In review. A board failure after the push is a warning, not an error. Done stays with GitHub's built-in workflows; the "Pull request linked to issue" workflow is turned off (it would set In Progress when the pull request opens).
- `project update-issues`: ticks are merged instead of skipping the issue. A box is ticked if it is ticked in the file or on GitHub (matched by the text of the line), never unticked; the comparison ignores box states; an issue whose GitHub body has a ticked box missing from the file is still skipped.
- CLAUDE.md, section "End of every task": issue moved forward, boxes ticked in the definition file only when done and proved, follow-up comment written in English in `build/issue-comments/<n>.md`, one block of commands for Max (`project board --start/--review`, `project update-issues`, `gh issue comment <n> --body-file …`), `tasks/todo.md` updated.
- First real run on a throwaway copy of the board (`gh project copy`), then on board #4.

Not verified, to watch during the run on the copy: that the built-in Status field accepts `updateProjectV2Field` and keeps item values for options sent with their `id` (the `id` input exists since the schema of 2026-04-23); that the order of the list gives the order of the columns; what `gh project copy` copies (workflows, items); whether the target status of the built-in workflows can be changed.

Steps, one commit each, tested in a throwaway clone with a fake `gh` (GraphQL state in JSON) and a pseudo-terminal:

- [x] 1. `scripts/lib/board.sh`, `scripts/project/board.conf`, `scripts/dev project board`
- [x] 2. `lot start` and `lot finish` hooks
- [x] 3. Tick merge in `project update-issues`
- [x] 4. `docs/project-management.md` and `CONTRIBUTING.md`
- [x] 5. CLAUDE.md "End of every task"

Review (2026-11-19):

- Checked only in throwaway clones, with a fake `gh` in Python (GraphQL operations recognised by name, state in JSON built from Max's real export of board #4, `--jq` passed to the real `jq` per page, pages of 20 issues to exercise `--paginate`; `updateProjectV2Field` modelled from the documentation: an option sent without its id is new, items on a dropped option lose their value), a fake `gradlew`, a bare origin behind the GitHub URL, and a pseudo-terminal to answer the questions.
- `project board`: dry run on the real data (2 options to create, 2 issues to add, 19 moves, the 7 closed issues of lot 1b-2 left off the board); no terminal (answer no, nothing sent); yes (options created with the existing ids kept, statuses set by hand kept: In Progress on #12, Done on an open issue); second run with nothing to do; `--start` and `--review`, closed issue given to `--start` (left to Done), unknown number, number in both lists, bad lists, unknown argument; gh without the `project` scope, wrong board number, no Status field, no `board.conf`; option update refused by the API (nothing else sent); an existing option without id (stopped before sending).
- `lot start`: dry run (plan line), answer with commas, spaces, `#` and an unknown number, `all`, empty answer, no terminal (branch created, board unchanged), gh without scope (warning and the command to run later), no `board.conf` (warning). `lot finish`: dry run (plan line), real run with `--except` (the 7 issues closed by the pull request moved to In review), gh without scope after the push (warning, exit 0).
- `update-issues`: box ticked on GitHub only (now "=", was skipped), ticks on both sides merged, `[X]`, CRLF body, ticked line reworded in the file (skipped, line listed), real run then second run with nothing to do.
- Not checked: anything against the real GitHub API. First real run on a copy of the board (`gh project copy`), commands given to Max; the points listed as not verified above stay open until then.

## Lot 1b-2: DHCP (session of 2026-11-18)

Issues #3 to #9, branch `lot/1b-2`. Architecture approved by Max on 2026-11-18 (`docs/lot-plan.md`, lot 1b, will hold the design and the status at the end of the lot). Protocol checked in the text of RFC 2131 and RFC 2132.

Decisions:

- Layers `Ipv4Packet` > `UdpDatagram` > `DhcpMessage` in `network/protocol/`; DHCP logic in plain Java in `network/dhcp/`, plugged into the box's local point and into each computer port.
- The DHCP server has its own MAC (`service_mac`, saved, generated for boxes of lot 1b-1 worlds); leases saved in an optional field of `network.dat`; a box removed or replaced forgets everything.
- Deviations from RFC 2131 approved: no random +/-1 s on retransmissions (4.1); REQUEST without answer retransmitted at 4, 8, 16, 32 s then back to INIT (3.1); INIT-REBOOT without answer goes back to INIT (4.4.2 allows keeping the address); renewal unicast to the server MAC taken from its ACK (no ARP before lot 1c); no DECLINE, RELEASE or ARP probe; lease in seconds in the message (1,200 s = 24,000 ticks); offered address kept 60 s; full pool: no offer, oldest expired lease reused first (2.2).
- Two boxes share 192.168.1.1: the conflict is detected by a computer that receives two offers from different source MACs for its xid; warning to the players within 32 blocks, at most once per minute and per network.
- "DHCP server enabled" setting on the box, saved, on by default, no interface in this lot. The `chain` and `loop` scenes turn off the server of the second box; the conflict has its own scene `dhcp_conflict`. The expiry and full pool game tests use the setting.
- Restart game test: reread `network.dat` into a fresh storage and run that simulation; no public method added for tests (package visibility).
- Panel: remaining lease time in real minutes and seconds; box detailed panel lists the active leases, 4 lines at most then "(+N)".
- Presence frame kept until issue #15: a bound computer is silent until T1 (600 s) while the switch forgets a MAC after 300 s.
- Every existing test that changes: show Max what it checked before and after.

Steps, one commit each after Max's green build:

- [x] 1. IPv4, UDP and DHCP layers (#3)
- [x] 2. DHCP server, service MAC, local point of the box, "server enabled" setting (#4)
- [x] 3. DHCP client and its timers (#5)
- [x] 4. Saved leases and INIT-REBOOT (#6)
- [x] 5. Conflict between two servers (#8)
- [x] 6. Status panel, language keys (#7)
- [x] 7. Game tests, scenes, check procedure and status in `docs/lot-plan.md` (#9)

Review of lot 1b-2 (2026-11-18):

- Checked for real: each step built and tested by Max (`./gradlew build test`, `./gradlew runGameTestServer`), commits made only after; the plain-Java code also compiled and tested locally before each handover; the 6 simulation tests and 2 loop tests that changed were shown to Max with what they checked before and after.
- Check by eye done by Max on 2026-11-18 ("Trials DHCP" world, game in French), analysed from the screenshots and the log: points 1, 2, 3, 7 and 8 passed; points 4, 5 and 6 passed for what the screenshots show, the rest covered by tests (table in `docs/lot-plan.md`); points 9 (restart) and 10 (loop) not seen by eye, resting on the game tests, as decided by Max.
- Not checked: the client display after a real quit and reopen of a world (point 9); the absence of a conflict message in the `loop` scene in game (point 10, proved at engine level only); the addresses of the `chain` computers in game (unit test only). The package-private access of the `gametest` source set (`NetworkTestAccess`) works at run time: 30 game tests green.
- Choices made by Claude and reported to Max: the conflict message names the nearest box (two devices do not fit in a chat line), French names of the DHCP states, colour of the waiting light, the four DHCP scenes.
- Documentation commits of the same session: results of going public; plan revised by Max (lot 1e, 6c split, ideas as issues); TCP lot 1f, shared console and terminal decisions.

## Blocking for publication

- [x] **Rename the mod before making the repository public.** The former name of the mod was also the name of a British company working in the same field (networking, internet security): risk of a name conflict. Max looked for another name; the rename (display name, `mod_id`, package, resources, GitHub repository, docs) was left to a dedicated session. Nothing is made public, nor published on CurseForge or Modrinth, before. (decided on 2026-11-18) Done on 2026-11-18: renamed World Wide Blocks (session C).

## Done in the renaming session (session C)

- [x] Translate the existing content into English: `docs/lot-plan.md`, `docs/messages.md`, `docs/network-equipment.md`, `docs/repository-security.md`, `tasks/`, `CLAUDE.md`, the code and its comments, the hook messages. Max's goal: a repository entirely in English. Since session B, everything new is already in English (commits, changelog, templates, labels, issues, new documents). (decided on 2026-11-18) Done on 2026-11-18 (session C).

## Session C: rename to World Wide Blocks, repository in English, listing texts (2026-11-18)

Plan and decisions approved by Max on 2026-11-18: display name World Wide Blocks, mod id `worldwideblocks`, package and Maven group `io.github.maximcuynat.worldwideblocks`, repository and Modrinth and CurseForge address `world-wide-blocks` (free on both, checked through the Modrinth API and by Max in a browser for CurseForge). No migration of existing worlds. README and SECURITY in English only; the electricity lot becomes plain lot E; subcommands and scenes in English; docs renamed `lot-plan.md`, `network-equipment.md`, `repository-security.md`; branch `chore/rename-and-english`. Short command alias `/wwb` (no vanilla command of that name in the 26.3 sources). Max takes care of his own worlds in `run/saves`.

One commit per step, code commits only after Max's green build:

- [x] C1 `refactor!`: rename (packages, resources, keys, `gradle.properties`, saved data `data/worldwideblocks/network.dat` checked in `SavedDataStorage`, client config, commands, `/wwb` alias redirected to the node Brigadier keeps, checked in Brigadier 1.3.11; the refusal game test runs both forms). Build, 123 unit tests, 17 game tests green (Max).
- [x] C2: code comments, test names and messages in English, by 4 agents in parallel with a glossary. Checked by a script that compares the Java token streams without comments with the previous commit (only consistent renames and test messages allowed, plus two exception messages listed one by one), and a search for renames that collide with an existing name. Build and tests green (Max).
- [x] C3: `links`, `test clear`, scenes `box_hosts`, `chain`, `loop`, `full_ports`, trial world prefix "Trials"; the game test refuses "Test Level" (the game test server's world, `GameTestServer`) explicitly. Build and tests green (Max).
- [x] `scripts/dev project update-issues` (asked by Max): rewrites issue bodies from their files, dry run with a diff per issue, confirmation, issues with ticked boxes skipped. Tried in a throwaway clone with a fake `gh` (dry run, CRLF bodies, ticked box, no terminal, yes in a pseudo-terminal, second run with nothing to do).
- [x] C4: design documents translated and renamed, links updated, lot E, `SECURITY.md` and language notes of `CLAUDE.md` and `CONTRIBUTING.md`.
- [x] C5: `tasks/` translated; historical lines that named the old identity reworded without it.
- [x] C6 README rewritten for discoverability; C7 `docs/listing.md`; C8 `docs/going-public.md`.

Final checks (2026-11-18, tracked files only):

- Old name, case-insensitive: only two old entries of `CHANGELOG.md`; no path.
- Accents, French quotes, em dash (outside `fr_fr.json` and `CHANGELOG.md`): only the angle quotation marks of `gradlew`, Gradle's own wrapper script, left as generated.
- French words: only inside three source URLs.

Review of session C:

- Checked for real: build, unit and game tests after C1, C2 and C3 (Max); JSON files, resource references, package declarations; Java logic unchanged (script); local links of every document.
- Not checked: the check by eye in a new "Trials" world (asked from Max at the end of the session); `scripts/dev project update-issues` against the real `gh` (first run with `--dry-run` by Max, after the merge); `gh repo view` field names and the summary limits of Modrinth and CurseForge, marked "(to verify)" in the docs; whether `gh repo rename` updates the `origin` remote.
- Choices made by Claude and reported to Max: class names `WorldWideBlocks*`, the 16 topics and the description, Modrinth categories `technology` and `utility`, CurseForge Technology and Education, British spelling in the docs, the milestone selected by the end of its title so the command needs no accented letter.
- Left to Max, in order: check by eye, push and pull request, merge, then `docs/going-public.md`.

## Session B: project management on GitHub and assisted commands (2026-11-18)

Design approved by Max on 2026-11-18: `developement` as default branch ("Closes #" only works towards it), branches `lot/<id>`, `release/v<version>` and `hotfix/<version>`, signed tag set on the merge commit of `main` by `release publish`, roadmap from 0.1.0 to 1.0.0 in the order of `lot-plan.md`, milestones for 0.1.0 and 0.2.0 only, the rest of this file turned into issues (ideas included, rename excluded), notes of 0.1.0-alpha.1 rewritten by hand (review and edit planned in `release publish`). Nothing is created on GitHub by Claude: Max runs the scripts.

One commit per subject (in English), tested in a throwaway clone with fake `gradlew` and `gh`:

- [x] 1. `cliff.toml` in English, "Breaks existing worlds" section first, `CHANGELOG.md` regenerated. Tried on a history with tags on the merge commits of `main`: a lot commit dated before a version but merged after it lands in the next version, as it should, `docs!:` has its section. Found on the way: without `--github-repo`, the comparison links of the changelog stayed empty (`release.sh` of session A); `scripts/dev` passes the repository read from the remote.
- [x] 2. Pre-push hook: Gradle output in `build/pre-push.log`, last 40 lines on failure, code in `scripts/lib/checks.sh` (5 cases tried).
- [x] 3. GitHub templates: bug issue, idea issue (`config.yml` without blank issue), pull request with "Closes #" and the "Breaks existing worlds" box.
- [x] 4. `scripts/dev`: base, `status`, `lot start`, `lot finish`.
- [x] 5. `scripts/dev`: `release prepare`, `release publish`, `hotfix start`; `scripts/release.sh` removed.
- [x] 6. `scripts/dev project sync` and its data: 16 labels, 10 milestones, 28 issues (7 + 7 + 3 for lots 1b-2, 1c, 1d; 4 tasks from this file; 7 ideas).
- [x] 7. `CONTRIBUTING.md` rewritten in English, `docs/project-management.md`, `docs/repository-security.md` (section 4 and table), sentence at the top of `README.md`.
- [x] 8. `CLAUDE.md` (language, branches, tool, issues), this file reduced to session notes, pointer in `docs/lot-plan.md`.

Review of session B (2026-11-18):

- Checked only in throwaway clones, with a fake `gradlew`, a fake `gh` (state in JSON, `--jq` passed to the real `jq`), a throwaway SSH signing key and a pseudo-terminal to answer the questions: each command in dry run (no git change, no GitHub write), without a terminal (answer no), answer no, checks failing, answer yes; empty case (no tag, issue, milestone or pull request); full cycle alpha, beta (freeze, `--allow-features`), release (version milestone closed), fixes 0.1.1 to 0.1.3 (breaking change refused, carry-over with merge commit, conflict on `CHANGELOG.md` kept on the `developement` side, other conflict stopped); `project sync` run again: no write on the second pass.
- Not checked: the real `gh` (milestones API, label renaming, gojq `capture` in `--jq`), the automations of the Projects board, signing with Max's key, the Gradle build in a fresh worktree (duration of the first build), Gradle at all (no mod code touched). First real try of each command by Max with `--dry-run`.
- Choices made by Claude, reported to Max: colours and descriptions of the labels, `type: idea` label in addition to the five requested types, `--allow-features` option, release notes kept in `build/release/` between the dry run and the real publication, release always created as a draft, fix possible only while the version to fix is the last one on `main`, clone URL replaced by an example in `CONTRIBUTING.md` (rename to come).
- Left to Max, in order: GitHub settings of `docs/project-management.md` (default branch, merges, Projects board and its automations), then `scripts/dev project sync --dry-run` and `scripts/dev project sync`, then `scripts/dev status`.
- Follow-up (2026-11-18, done by the maintainer): first push of the rewritten pre-push hook green (real build, unit and game tests); default branch `developement` (`gh repo edit --default-branch`); merge commits only, automatic branch deletion (`gh repo edit`), default merge message checked; "Roadmap" board (#4) linked with `gh project link 4 --owner <login>` (`--owner @me` refused), Status field and workflows set in the interface; `scripts/dev project sync`: 54 changes, issues #1 to #28, second run "everything is up to date". Commands that ask for an answer must run in a real terminal. Docs updated accordingly; `CLAUDE.md` rewritten in English for this organisation. Session B closed.

## Session A: clean, secure and equipped repository (2026-11-18)

Audit of the history done and approved by Max on 2026-11-18: no secret (gitleaks, 57 commits), authors with GitHub's masked address, no unwanted file ever committed, no local path. `CLAUDE.md`, `tasks/` and the mentions of Max stay public. No change to the mod code, no GitHub Actions.

Decisions of Max: LGPL-3.0 license, holder "maximcuynat"; SemVer version `0.1.0-alpha.1` in `gradle.properties` (only source), channel deduced from the suffix (alpha, beta, nothing = release), tag `v0.1.0-alpha.1`, jar `<mod_id>-26.3-0.1.0-alpha.1.jar`; `main` only receives published versions, through a pull request from `developement` merged with a merge commit; the 0.1.0-alpha.1 tag may become a GitHub pre-release, CurseForge and Modrinth wait for the end of lot 1; pre-push hook: tests skipped only if all modified files are `.md` or in `docs/`; gitleaks missing: warning.

One commit per subject, after Max's green build (Gradle does not run in Claude's sandbox):

- [x] 1. Gradle wrapper regenerated as 9.2.1 with the archive checksum (`distributionSha256Sum`). The current jar is an official 8.14.x wrapper, authentic but outdated. Command run by Max, then check: SHA-256 of the jar = `423cb469…` (official 9.2.1 wrapper), `build test` green (Max, 2026-11-18). Line endings set in `.gitattributes` (`gradlew` in LF, `*.bat` in CRLF, `*.jar` binary). The checksum is only checked when the distribution is downloaded: the 9.2.1 distribution already in the cache was not rechecked.
- [x] 2. `.gitignore` completed (`.env`, logs, screenshots, crash reports, macOS and editor files); `git ls-files -ci --exclude-standard` stays empty.
- [x] 3. LGPL-3.0 license: `COPYING.LESSER` (LGPL) and `COPYING` (GPL, which the LGPL refers to), `mod_license=LGPL-3.0-only`. `TEMPLATE_LICENSE.txt` (MIT of the MDK) kept. Build green, `license="LGPL-3.0-only"` in the jar (Max, 2026-11-18). Hooks installed and gitleaks checked on Max's machine.
- [x] 4. Version `0.1.0-alpha.1` and jar name with the Minecraft version. Build and game tests green (FML accepts the version) and `<mod_id>-26.3-0.1.0-alpha.1.jar` produced, `version="0.1.0-alpha.1"` in the jar (Max, 2026-11-18).
- [x] 5. Versioned hooks (`.githooks/`) and `scripts/install-hooks.sh`: commit messages (replayed on the 57 existing subjects), gitleaks before commit, build and game tests before push (except `.md` and `docs/` only; clean tree required). Pre-push hook tested in a throwaway clone with a fake `gradlew` (11 scenarios). The pre-commit hook also blocks when gitleaks reports an error: in a trial, gitleaks answered "no leaks found" while git had failed. Installed and checked on Max's machine.
- [x] 6. Releases: `cliff.toml` (Keep a Changelog in French at the time: Added, Changed, Fixed), `CHANGELOG.md`, `scripts/release.sh` (`--dry-run` mode, signed commit and tag, confirmation before any push, GitHub release as a draft). Full flow tried in a throwaway clone with fake `gradlew` and `gh` (refusal, atomic push, pull request, safeguards of `--publish`, draft as a pre-release, no terminal = no). First real try: `scripts/release.sh --dry-run 0.1.0-alpha.1` by Max.
- [x] 7. `README.md` (French and English), `SECURITY.md` (private report through GitHub), `CONTRIBUTING.md` (commits, branches, hooks, tests, releases).
- [x] 8. `docs/repository-security.md`: GitHub settings for Max to make, interface paths, order of operations, SSH signing of commits and tags.
- [x] 9. `CLAUDE.md`: hooks, release script, branch model. Review of the session below.

Review of session A (2026-11-18):

- Done: audit, 10 commits on `developement` (masked identity, unsigned), all after a green build by Max or with no effect on the build (hooks, docs, release tooling).
- Really checked: official 9.2.1 wrapper, build and tests, license and version in the jar, game tests with `0.1.0-alpha.1`, gitleaks hook on Max's machine. Checked only in throwaway clones: pre-push hook (fake `gradlew`), `scripts/release.sh` (fake `gradlew` and `gh`, signing with a throwaway key). Max's first push will be the first real run of the pre-push hook.
- Left to Max: `scripts/release.sh --dry-run 0.1.0-alpha.1` (first real try of the script), SSH signing then GitHub settings in the order of `docs/repository-security.md`, choice 3.8 (signing Claude's commits) before requiring signed commits on `main`.
- Blocking before making the repository public: rename of the mod (dedicated session).
- Choices made by Claude and reported to Max: `LGPL-3.0-only` rather than `-or-later`, `COPYING` and `COPYING.LESSER` files instead of a `LICENSE` (check GitHub's license badge after the push), commitments of `SECURITY.md` (latest version only, no guaranteed delay) and fork then pull request path of `CONTRIBUTING.md`.
- GitHub project management (labels, milestones, board): session B.

## Step 1: dev environment

- [x] Choose the loader and the Minecraft version: NeoForge 26.3.0.103 on Minecraft 26.3 (the mod must first work alone)
- [x] JDK 25: downloaded automatically by Gradle at the first build (foojay toolchain), the Homebrew Java 17 is enough to run Gradle 9.2.1
- [x] Generate the project from the official MDK-26.3-ModDevGradle template
- [x] Set the mod identity (mod identity: the former id, package and name, replaced on 2026-11-18; version 0.1.0)
- [x] Initialise git and push to the private GitHub repository (commit 41eadcb, run by Max)
- [x] `./gradlew build` passes: BUILD SUCCESSFUL in 3 min 53 s, the jar produced in `build/libs/` (run by Max, Gradle cannot open a local socket in Claude's sandbox)
- [x] The game starts through `./gradlew runClient` (confirmed by Max on 2026-11-17). Test world: a creative world, superflat Redstone Ready. Presence of the mod in the Mods screen not explicitly confirmed.

## Blocks

- [x] PC tower (`pc_tower`): block, model, texture
- Issue: laptop (`laptop`), code and resources written on 2026-11-17, in-game check to do (`scripts/project/issues/0.1.0/01-laptop-in-game.md`).

## Lot plan

The full breakdown (lots, equipment, dependencies) and its approval status are in `docs/lot-plan.md`; roadmap of the versions in `docs/project-management.md`.

Inventory and real dimensions: `docs/network-equipment.md`.

- [x] Lot 1a (cable and connection): coded and approved in game on 2026-11-17 (procedure in `docs/lot-plan.md`).
- [x] Principle "real protocol exchanges" in `docs/lot-plan.md` and `CLAUDE.md` (commit 132fbc6).
- [x] Lot 1b-1 (box, MAC, switching): approved by Max on 2026-11-18 (state and test method in `docs/lot-plan.md`, lot 1b). Steps, one commit each:
  - [x] Typed ports (Ethernet, line) and MAC per port in the graph, save compatible with lot 1a worlds, tests (53 tests green)
  - [x] Frame layer: `MacAddress`, `EtherType`, `EthernetFrame`, typed content, tests
  - [x] Reusable switching: learning table with expiry, forwarding decision, tests (75 tests green)
  - [x] Simulation: delivery queue, timers, tap, loop detection and per-tick limit, computer and box logic, device role, tests (87 tests green)
  - [x] Minecraft connection: server tick, cable events, loop message
  - [x] Internet box block (7 x 7 x 3 px model, provisional texture, resources)
  - [x] Indicator: computer MAC, MACs learned by the box (status packet), green build, in-game trial to do
  - [x] Cord bug: the pending state stayed attached to a broken start point (reported by Max on 2026-11-17). Reproduced by `PendingCableTest`, fixed (cancel also on dimension change)
  - [x] Remove the temporary trial log lines of lot 1b-1 (`debug/TrialLog` and its calls): done at step 5 of the message module; the useful log lines go through `Diagnostics` (DEBUG, English)
  - [x] Check build of c178668 and 5014267 (committed without a build, see `tasks/lessons.md`): green on 5014267 on 2026-11-18
  - [x] Automatic game tests and trial scenes: design approved by Max on 2026-11-18. One commit per step, only after a green build:
    - [x] Source set `src/gametest/` excluded from the jar, wired to the runs; `gameTestServer` run in `run-gametest/` (ignored by git); `./gradlew build` also compiles this source set
    - [x] Empty 16 x 4 x 16 structure, connected test player (modelled on NeoForge's `GameTestPlayer`, messages captured), cleanup of each test's devices at its end, passed or not
    - [x] Tests of the common batch (green on 2026-11-18): start destroyed (break and explosion, creative and survival, cord count), dimension change, disconnection and reconnection, MAC expiry, save and reload
    - [x] Separate batch (dedicated environment) for what touches the engine's global limits: loop and limit of one report per minute
    - [x] Engine clock (requested by Max on 2026-11-18): a change that happened between two ticks took the time of the previous tick, the loop was reported every 90 s instead of 60. Changes applied at the start of the next tick, unit tests of the clock, game tests of the loop (exact gap) and of the expiry (exact bounds) tightened
    - [x] `/worldwideblocks test <scene>` and `/worldwideblocks test clear` (scenes tested in game, check by eye done by Max on 2026-11-18): development only (`gametest` source set), refused outside a world whose name starts with "Trials", refused if the area is not empty
    - [x] Automatic test of the refusal outside a trial world (name rule and real command), second computer on the right box of the `chain` scene (2 MAC on port 1)
  - [x] In-game test procedure (what needs eyes) and state of the lot in `docs/lot-plan.md`
  - [x] Check by eye of the message module: done by Max on 2026-11-18 (game in French, a trial world), no crash, rendering approved as the base of the future HUD (lot F). Table of the 6 points in `docs/lot-plan.md`.
  - [x] Points without a capture (detailed panel of a computer and of the laptop, right box of the `chain` scene, 99-block cable along its whole length, loop messages stopping after a cable is removed, `/worldwideblocks links`, rendering of ✔ and ℹ, fallback `messageSymbols = false`): seen by eye by Max during the same session, approved on his word on 2026-11-18
  - Issue: lot F (HUD interface), rendering on several window sizes (`scripts/project/issues/backlog/02-hud-window-sizes.md`).
  - [x] Message module, before 1b-2: audit and design approved by Max on 2026-11-18 (`docs/messages.md`), 7 steps coded and committed the same day after green builds, check by eye done the same day (result in `docs/lot-plan.md`). Steps:
    - [x] 1. Core in plain Java (`message/`): levels, uses, channels, values, routing, repeat limit, sent message and listener; unit tests of routing and of the repeat limit. No visible change.
    - [x] 2. Transport and display: `MessagePayload` (channel, level, key, values), `MessageCenter` carried by the server data, display decided by the client (symbol or word according to the `messageSymbols` setting), cord messages migrated, game tests of the cord through the listener. `TestPlayer` still captures the chat for the loop test, until step 4.
    - [x] 3. Texts: formal address in French, whole distances, duplicate and `result.ok` removed, common device format, cord tooltip, test of the language files (same keys, maximum lengths).
    - [x] 4. Loop and saturation: one event per network and per tick, per-minute limit in the module, forgotten when the network changes, message to the device closest to the player, saturation to the operators and to the owner in single player; tests of the engine, of the module and in game (a single series of messages for two boxes).
    - [x] 5. Log in English and cleanup: useful log lines passed through the module (DEBUG, English), `TrialLog` and `graphForTrial` removed, exceptions in English.
    - [x] 6. Status panel: lines built in plain Java (compact, detailed when sneaking, "Sneak: details" line), the client only draws; unit tests.
    - [x] 7. Documents: `CLAUDE.md` (log in English, module rules, data separated from drawing), state in `docs/lot-plan.md`, check-by-eye procedure.
- Lot 1b-2 (DHCP): architecture approved with 1b-1, not started. Tasks: issues of the milestone "Lot 1b-2: DHCP" (INIT-REBOOT included).

## Multiplayer tests

Became issues on 2026-11-18: end of lot 1 (milestone "v0.1.0", `scripts/project/issues/0.1.0/02-multiplayer-dedicated-server.md`) and two players towards the end of lot 3 (`scripts/project/issues/backlog/01-multiplayer-two-players.md`). Meanwhile, keep the client and server split strict in the code (see the rules in `CLAUDE.md`).

## Loose ideas

Became `type: idea` issues on 2026-11-18 (`scripts/project/issues/ideas/`). Max's vision: a Packet Tracer made of blocks, faithful to reality, complete, for everyone and for networking enthusiasts. "Computers: PCs to place and cable" is done (lots 0 and 1a) and has no issue.

## Facts checked on 2026-11-17

- Latest Minecraft version: 26.3 (2026-09-15), Java 25. 1.21.1 needs Java 21.
- NeoForge: builds 26.3.0.x available, official templates MDK-26.3-ModDevGradle and MDK-1.21.1-ModDevGradle.
- NeoForge builds on Modrinth:
  - 1.21.1: Mekanism, Create, AE2, CC:Tweaked, Powah, Energized Power
  - 26.3: AE2, CC:Tweaked, Powah, Energized Power (no Mekanism, no Create)
- Immersive Engineering stops at 1.21.1, Thermal Expansion at 1.20.1 (Forge only).
- NeoForge 21.9 (Minecraft 1.21.9) reworked the transfer APIs (energy, items, fluids) and FML: a port from 1.21.1 to 26.x touches the energy layer.
- Machine: Homebrew Java 17 (not enough), VS Code present, IntelliJ absent.

## Docs: automated checks (session of 2026-11-19)

Branch `docs/automated-checks`, from `developement`.

- [x] 1. CLAUDE.md, section "Automated checks", details in the new `docs/automated-checks.md` (also listed in "Read first"); "Constraints of the sandbox" adjusted
- [x] 2. Issue definition `scripts/project/issues/1c/09-permanent-capture-runs.md` (type: feature, area: tooling, priority: high, lot 1c-1)
- [x] 3. `CONTRIBUTING.md` ("Run the tests") and `docs/project-management.md` ("Where things live")

Review: documentation only, nothing to build. The capture run exists only in the uncommitted prototype of the terminal screen, so the docs say that the `runClient` procedures of `docs/lot-plan.md` apply until the new issue is done. Left on purpose for that issue: the "Test method" paragraph and the eye check procedures of lots 1b-1 and 1b-2 in `docs/lot-plan.md`. After the merge: `scripts/dev project sync` creates the issue.
