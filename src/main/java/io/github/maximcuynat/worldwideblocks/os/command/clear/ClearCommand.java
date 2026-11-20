package io.github.maximcuynat.worldwideblocks.os.command.clear;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.os.TerminalOutput;
import io.github.maximcuynat.worldwideblocks.os.process.Process;
import io.github.maximcuynat.worldwideblocks.os.process.ProcessContext;

/**
 * {@code clear [-x]} of ncurses: clears the screen and the lines that scrolled above it ({@code ESC[3J}), or the
 * screen only with {@code -x}. Its errors and statuses are those of ncurses 6.5 on Debian 13, seen on 2026-11-20
 * ({@code src/test/resources/linux-real-outputs.txt}, "bash clear ..."). {@code -T} and {@code -V} are answered as
 * unknown options; the usage lists only {@code -x}, in the words of ncurses (licence X11).
 */
public final class ClearCommand implements Process {
    private static final List<String> USAGE = List.of(
            "Usage: clear [options]",
            "",
            "Options:",
            "  -x          do not try to clear scrollback");

    @Override
    public void start(ProcessContext context) {
        boolean keepScrollback = false;
        List<String> arguments = context.arguments();
        TerminalOutput out = context.out();
        for (String argument : arguments.subList(1, arguments.size())) {
            if (argument.length() > 1 && argument.startsWith("-")) {
                for (char option : argument.substring(1).toCharArray()) {
                    if (option != 'x') {
                        out.line("clear: invalid option -- '" + option + "'");
                        USAGE.forEach(out::line);
                        context.exit(1);
                        return;
                    }
                    keepScrollback = true;
                }
            } else {
                USAGE.forEach(out::line);
                context.exit(1);
                return;
            }
        }
        out.clear(keepScrollback);
        context.exit(0);
    }
}
