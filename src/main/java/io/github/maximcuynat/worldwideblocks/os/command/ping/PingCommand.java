package io.github.maximcuynat.worldwideblocks.os.command.ping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.maximcuynat.worldwideblocks.network.Latency;
import io.github.maximcuynat.worldwideblocks.network.ip.IcmpSocket;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.protocol.IcmpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.os.TerminalOutput;
import io.github.maximcuynat.worldwideblocks.os.process.Process;
import io.github.maximcuynat.worldwideblocks.os.process.ProcessContext;

/**
 * {@code ping [-c count] <address>}, as iputils 20240905 runs it on Debian 13: real ICMP echo requests from a ping
 * socket of the machine, one per second, and the replies or the errors that really come back. Its main loop, read in
 * {@code ping_common.c} ({@code main_loop}, {@code pinger}, {@code __schedule_exit}, {@code finish}) on 2026-11-19:
 *
 * <ul>
 * <li>The first request leaves at once, then one per second ({@code interval} 1000 ms). On every wake-up, a request
 * due is sent first, then what arrived is read: an error that arrives with a request due is read after that request
 * (the {@code pipe} of the statistics counts it).</li>
 * <li>With {@code -c n}: no request after the {@code n}th; {@code ping} ends as soon as the replies and the errors
 * reach {@code n}, otherwise after a wait set when the last request leaves: twice the longest round trip, at least
 * one second, if a reply came, 10 s ({@code MAXWAIT}) if none did.</li>
 * <li>Ctrl+C ends it with its statistics. The {@code time} of the statistics runs from the start to the last request
 * sent. Exit status 0 if a reply came, 1 otherwise, 2 for an error before the first request.</li>
 * <li>A request that finds no route (the address lost meanwhile) is counted as sent and prints nothing
 * ({@code hard_local_error}: the error queue is empty, so {@code sendmsg} is not reported), as seen on Debian 13
 * ({@code linux-real-outputs.txt}, "ping route lost"); a request without reply prints nothing either.</li>
 * </ul>
 *
 * <p>Round trip: the difference between the latency carried by the reply and the one of the request
 * ({@link Latency}), both within the tick where they travel: the game time spent waiting (a request held while ARP
 * resolves its next hop over several seconds) is not counted. Game time ({@code 1 tick = 50 ms}) for the interval, the
 * waits and the elapsed time.
 *
 * <p>Differences with the real tool: only {@code -c}; the other options are answered as invalid. A destination that
 * is not written {@code a.b.c.d} is looked up as a name, and the machine has no name service yet (lot 7b): it gets the
 * error of a resolver that gets no answer. Several destinations (a source route on Linux, refused with
 * {@code ping: record route: Operation not permitted} in the real capture) get the usage. The usage is
 * written for the mod in the layout of iputils (its text is under the BSD licence and is not copied). No duplicate
 * reply can happen in the engine.
 */
public final class PingCommand implements Process {
    /** Bytes of data of each request ({@code DEFDATALEN}, 64 - 8). */
    static final int DATA_LENGTH = 56;
    /** One second between two requests. */
    static final long INTERVAL_TICKS = 20L;
    /** {@code MAXWAIT}: 10 s of waiting after the last request when no reply came. */
    static final long LINGER_TICKS = 10 * 20L;
    /** Errors of the arguments ({@code strtol_or_err}: {@code EXIT_FAILURE}). */
    static final int EXIT_ARGUMENT = 1;
    /** Errors before the first request ({@code error(2, ...)}, {@code usage}). */
    static final int EXIT_SETUP = 2;

    /** Usage written for the mod, in the layout of iputils, on standard error. */
    static final List<String> USAGE = List.of(
            "",
            "Usage",
            "  ping [options] <destination>",
            "",
            "Options:",
            "  <destination>      IPv4 address",
            "  -c <count>         number of replies to wait for",
            "  -h                 this help");

    private enum Wake {
        SEND, EXIT
    }

    private final Map<Integer, Long> sentLatency = new HashMap<>();
    private final Set<Integer> received = new HashSet<>();
    private Ipv4Address destination;
    private String hostname;
    private long count;
    private IcmpSocket socket;
    private long start;
    private long lastSent;
    private long nextSend;
    private long transmitted;
    private long replies;
    private long duplicates;
    private long errors;
    private long tripMin = Long.MAX_VALUE;
    private long tripMax;
    private double tripSum;
    private double tripSquares;
    private int pipe = -1;
    private boolean exitScheduled;
    private boolean finished;
    private boolean netUnreachable;
    private boolean hostUnreachable;

    @Override
    public void start(ProcessContext context) {
        TerminalOutput out = context.out();
        Optional<Integer> refused = this.readArguments(context.arguments(), out);
        if (refused.isPresent()) {
            context.exit(refused.get());
            return;
        }
        Ipv4Stack stack = context.system().stack();
        if (stack.sourceFor(this.destination).isEmpty()) {
            out.line("ping: connect: Network is unreachable");
            out.advise("terminal.worldwideblocks.advice.ping.no_route", this.destination.toString());
            context.exit(EXIT_SETUP);
            return;
        }
        if (stack.isBroadcast(this.destination)) {
            out.line("ping: Do you want to ping broadcast? Then -b. If not, check your local firewall rules");
            context.exit(EXIT_SETUP);
            return;
        }
        out.line(PingOutput.header(this.hostname, this.destination, DATA_LENGTH));
        this.socket = context.openPingSocket();
        this.start = context.now();
        this.nextSend = this.start;
        this.pinger(context);
    }

    @Override
    public void onTimer(ProcessContext context, Object key) {
        if (this.finished) {
            return;
        }
        if (key == Wake.EXIT) {
            this.finish(context);
        } else {
            this.pinger(context);
        }
    }

    @Override
    public void onReceive(ProcessContext context, IcmpSocket from, IcmpSocket.Received message) {
        if (this.finished) {
            return;
        }
        this.pinger(context);
        switch (message.message()) {
            case IcmpMessage.Echo echo when echo.reply() -> this.reply(context, message, echo);
            case IcmpMessage.Unreachable error when error.original().payload() instanceof IcmpMessage.Echo echo ->
                    this.error(context, message, error, echo.sequence());
            default -> {
                return;
            }
        }
        if (this.count > 0 && this.replies + this.errors >= this.count) {
            this.finish(context);
        }
    }

    @Override
    public void interrupt(ProcessContext context) {
        if (!this.finished) {
            this.finish(context);
        }
    }

    // ---- Arguments ----

    /** {@code getopt} on {@code "h?...c:..."}, then the destination; the exit status if {@code ping} stops there. */
    private Optional<Integer> readArguments(List<String> argv, TerminalOutput out) {
        List<String> operands = new ArrayList<>();
        boolean onlyOperands = false;
        for (int i = 1; i < argv.size(); i++) {
            String word = argv.get(i);
            if (onlyOperands || !word.startsWith("-") || word.equals("-")) {
                operands.add(word);
                continue;
            }
            if (word.equals("--")) {
                onlyOperands = true;
                continue;
            }
            for (int j = 1; j < word.length(); j++) {
                char option = word.charAt(j);
                if (option == 'c') {
                    String value;
                    if (j + 1 < word.length()) {
                        value = word.substring(j + 1);
                    } else if (i + 1 < argv.size()) {
                        value = argv.get(++i);
                    } else {
                        out.line("ping: option requires an argument -- 'c'");
                        return usage(out);
                    }
                    Optional<Integer> refused = this.readCount(value, out);
                    if (refused.isPresent()) {
                        return refused;
                    }
                    break;
                } else if (option == 'h' || option == '?') {
                    return usage(out);
                } else {
                    out.line("ping: invalid option -- '" + option + "'");
                    return usage(out);
                }
            }
        }
        if (operands.isEmpty()) {
            out.line("ping: usage error: Destination address required");
            return Optional.of(EXIT_SETUP);
        }
        if (operands.size() > 1) {
            return usage(out);
        }
        String target = operands.getFirst();
        Optional<Ipv4Address> address = Ipv4Address.parse(target);
        if (address.isEmpty()) {
            // getaddrinfo with a resolver that gets no answer: EAI_AGAIN (as captured on Debian 13 without DNS).
            out.line("ping: " + target + ": Temporary failure in name resolution");
            out.advise("terminal.worldwideblocks.advice.ping.no_name_service");
            return Optional.of(EXIT_SETUP);
        }
        this.destination = address.get();
        this.hostname = target;
        return Optional.empty();
    }

    /** {@code strtol_or_err(optarg, "invalid argument", 1, LONG_MAX)}. */
    private Optional<Integer> readCount(String value, TerminalOutput out) {
        String digits = value.stripLeading();
        boolean valid = digits.matches("[+-]?[0-9]+");
        if (!valid) {
            out.line("ping: invalid argument: '" + value + "'");
            return Optional.of(EXIT_ARGUMENT);
        }
        long parsed;
        try {
            parsed = Long.parseLong(digits.startsWith("+") ? digits.substring(1) : digits);
        } catch (NumberFormatException overflow) {
            // strtol sets ERANGE; error() appends strerror(ERANGE).
            out.line("ping: invalid argument: '" + value + "': Numerical result out of range");
            return Optional.of(EXIT_ARGUMENT);
        }
        if (parsed < 1) {
            out.line("ping: invalid argument: '" + value + "': out of range: 1 <= value <= " + Long.MAX_VALUE);
            return Optional.of(EXIT_ARGUMENT);
        }
        this.count = parsed;
        return Optional.empty();
    }

    private static Optional<Integer> usage(TerminalOutput out) {
        USAGE.forEach(out::line);
        return Optional.of(EXIT_SETUP);
    }

    // ---- Main loop ----

    /** {@code pinger}: sends the request that is due, if one is. */
    private void pinger(ProcessContext context) {
        if (this.count > 0 && this.transmitted >= this.count) {
            return;
        }
        if (context.now() < this.nextSend) {
            return;
        }
        int sequence = (int) ((this.transmitted + 1) & 0xFFFF);
        this.sentLatency.put(sequence, context.latency());
        this.received.remove(sequence);
        context.system().stack().sendEcho(context.machine().network(), this.socket, this.destination, sequence, DATA_LENGTH);
        this.transmitted++;
        this.lastSent = context.now();
        this.nextSend = context.now() + INTERVAL_TICKS;
        context.schedule(INTERVAL_TICKS, Wake.SEND);
        if (this.count > 0 && this.transmitted >= this.count && !this.exitScheduled) {
            this.exitScheduled = true;
            long waitMicros = this.replies > 0 ? Math.max(2 * this.tripMax, 1_000_000L) : LINGER_TICKS * 50_000L;
            context.schedule(Math.max(1L, (waitMicros + 49_999L) / 50_000L), Wake.EXIT);
        }
    }

    private void reply(ProcessContext context, IcmpSocket.Received message, IcmpMessage.Echo echo) {
        Long sent = this.sentLatency.get(echo.sequence());
        if (sent == null) {
            return; // not a request of this run
        }
        this.acknowledge(echo.sequence());
        long trip = Math.max(0L, (message.latency() - sent) / 1000L);
        boolean duplicate = !this.received.add(echo.sequence());
        if (duplicate) {
            this.duplicates++;
        } else {
            this.replies++;
        }
        this.tripSum += trip;
        this.tripSquares += (double) trip * trip;
        this.tripMin = Math.min(this.tripMin, trip);
        this.tripMax = Math.max(this.tripMax, trip);
        context.out().line(PingOutput.reply(echo.length(), message.from(), echo.sequence(), message.ttl(), trip, duplicate));
    }

    private void error(ProcessContext context, IcmpSocket.Received message, IcmpMessage.Unreachable error, int sequence) {
        this.acknowledge(sequence);
        this.errors++;
        if (error.code() == IcmpMessage.Unreachable.NET_UNREACHABLE) {
            this.netUnreachable = true;
        } else {
            this.hostUnreachable = true;
        }
        context.out().line(PingOutput.error(message.from(), sequence, error));
    }

    /** {@code acknowledge}: how many requests were out when this one was answered ({@code pipe}). */
    private void acknowledge(int sequence) {
        int out = (int) ((this.transmitted - sequence) & 0xFFFF);
        if (out <= 0x7FFF && out + 1 > this.pipe) {
            this.pipe = out + 1;
        }
    }

    /** {@code finish}: the statistics, the advice after a failure, the exit status. */
    private void finish(ProcessContext context) {
        this.finished = true;
        long elapsed = (this.lastSent - this.start) * (Latency.TICK_NANOS / 1_000_000L);
        PingOutput.statistics(new PingOutput.Statistics(this.hostname, this.transmitted, this.replies, this.duplicates,
                this.errors, elapsed, this.tripMin, this.tripMax, this.tripSum, this.tripSquares, this.pipe))
                .forEach(context.out()::line);
        if (this.replies == 0) {
            String key = this.netUnreachable ? "net_unreachable" : this.hostUnreachable ? "host_unreachable" : "no_reply";
            context.out().advise("terminal.worldwideblocks.advice.ping." + key, this.destination.toString());
        }
        context.exit(this.replies == 0 ? 1 : 0);
    }
}
