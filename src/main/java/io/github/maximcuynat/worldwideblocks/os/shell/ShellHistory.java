package io.github.maximcuynat.worldwideblocks.os.shell;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The command history of the account, shared by the shells of the machine, as {@code ~/.bash_history} is: a new
 * shell after {@code exit} finds the lines of the previous one. Kept in server memory only, like the rest of the
 * operating system until the computers have a disk.
 *
 * <p>Rules of a Debian 13 account ({@code /etc/skel/.bashrc}, seen on 2026-11-20): {@code HISTCONTROL=ignoreboth}, so
 * a line that starts with a space and a line equal to the previous entry are not kept (a line of spaces only starts
 * with a space); an empty line is never kept; {@code HISTSIZE=1000}. Entries keep their number when older ones are
 * dropped, as in bash ({@code history_base}).
 */
public final class ShellHistory {
    /** {@code HISTSIZE} of {@code /etc/skel/.bashrc}. */
    public static final int MAX_ENTRIES = 1000;

    private final ArrayDeque<String> entries = new ArrayDeque<>();
    /** Number of the oldest entry kept. */
    private int base = 1;

    /** Keeps a typed line, unless {@code ignoreboth} leaves it out; true if it was kept. */
    public boolean add(String line) {
        if (line.isEmpty() || line.charAt(0) == ' ' || line.equals(this.entries.peekLast())) {
            return false;
        }
        this.entries.addLast(line);
        while (this.entries.size() > MAX_ENTRIES) {
            this.entries.removeFirst();
            this.base++;
        }
        return true;
    }

    /** {@code history -c}. Numbering starts again at 1, as in bash. */
    public void clear() {
        this.entries.clear();
        this.base = 1;
    }

    public List<String> entries() {
        return List.copyOf(this.entries);
    }

    /** Number of the oldest entry kept. */
    public int base() {
        return this.base;
    }

    /** The lines {@code history} writes for the last {@code count} entries: number on 5 columns, two spaces, the line. */
    public List<String> listing(int count) {
        List<String> all = new ArrayList<>(this.entries);
        int from = Math.max(0, all.size() - count);
        List<String> lines = new ArrayList<>(all.size() - from);
        for (int i = from; i < all.size(); i++) {
            lines.add(String.format(Locale.ROOT, "%5d  %s", this.base + i, all.get(i)));
        }
        return lines;
    }
}
