package io.github.maximcuynat.worldwideblocks.os.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** History of a Debian 13 account: {@code HISTCONTROL=ignoreboth}, {@code HISTSIZE=1000} ({@code /etc/skel/.bashrc}). */
class ShellHistoryTest {

    @Test
    void ignorebothLeavesOutRepeatsAndLinesStartingWithASpace() {
        ShellHistory history = new ShellHistory();

        assertTrue(history.add("hostname"));
        assertFalse(history.add("hostname"), "same as the previous entry");
        assertFalse(history.add(" hostname"), "starts with a space");
        assertFalse(history.add("   "), "spaces only start with a space");
        assertFalse(history.add(""));
        assertTrue(history.add("ip a"));
        assertTrue(history.add("hostname"), "not the previous entry any more");

        assertEquals(List.of("    1  hostname", "    2  ip a", "    3  hostname"), history.listing(Integer.MAX_VALUE));
        assertEquals(List.of("    3  hostname"), history.listing(1));
        assertEquals(List.of(), history.listing(0));
    }

    @Test
    void theOldestEntriesGoBeyondTheSizeAndTheOthersKeepTheirNumber() {
        ShellHistory history = new ShellHistory();
        for (int i = 1; i <= ShellHistory.MAX_ENTRIES + 2; i++) {
            history.add("command " + i);
        }

        assertEquals(ShellHistory.MAX_ENTRIES, history.entries().size());
        assertEquals(3, history.base());
        assertEquals(List.of(" 1002  command 1002"), history.listing(1));
    }

    @Test
    void clearingStartsTheNumbersAgain() {
        ShellHistory history = new ShellHistory();
        history.add("a");
        history.add("b");
        history.clear();
        history.add("c");

        assertEquals(List.of("    1  c"), history.listing(10));
    }
}
