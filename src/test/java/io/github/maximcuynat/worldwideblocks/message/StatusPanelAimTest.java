package io.github.maximcuynat.worldwideblocks.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.message.StatusPanel.AimedPort;
import io.github.maximcuynat.worldwideblocks.message.StatusPanel.Light;
import io.github.maximcuynat.worldwideblocks.message.StatusPanel.Line;
import io.github.maximcuynat.worldwideblocks.message.StatusPanel.OtherEnd;
import io.github.maximcuynat.worldwideblocks.network.PortKind;

/** The line of the aimed port in the status panel (issue #62): first line, compact or detailed. */
class StatusPanelAimTest {
    private static final List<PortKind> BOX = List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE);
    /** Ports 1 and 3 (engine 0 and 2) have a cable. */
    private static final Set<Integer> CONNECTED = Set.of(0, 2);

    private static List<Line> lines(boolean detailed, Optional<AimedPort> aimed) {
        return StatusPanel.lines(BOX, CONNECTED::contains, null, detailed, 0, aimed);
    }

    private static Line line(Light light, String key, MessageArg... args) {
        return new Line(light, key, List.of(args));
    }

    @Test
    void withoutAnAimedPortThePanelIsUnchanged() {
        assertEquals(StatusPanel.lines(BOX, CONNECTED::contains, null, false, 0), lines(false, Optional.empty()));
        assertEquals(StatusPanel.lines(BOX, CONNECTED::contains, null, true, 0), lines(true, Optional.empty()));
    }

    @Test
    void aFreeAimedPortComesFirst() {
        List<Line> lines = lines(false, Optional.of(new AimedPort(1, Optional.empty())));
        assertEquals(line(Light.FREE, "hud.worldwideblocks.aim.free", MessageArg.number(2)), lines.getFirst());
        assertEquals(lines(false, Optional.empty()), lines.subList(1, lines.size()));
    }

    @Test
    void aConnectedAimedPortIsCompactUnlessSneaking() {
        Optional<AimedPort> aimed = Optional.of(new AimedPort(2, Optional.of(new OtherEnd(Optional.of("block.worldwideblocks.pc_tower"), 0))));
        assertEquals(line(Light.CONNECTED, "hud.worldwideblocks.aim.connected", MessageArg.number(3)), lines(false, aimed).getFirst());
        assertEquals(line(Light.CONNECTED, "hud.worldwideblocks.aim.connected_to", MessageArg.number(3),
                MessageArg.key("block.worldwideblocks.pc_tower"), MessageArg.number(1)), lines(true, aimed).getFirst());
    }

    @Test
    void theDeviceAtTheOtherEndMayBeOutOfSight() {
        Optional<AimedPort> aimed = Optional.of(new AimedPort(0, Optional.of(new OtherEnd(Optional.empty(), 3))));
        assertEquals(line(Light.CONNECTED, "hud.worldwideblocks.aim.connected_to_unseen", MessageArg.number(1), MessageArg.number(4)),
                lines(true, aimed).getFirst());
    }

    @Test
    void theAimedLineInputIsInactive() {
        assertEquals(line(Light.INACTIVE, "hud.worldwideblocks.port.line", MessageArg.number(5)),
                lines(false, Optional.of(new AimedPort(4, Optional.empty()))).getFirst());
    }

    @Test
    void aPortOutsideTheDeviceIsIgnored() {
        assertEquals(lines(false, Optional.empty()), lines(false, Optional.of(new AimedPort(7, Optional.empty()))));
    }
}
