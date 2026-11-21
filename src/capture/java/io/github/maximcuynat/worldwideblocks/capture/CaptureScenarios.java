package io.github.maximcuynat.worldwideblocks.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.github.maximcuynat.worldwideblocks.capture.CaptureScenario.Step;
import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenario;
import io.github.maximcuynat.worldwideblocks.registry.ModItems;
import net.minecraft.client.CameraType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;

/**
 * The capture scenarios, by name ({@code ./gradlew runClientCapture -Pscenario=<name>}). Each one replaces a check by
 * eye of {@code docs/lot-plan.md}.
 */
public final class CaptureScenarios {
    private static final int AUTO_SCALE = 0;
    /** Ticks for the first exchanges of a new scene (DHCP, frames learned by the boxes) and their payloads. */
    private static final int SETTLE = 40;
    /** One game minute: the loop message is repeated at most once per minute. */
    private static final int MINUTE = 1200;
    /**
     * Five game seconds: a {@code ping} to an absent address prints its first {@code Destination Host Unreachable}
     * lines after three; a {@code ping -c 3} has ended.
     */
    private static final int FIVE_SECONDS = 100;
    /** The left tower of the scene {@code box_hosts} (device 1; 0 is the box, 2 the laptop, 3 the right tower). */
    private static final int TOWER_LEFT = 1;
    /** A reply of {@code ping}, from the address given as a regular expression. */
    private static final String REPLY = "64 bytes from %s: icmp_seq=[123] ttl=64 time=[0-9.]+ ms";
    /** The statistics of {@code ping -c 3} without loss. */
    private static final String NO_LOSS = "3 packets transmitted, 3 received, 0% packet loss, .*";
    /** Half a lease of one game day: 600 s, the renewal time T1. */
    private static final int HALF_LEASE = 12_000;

    /**
     * Lot 1b-1, scene {@code box}: the compact panel ("0/4 ports connected"), then the detailed one (ports 1 to 4
     * free, port 5, the line, inactive), the box facing the player.
     */
    static final CaptureScenario BOX = new CaptureScenario("box", "box alone, compact then detailed panel",
            CaptureScenario.STANDARD_SIZES, List.of(AUTO_SCALE), CaptureScenario.BOTH_LANGUAGES, List.of(
                    new Step.BuildScene(TrialScenario.BOX),
                    new Step.Aim(0, false),
                    new Step.Shot("compact"),
                    new Step.Aim(0, true),
                    new Step.Shot("detailed")));

    /**
     * Lot 1b-1, scene {@code box_hosts}, and point 2 of the message module: the box with 3 computers, compact ("3/4
     * ports connected") then detailed (3 ports with the MAC learned, port 4 free); a computer compact then detailed;
     * then {@code /worldwideblocks links} in the chat (point 6: device names and whole-number lengths).
     */
    static final CaptureScenario BOX_HOSTS = standard("box_hosts", "box and 3 computers, panels, then the links command",
            new Step.BuildScene(TrialScenario.BOX_HOSTS),
            new Step.Wait(SETTLE),
            new Step.Aim(0, false),
            new Step.Shot("box_compact"),
            new Step.Aim(0, true),
            new Step.Shot("box_detailed"),
            new Step.Aim(1, false),
            new Step.Shot("computer_compact"),
            new Step.Aim(1, true),
            new Step.Shot("computer_detailed"),
            new Step.Command("worldwideblocks links"),
            new Step.OpenChat(),
            new Step.Shot("links"),
            new Step.CloseScreen());

    /**
     * Lot 1b-1, scene {@code chain}, point 3 of the message module and point 4 of lot 1b-2: port 1 of the left box
     * shows one MAC and "(+1)", and the left box lends the 3 addresses; the right box, its ports with one MAC each and
     * "DHCP server: off"; each computer of the right box has an address; no DHCP conflict in the chat.
     */
    static final CaptureScenario CHAIN = standard("chain", "two linked boxes, one server: panels, chat without conflict",
            new Step.BuildScene(TrialScenario.CHAIN),
            new Step.Wait(SETTLE),
            new Step.Aim(0, true),
            new Step.Shot("left_box_detailed"),
            new Step.Aim(1, true),
            new Step.Shot("right_box_detailed"),
            new Step.Aim(3, false),
            new Step.Shot("right_laptop_compact"),
            new Step.Aim(4, false),
            new Step.Shot("right_tower_compact"),
            new Step.OpenChat(),
            new Step.Shot("chat"),
            new Step.CloseScreen());

    /**
     * Lot 1b-1, scene {@code loop}, point 5 of the message module and point 10 of lot 1b-2: a single loop message in
     * the chat and no DHCP conflict (the server of the right box is off), then a single other one a game minute later
     * ({@code values.txt} lists the lines that reached the chat between two shots). Then the server removes one of the
     * two cables between the boxes, and the run fails if any line reaches the chat during the minute and a half that
     * follows: the second message proved that a minute of traffic in the loop is reported. The traffic is a continuous
     * {@code ping} of the computer to an absent address, typed in its terminal after the first message: an ARP request
     * broadcast every second (issue #15 removes the presence frame); the shot {@code ping_running} shows it, and it
     * goes on, screen closed, until the end of the run.
     */
    static final CaptureScenario LOOP = standard("loop", "two boxes in a loop: one message per minute, none once broken",
            new Step.BuildScene(TrialScenario.LOOP),
            new Step.Wait(SETTLE),
            new Step.OpenChat(),
            new Step.Shot("first_minute"),
            new Step.CloseScreen(),
            new Step.OpenTerminal(2),
            new Step.Type("ping 192.168.1.250"),
            new Step.Press(Step.TerminalKey.ENTER),
            new Step.Wait(FIVE_SECONDS),
            new Step.Shot("ping_running"),
            new Step.Press(Step.TerminalKey.ESCAPE),
            new Step.Wait(MINUTE),
            new Step.OpenChat(),
            new Step.Shot("second_minute"),
            new Step.CloseScreen(),
            new Step.RemoveCable(0, 1),
            new Step.Silence(MINUTE + MINUTE / 2),
            new Step.OpenChat(),
            new Step.Shot("loop_broken"),
            new Step.CloseScreen());

    /**
     * Lot 1b-1, scene {@code full_ports}, and points 1 and 6 of the message module: cord in hand, a right-click on the
     * box whose 4 ports are used shows "✖ Internet Box: no free port." in red above the hotbar; with the client option
     * {@code messageSymbols} off, "Error: ..." instead. The option is put back at the end.
     */
    static final CaptureScenario FULL_PORTS = standard("full_ports", "cord on a full box, with and without symbols",
            new Step.BuildScene(TrialScenario.FULL_PORTS),
            new Step.Wait(SETTLE),
            new Step.Hold(ModItems.ETHERNET_CABLE),
            new Step.Aim(0, false),
            new Step.Shot("no_free_port", true),
            new Step.MessageSymbols(false),
            new Step.Shot("no_free_port_without_symbols", true),
            new Step.MessageSymbols(true));

    /**
     * Lot 1b-1, scene {@code distance}, and point 4 of the message module: the cable of 99 blocks seen from above each
     * end towards the other (it goes on to the horizon), then each end with its panel, the laptop also detailed. The
     * computers have no box: no address, the DHCP client searches.
     */
    static final CaptureScenario DISTANCE = standard("distance", "cable of 99 blocks, both ends",
            new Step.BuildScene(TrialScenario.DISTANCE),
            new Step.Wait(SETTLE),
            new Step.View(0, 2, 3, 3, 1),
            new Step.Shot("cable_from_the_tower"),
            new Step.View(1, 2, 3, -3, 0),
            new Step.Shot("cable_from_the_laptop"),
            new Step.Aim(0, false),
            new Step.Shot("tower_compact"),
            new Step.Aim(1, false),
            new Step.Shot("laptop_compact"),
            new Step.Aim(1, true),
            new Step.Shot("laptop_detailed"));

    /**
     * Point 1 of the message module: the tooltip of the cord in the inventory, 3 grey lines. In survival mode: in
     * creative, the game replaces the inventory by the creative one ({@code InventoryScreen.init}), where the cord
     * would be hovered in the tab of the mod instead.
     */
    static final CaptureScenario TOOLTIP = standard("tooltip", "tooltip of the cord in the inventory",
            new Step.Hold(ModItems.ETHERNET_CABLE),
            new Step.GameMode(GameType.SURVIVAL),
            new Step.HoverHeldItem(),
            new Step.Shot("cord"),
            new Step.CloseScreen());

    /**
     * Check of the run itself: a shot changes the language, the window size and the GUI scale, the client option
     * {@code messageSymbols} is changed, then a step fails on purpose (no device 9 in the scene). The run must end with
     * {@code failed: ...}, the command must fail, and the settings must be back (log lines "put back",
     * {@code run-capture/options.txt}, {@code run-capture/config/worldwideblocks-client.toml}).
     */
    static final CaptureScenario FAILURE_CHECK = new CaptureScenario("failure_check",
            "a step fails on purpose after the settings changed", CaptureScenario.STANDARD_SIZES, List.of(AUTO_SCALE, 2),
            CaptureScenario.BOTH_LANGUAGES, List.of(
                    new Step.BuildScene(TrialScenario.BOX),
                    new Step.Aim(0, false),
                    new Step.Shot("before_failure"),
                    new Step.MessageSymbols(false),
                    new Step.Aim(9, false)));

    /**
     * Lot 1b-2, points 1 and 2, scene {@code box_hosts}: a computer with its address, compact ("192.168.1.1x (DHCP:
     * bound)", green light) then detailed (MAC, address and mask, gateway, name server, time left on the lease, state);
     * the box detailed, with the addresses it lent. Then, empty hand, sneak and use on the computer: the end of its
     * cable comes into the hand and the computer shows "Not connected"; use on it again with that end: it is plugged
     * again and gets the same address.
     */
    static final CaptureScenario DHCP_ADDRESSES = standard("dhcp_addresses",
            "addresses of the computers, unplugged and plugged again",
            new Step.BuildScene(TrialScenario.BOX_HOSTS),
            new Step.Wait(SETTLE),
            new Step.Aim(1, false),
            new Step.Shot("computer_compact"),
            new Step.Aim(1, true),
            new Step.Shot("computer_detailed"),
            new Step.Aim(0, true),
            new Step.Shot("box_detailed"),
            new Step.Hold(null),
            new Step.Aim(1, true),
            new Step.Use(),
            new Step.Aim(1, false),
            new Step.Shot("computer_unplugged"),
            new Step.Use(),
            new Step.Wait(SETTLE),
            new Step.Shot("computer_plugged_again"));

    /**
     * Lot 1b-2, point 3, scene {@code box_hosts}: the time left on the lease of a computer (about 20:00), then 600 s of
     * game run at once ({@code /tick sprint 600s}, half the lease): the time left is about 20:00 again, not 10:00,
     * because the lease was renewed at half time on the way. The game reports the sprint in the chat.
     */
    static final CaptureScenario DHCP_RENEWAL = standard("dhcp_renewal", "lease renewed at half time during a sprint",
            new Step.BuildScene(TrialScenario.BOX_HOSTS),
            new Step.Wait(SETTLE),
            new Step.Aim(1, true),
            new Step.Shot("before_sprint"),
            new Step.Sprint(HALF_LEASE),
            new Step.Shot("after_sprint"));

    /**
     * Lot 1b-2, point 5, scene {@code dhcp_no_box}: both computers "No address (DHCP: selecting)", yellow light
     * ({@code [waiting]} in {@code values.txt}).
     */
    static final CaptureScenario DHCP_NO_BOX = standard("dhcp_no_box", "two computers without a box",
            new Step.BuildScene(TrialScenario.DHCP_NO_BOX),
            new Step.Wait(SETTLE),
            new Step.Aim(0, false),
            new Step.Shot("tower_compact"),
            new Step.Aim(1, false),
            new Step.Shot("laptop_compact"));

    /**
     * Lot 1b-2, point 6, scene {@code dhcp_conflict}: a single yellow message in the chat, "⚠ Two DHCP servers on the
     * network of Internet Box [x, y, z]. Keep only one."; the computer still has an address. The English log line that
     * names both boxes is in {@code run-capture/logs/latest.log}. A line of the chat keeps the language loaded when it
     * arrived: the scene is built a second time once the shots left French loaded, for the French message (a new
     * network, so the once-per-minute limit does not hold it back).
     */
    static final CaptureScenario DHCP_CONFLICT = standard("dhcp_conflict", "two DHCP servers: one message, an address",
            new Step.BuildScene(TrialScenario.DHCP_CONFLICT),
            new Step.Wait(SETTLE),
            new Step.OpenChat(),
            new Step.Shot("chat"),
            new Step.CloseScreen(),
            new Step.Aim(2, false),
            new Step.Shot("computer_compact"),
            new Step.ClearScene(),
            new Step.BuildScene(TrialScenario.DHCP_CONFLICT),
            new Step.Wait(SETTLE),
            new Step.OpenChat(),
            new Step.Shot("chat_received_in_french"),
            new Step.CloseScreen());

    /**
     * Lot 1b-2, point 7, scene {@code dhcp_pool_full}: the computer "No address (DHCP: selecting)"; the box detailed,
     * "Addresses lent: 191", four lines then "+187 more".
     */
    static final CaptureScenario DHCP_POOL_FULL = standard("dhcp_pool_full", "box with no address left, one computer",
            new Step.BuildScene(TrialScenario.DHCP_POOL_FULL),
            new Step.Wait(SETTLE),
            new Step.Aim(1, false),
            new Step.Shot("computer_compact"),
            new Step.Aim(0, true),
            new Step.Shot("box_detailed"));

    /**
     * Lot 1b-2, point 8, scene {@code dhcp_server_off}: the box detailed, "DHCP server: off"; the computer without
     * address.
     */
    static final CaptureScenario DHCP_SERVER_OFF = standard("dhcp_server_off", "box with its DHCP server off",
            new Step.BuildScene(TrialScenario.DHCP_SERVER_OFF),
            new Step.Wait(SETTLE),
            new Step.Aim(0, true),
            new Step.Shot("box_detailed"),
            new Step.Aim(1, false),
            new Step.Shot("computer_compact"));

    /**
     * Lot 1b-2, point 9, scene {@code box_hosts}: the address of each computer, then save and quit to the title screen
     * and open the world again: each computer shows the same address, state "bound", once the world is open.
     */
    static final CaptureScenario DHCP_RESTART = standard("dhcp_restart", "same addresses after quitting and reopening",
            new Step.BuildScene(TrialScenario.BOX_HOSTS),
            new Step.Wait(SETTLE),
            new Step.Aim(1, false),
            new Step.Shot("tower_left_before"),
            new Step.Aim(2, false),
            new Step.Shot("laptop_before"),
            new Step.Aim(3, false),
            new Step.Shot("tower_right_before"),
            new Step.Restart(),
            new Step.Aim(1, false),
            new Step.Shot("tower_left_after"),
            new Step.Aim(2, false),
            new Step.Shot("laptop_after"),
            new Step.Aim(3, false),
            new Step.Shot("tower_right_after"));

    /**
     * Lot 1c-1, issue #11: the terminal of the tower then of the laptop, opened by a click as a player does (on the
     * keyboard of the laptop), at 854 x 480 and 1920 x 1080, with the automatic GUI scale and with scale 2: its text
     * size does not follow the GUI scale ({@code values.txt}: columns, rows, text size). The empty console and its
     * prompt; {@code help} (lines of the game, translated); {@code ip a}, whose line {@code lo} wraps; a larger text
     * (Ctrl+=, the option put back by the run); a running {@code ping} ("Ctrl+C: stop" in the title), stopped by
     * Ctrl+C; Ctrl+C at the prompt then Ctrl+L; scrolled up (Shift+Page Up), then down by one notch of the wheel; back
     * to the bottom (Shift+Page Down), then up by one notch ({@code values.txt}: rows scrolled up); the history (Up);
     * Esc then a new opening (same session, the game time goes on: no pause); {@code exit} closes the screen; the
     * laptop, with accents typed.
     */
    static final CaptureScenario TERMINAL_SCREEN = new CaptureScenario("terminal_screen",
            "terminal of the tower and the laptop: prompt, help, wrap, text size, keys, Esc, exit",
            CaptureScenario.STANDARD_SIZES, List.of(AUTO_SCALE, 2), CaptureScenario.BOTH_LANGUAGES, List.of(
                    new Step.BuildScene(TrialScenario.BOX_HOSTS),
                    new Step.Wait(SETTLE),
                    new Step.OpenTerminal(1),
                    new Step.Shot("opened"),
                    new Step.Type("help"),
                    new Step.Press(Step.TerminalKey.ENTER),
                    new Step.Shot("help"),
                    new Step.Type("hostname"),
                    new Step.Press(Step.TerminalKey.ENTER),
                    new Step.Type("ip a"),
                    new Step.Press(Step.TerminalKey.ENTER),
                    new Step.Shot("ip_a"),
                    new Step.Press(Step.TerminalKey.CTRL_PLUS),
                    new Step.Shot("text_larger"),
                    new Step.Press(Step.TerminalKey.CTRL_ZERO),
                    new Step.Type("ping 192.168.1.1"),
                    new Step.Press(Step.TerminalKey.ENTER),
                    new Step.Wait(60),
                    new Step.Shot("ping_running"),
                    new Step.Press(Step.TerminalKey.CTRL_C),
                    new Step.Wait(10),
                    new Step.Shot("ping_stopped"),
                    new Step.Type("histo"),
                    new Step.Press(Step.TerminalKey.CTRL_C),
                    new Step.Press(Step.TerminalKey.CTRL_L),
                    new Step.Shot("cleared"),
                    new Step.Press(Step.TerminalKey.SHIFT_PAGE_UP),
                    new Step.Shot("scrolled_up"),
                    new Step.Scroll(-1),
                    new Step.Shot("wheel_down"),
                    new Step.Press(Step.TerminalKey.SHIFT_PAGE_DOWN),
                    new Step.Scroll(1),
                    new Step.Shot("wheel_up"),
                    new Step.Press(Step.TerminalKey.ESCAPE),
                    new Step.Wait(SETTLE),
                    new Step.OpenTerminal(1),
                    new Step.Press(Step.TerminalKey.UP),
                    new Step.Shot("reopened_history_up"),
                    new Step.Press(Step.TerminalKey.CTRL_C),
                    new Step.Type("exit"),
                    new Step.Press(Step.TerminalKey.ENTER),
                    new Step.Wait(SETTLE),
                    new Step.OpenTerminal(2),
                    new Step.Type("é è ç à ù ê ë œ ~ @"),
                    new Step.Shot("laptop"),
                    new Step.Press(Step.TerminalKey.ESCAPE)));

    /**
     * Lot 1c-1, issue #11: an advice of the game after a failed command, drawn apart from the output (its colour and
     * symbol are a choice of taste of the maintainer), then hidden by the client option {@code terminalAdvice}, put
     * back by the run. {@code ping -c 2} to an address nobody has: ARP finds nobody, the advice of
     * {@code host_unreachable} follows the statistics.
     */
    static final CaptureScenario TERMINAL_ADVICE = standard("terminal_advice", "advice after a failed ping, shown then turned off",
            new Step.BuildScene(TrialScenario.BOX_HOSTS),
            new Step.Wait(SETTLE),
            new Step.OpenTerminal(1),
            new Step.Type("ping -c 2 192.168.1.250"),
            new Step.Press(Step.TerminalKey.ENTER),
            new Step.Wait(8 * 20),
            new Step.Shot("advice_shown"),
            new Step.TerminalAdvice(false),
            new Step.Shot("advice_off"),
            new Step.Press(Step.TerminalKey.ESCAPE));

    /**
     * Lot 1c-1, issue #16, check by eye of the lot: from the terminal of the left tower of {@code box_hosts}, each
     * command on a cleared screen (Ctrl+L): {@code ip a} (the address of its lease); {@code ping -c 3} to the box, three
     * replies; {@code ping -c 3} to the laptop, at the address of its lease read when the command is typed, three
     * replies: the goal of the lot, a computer that pings another one which answers; {@code ping -c 3} to an address
     * nobody has: three {@code Destination Host Unreachable} from the tower itself, the statistics, then the advice of
     * the game. Before each shot, the run fails unless the terminal shows what is expected.
     */
    static final CaptureScenario TERMINAL = standard("terminal", "ip a, then ping to the box, to the laptop and to an absent address",
            new Step.BuildScene(TrialScenario.BOX_HOSTS),
            new Step.Wait(SETTLE),
            new Step.OpenTerminal(TOWER_LEFT),
            new Step.Type("ip a"),
            new Step.Press(Step.TerminalKey.ENTER),
            new Step.Wait(SETTLE),
            new Step.Expect("\\s*inet {address:1}/24 brd 192\\.168\\.1\\.255 scope global dynamic eth0", 1),
            new Step.Shot("ip_a"),
            new Step.Press(Step.TerminalKey.CTRL_L),
            new Step.Type("ping -c 3 192.168.1.1"),
            new Step.Press(Step.TerminalKey.ENTER),
            new Step.Wait(FIVE_SECONDS),
            new Step.Expect(REPLY.formatted("192\\.168\\.1\\.1"), 3),
            new Step.Expect(NO_LOSS, 1),
            new Step.Shot("ping_box"),
            new Step.Press(Step.TerminalKey.CTRL_L),
            new Step.Type("ping -c 3 {address:2}"),
            new Step.Press(Step.TerminalKey.ENTER),
            new Step.Wait(FIVE_SECONDS),
            new Step.Expect(REPLY.formatted("{address:2}"), 3),
            new Step.Expect(NO_LOSS, 1),
            new Step.Shot("ping_computer"),
            new Step.Press(Step.TerminalKey.CTRL_L),
            new Step.Type("ping -c 3 192.168.1.250"),
            new Step.Press(Step.TerminalKey.ENTER),
            new Step.Wait(FIVE_SECONDS),
            new Step.Expect("From {address:1} icmp_seq=[123] Destination Host Unreachable", 3),
            new Step.Expect("3 packets transmitted, 0 received, \\+3 errors, 100% packet loss, .*", 1),
            new Step.Shot("ping_absent"),
            new Step.Press(Step.TerminalKey.ESCAPE));

    /**
     * Lot 1d-1, issue #62, scene {@code port_aim}: cord in hand, the aimed port of the box and of the switch, outlined and
     * named first in the panel, from the front and from above: a free port ("Port 2: free"), a connected one, compact then
     * detailed (sneaking: the device at the other end and its port); on the switch the zone on its top. Then with an empty
     * hand, sneaking: the port whose end would be taken off. Each aim fails the run unless the panel names that port first.
     */
    static final CaptureScenario PORT_AIM = new CaptureScenario("port_aim", "aimed port: outline and panel line on a box and a switch",
            List.of(new CaptureScenario.Size(1920, 1080)), List.of(AUTO_SCALE), CaptureScenario.BOTH_LANGUAGES, List.of(
                    new Step.BuildScene(TrialScenario.PORT_AIM),
                    new Step.Wait(SETTLE),
                    new Step.Hold(ModItems.ETHERNET_CABLE),
                    new Step.AimPort(0, 1, 2.5, false, false), new Step.Shot("box_port_2_front"),
                    new Step.AimPort(0, 0, 2.5, false, false), new Step.Shot("box_port_1_connected"),
                    new Step.AimPort(0, 0, 2.5, false, true), new Step.Shot("box_port_1_detailed"),
                    new Step.AimPort(0, 3, 1.2, false, false), new Step.Shot("box_port_4_above"),
                    new Step.AimPort(1, 2, 2.5, false, false), new Step.Shot("switch_port_3_front"),
                    new Step.AimPort(1, 4, 1.0, true, false), new Step.Shot("switch_port_5_above"),
                    new Step.AimPort(1, 0, 1.0, true, true), new Step.Shot("switch_port_1_detailed"),
                    new Step.Hold(null),
                    new Step.AimPort(0, 0, 2.5, false, true), new Step.Shot("box_take_end")));

    /**
     * Lot 1d-1, issue #63, scene {@code plugs}: the RJ45 plug in each port that has a cable and the thick cable leaving
     * from it: the box and the 5-port switch from the front, three-quarter and above; each turned device seen from the
     * side of its port (tower turned by 90 and 270 degrees, laptop by 180, 8-port switch by 90, box by 270); the whole
     * scene from above; the box again at night (cables and plugs darken with the world); then a cord started on port 4 of
     * the switch, the cable going from its plug to the hand, in first
     * and third person.
     */
    static final CaptureScenario PLUGS = new CaptureScenario("plugs", "plugs in their ports, thick cables, turned devices, held cable",
            List.of(new CaptureScenario.Size(1920, 1080)), List.of(AUTO_SCALE), List.of("en_us"), List.of(
                    new Step.BuildScene(TrialScenario.PLUGS),
                    new Step.Wait(SETTLE),
                    new Step.GameMode(GameType.SPECTATOR),
                    new Step.HideHud(true),
                    new Step.Orbit(0, 0, 20, 1.1), new Step.Shot("box_front"),
                    new Step.Orbit(0, 35, 30, 1.2), new Step.Shot("box_three_quarter"),
                    new Step.Orbit(1, 0, 25, 0.9), new Step.Shot("switch_front"),
                    new Step.Orbit(1, 325, 45, 1.0), new Step.Shot("switch_three_quarter"),
                    new Step.Orbit(2, 270, 25, 1.6), new Step.Shot("tower_90_port"),
                    new Step.Orbit(3, 270, 30, 1.3), new Step.Shot("laptop_180_port"),
                    new Step.Orbit(4, 90, 25, 1.6), new Step.Shot("tower_270_port"),
                    new Step.Orbit(5, 90, 30, 1.0), new Step.Shot("switch8_90_port"),
                    new Step.Orbit(6, 270, 25, 1.2), new Step.Shot("box_270_port"),
                    new Step.Orbit(0, 0, 55, 9.0, 3.0), new Step.Shot("scene"),
                    // At night the cables and the plugs darken with the world: they are lit by its light.
                    new Step.Command("time set midnight"),
                    new Step.Wait(20),
                    new Step.Orbit(0, 35, 30, 1.2), new Step.Shot("box_night"),
                    new Step.Command("time set noon"),
                    new Step.Wait(20),
                    new Step.HideHud(false),
                    new Step.GameMode(GameType.CREATIVE),
                    new Step.Hold(ModItems.ETHERNET_CABLE),
                    new Step.AimPort(1, 3, 2.0, false, false),
                    new Step.Use(),
                    new Step.Shot("held_cable"),
                    new Step.Camera(CameraType.THIRD_PERSON_BACK),
                    new Step.Shot("held_cable_third_person"),
                    new Step.Camera(CameraType.FIRST_PERSON)));

    // ---- Devices (issue #68): close-up views, the four orientations, the item ----

    /** One size and one language for the devices: the models carry no text. */
    private static final List<CaptureScenario.Size> DEVICE_SIZE = List.of(new CaptureScenario.Size(1920, 1080));

    /** The internet box: 7 x 3 x 7 px, seen from 1.1 blocks. */
    static final CaptureScenario DEVICE_BOX = device("device_box", "internet box: close-up views, four orientations",
            TrialScenario.DEVICE_BOX, ModItems.INTERNET_BOX::get, 1.1, List.of());

    /** The PC tower: 7 x 15 x 15 px, seen from 2 blocks. */
    static final CaptureScenario DEVICE_TOWER = device("device_tower", "PC tower: close-up views, four orientations",
            TrialScenario.DEVICE_TOWER, ModItems.PC_TOWER::get, 2.0, List.of());

    /** The laptop: 14 x 9 px, open (device 0) then closed (device 5), seen from 1.6 blocks. */
    static final CaptureScenario DEVICE_LAPTOP = device("device_laptop", "laptop: close-up views open and closed, four orientations",
            TrialScenario.DEVICE_LAPTOP, ModItems.LAPTOP::get, 1.6, closeUps(5, "closed_", 1.6));

    /** The 5-port desktop switch: 4 x 3 x 1 px, seen from 0.8 block (issue #17). */
    static final CaptureScenario DEVICE_SWITCH_5 = device("device_switch_5", "5-port switch: close-up views, four orientations",
            TrialScenario.DEVICE_SWITCH_5, ModItems.DESKTOP_SWITCH_5::get, 0.8, List.of());

    /** The 8-port desktop switch: 6.5 x 4 x 1 px, seen from 1 block (issue #18). */
    static final CaptureScenario DEVICE_SWITCH_8 = device("device_switch_8", "8-port switch: close-up views, four orientations",
            TrialScenario.DEVICE_SWITCH_8, ModItems.DESKTOP_SWITCH_8::get, 1.0, List.of());

    /**
     * The views of a device: the close-ups of device 0 (and the extra steps, a second device), HUD hidden, in spectator
     * mode; then the four orientations of devices 1 to 4 seen from above the row; then each of them aimed, HUD shown, as
     * the game draws the selection outline only with the HUD and on the aimed block; then the item: large with its
     * inventory settings, in hand in first and third person (from above, in creative mode), on the ground and in an
     * item frame (in spectator mode, HUD hidden).
     */
    private static CaptureScenario device(String name, String description, TrialScenario scene,
            Supplier<? extends ItemLike> item, double distance, List<Step> extra) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step.BuildScene(scene));
        steps.add(new Step.GameMode(GameType.SPECTATOR));
        steps.add(new Step.HideHud(true));
        steps.addAll(closeUps(0, "", distance));
        steps.addAll(extra);
        steps.add(new Step.Orbit(2, 0, 40, 4.5, 1.0)); // devices 1 to 4 are 2 blocks apart: between devices 2 and 3
        steps.add(new Step.Shot("orientations"));
        steps.add(new Step.HideHud(false));
        steps.add(new Step.GameMode(GameType.CREATIVE));
        String[] turns = {"0", "90", "180", "270"};
        for (int i = 0; i < 4; i++) {
            steps.add(new Step.Aim(1 + i, false));
            steps.add(new Step.Shot("outline_" + turns[i]));
        }
        steps.add(new Step.ShowItem(item));
        steps.add(new Step.Shot("item_inventory"));
        steps.add(new Step.CloseScreen());
        steps.add(new Step.Hold(item));
        steps.add(new Step.Orbit(0, 0, 35, 3.0)); // high enough for the feet of a creative player to stay above the ground
        steps.add(new Step.Shot("item_first_person"));
        steps.add(new Step.Camera(CameraType.THIRD_PERSON_FRONT));
        steps.add(new Step.Shot("item_third_person"));
        steps.add(new Step.Camera(CameraType.FIRST_PERSON));
        steps.add(new Step.Hold(null));
        steps.add(new Step.GameMode(GameType.SPECTATOR));
        steps.add(new Step.HideHud(true));
        steps.add(new Step.DropItem(item, 0, 2));
        steps.add(new Step.Orbit(0, 0, 35, 2.0, 2.0));
        steps.add(new Step.Shot("item_ground"));
        steps.add(new Step.FrameItem(item, 0, -2));
        steps.add(new Step.Orbit(0, 0, 10, 2.0, -2.0));
        steps.add(new Step.Shot("item_frame"));
        return new CaptureScenario(name, description, DEVICE_SIZE, List.of(AUTO_SCALE), List.of("en_us"), steps);
    }

    /** Front, back, three-quarter front and back, top of one device. */
    private static List<Step> closeUps(int device, String prefix, double distance) {
        return List.of(
                new Step.Orbit(device, 0, 15, distance), new Step.Shot(prefix + "front"),
                new Step.Orbit(device, 180, 15, distance), new Step.Shot(prefix + "back"),
                new Step.Orbit(device, 40, 30, distance), new Step.Shot(prefix + "three_quarter_front"),
                new Step.Orbit(device, 220, 30, distance), new Step.Shot(prefix + "three_quarter_back"),
                new Step.Orbit(device, 0, 80, distance), new Step.Shot(prefix + "top"));
    }

    private static final Map<String, CaptureScenario> BY_NAME = List.of(BOX, BOX_HOSTS, CHAIN, LOOP, FULL_PORTS, DISTANCE,
            TOOLTIP, TERMINAL, TERMINAL_SCREEN, TERMINAL_ADVICE, DHCP_ADDRESSES, DHCP_RENEWAL, DHCP_NO_BOX, DHCP_CONFLICT, DHCP_POOL_FULL, DHCP_SERVER_OFF, DHCP_RESTART,
            DEVICE_BOX, DEVICE_TOWER, DEVICE_LAPTOP, DEVICE_SWITCH_5, DEVICE_SWITCH_8, PORT_AIM, PLUGS, FAILURE_CHECK).stream()
            .collect(Collectors.toUnmodifiableMap(CaptureScenario::name, Function.identity()));

    private CaptureScenarios() {
    }

    /** A scenario in the standard combinations: English and French, 854 x 480 and 1920 x 1080, automatic GUI scale. */
    private static CaptureScenario standard(String name, String description, Step... steps) {
        return new CaptureScenario(name, description, CaptureScenario.STANDARD_SIZES, List.of(AUTO_SCALE),
                CaptureScenario.BOTH_LANGUAGES, List.of(steps));
    }

    public static Optional<CaptureScenario> byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    /** The names, sorted, for the message of an unknown scenario. */
    public static List<String> names() {
        return BY_NAME.keySet().stream().sorted().toList();
    }
}
