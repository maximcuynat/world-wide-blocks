package io.github.maximcuynat.worldwideblocks.os.command.hostname;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.os.TerminalOutput;
import io.github.maximcuynat.worldwideblocks.os.process.Process;
import io.github.maximcuynat.worldwideblocks.os.process.ProcessContext;

/**
 * {@code hostname} of Debian (package {@code hostname} 3.25): writes the name of the machine. A name given to it is
 * refused, since the account is not the administrator; the messages and statuses are those seen on Debian 13 on
 * 2026-11-20 ({@code src/test/resources/linux-real-outputs.txt}, "bash hostname ..."). Its options are answered as
 * unknown (no domain name nor name service before lot 7b); the usage is written for the mod (the tool is GPL).
 */
public final class HostnameCommand implements Process {
    /** {@code usage()} of the tool ends with {@code exit(-1)}. */
    private static final int USAGE_STATUS = 255;
    private static final List<String> USAGE = List.of(
            "Usage: hostname                display host name");

    @Override
    public void start(ProcessContext context) {
        List<String> arguments = context.arguments().subList(1, context.arguments().size());
        TerminalOutput out = context.out();
        if (arguments.isEmpty()) {
            out.line(context.system().hostName());
            context.exit(0);
            return;
        }
        String first = arguments.getFirst();
        if (first.startsWith("--") && first.length() > 2) {
            out.line("hostname: unrecognized option '" + first + "'");
            USAGE.forEach(out::line);
            context.exit(USAGE_STATUS);
        } else if (first.length() > 1 && first.startsWith("-")) {
            out.line("hostname: invalid option -- '" + first.charAt(1) + "'");
            USAGE.forEach(out::line);
            context.exit(USAGE_STATUS);
        } else if (arguments.size() > 1) {
            USAGE.forEach(out::line);
            context.exit(USAGE_STATUS);
        } else {
            out.line("hostname: you must be root to change the host name");
            context.exit(1);
        }
    }
}
