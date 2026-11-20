package io.github.maximcuynat.worldwideblocks.capture;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.maximcuynat.worldwideblocks.capture.CaptureScenario.Step;
import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenario;
import io.github.maximcuynat.worldwideblocks.registry.ModItems;
import net.minecraft.world.level.GameType;

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
     * follows: the second message proved that a minute of traffic in the loop is reported. The traffic is the presence
     * frame of each computer, every 30 s; when issue #15 removes it, check that the loop still carries enough frames.
     */
    static final CaptureScenario LOOP = standard("loop", "two boxes in a loop: one message per minute, none once broken",
            new Step.BuildScene(TrialScenario.LOOP),
            new Step.Wait(SETTLE),
            new Step.OpenChat(),
            new Step.Shot("first_minute"),
            new Step.CloseScreen(),
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

    private static final Map<String, CaptureScenario> BY_NAME = List.of(BOX, BOX_HOSTS, CHAIN, LOOP, FULL_PORTS, DISTANCE,
            TOOLTIP, DHCP_ADDRESSES, DHCP_RENEWAL, DHCP_NO_BOX, DHCP_CONFLICT, DHCP_POOL_FULL, DHCP_SERVER_OFF, DHCP_RESTART,
            FAILURE_CHECK).stream()
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
