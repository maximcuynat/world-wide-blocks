package io.github.maximcuynat.worldwideblocks.os.command.ping;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.protocol.IcmpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;

/**
 * The lines of {@code ping}, in the formats of iputils 20240905 (Debian 13 ships 3:20240905-3), read in its sources on
 * 2026-11-19 ({@code ping/ping.c}, {@code ping/ping_common.c}) and compared with the real outputs of
 * {@code src/test/resources/linux-real-outputs.txt}. Times in microseconds, as iputils counts them.
 */
final class PingOutput {

    private PingOutput() {
    }

    /** {@code ping4_run}: {@code PING %s (%s) %d(%d) bytes of data.}, the sizes of the data and of the IP packet. */
    static String header(String hostname, Ipv4Address address, int dataLength) {
        return "PING " + hostname + " (" + address + ") " + dataLength + "(" + (dataLength + 8 + 20) + ") bytes of data.";
    }

    /** {@code gather_statistics}: {@code %d bytes from %s: icmp_seq=%u ttl=%d time=...}. */
    static String reply(int bytes, Ipv4Address from, int sequence, int ttl, long tripMicros, boolean duplicate) {
        return bytes + " bytes from " + from + ": icmp_seq=" + sequence + " ttl=" + ttl + " time=" + time(tripMicros)
                + (duplicate ? " (DUP!)" : "");
    }

    /**
     * The round trip as {@code gather_statistics} writes it: three decimals below 1 ms, two below 10 ms, one below
     * 100 ms, none beyond, each rounded except the first.
     */
    static String time(long micros) {
        if (micros >= 100_000 - 50) {
            return (micros + 500) / 1000 + " ms";
        }
        if (micros >= 10_000 - 5) {
            return (micros + 50) / 1000 + "." + (micros + 50) % 1000 / 100 + " ms";
        }
        if (micros >= 1000) {
            return (micros + 5) / 1000 + "." + pad((micros + 5) % 1000 / 10, 2) + " ms";
        }
        return micros / 1000 + "." + pad(micros % 1000, 3) + " ms";
    }

    /** {@code ping4_receive_error_msg}: {@code From %s icmp_seq=%u }, then the text of {@code pr_icmph}. */
    static String error(Ipv4Address from, int sequence, IcmpMessage.Unreachable error) {
        String text = switch (error.code()) {
            case IcmpMessage.Unreachable.NET_UNREACHABLE -> "Destination Net Unreachable";
            case IcmpMessage.Unreachable.HOST_UNREACHABLE -> "Destination Host Unreachable";
            default -> "Dest Unreachable, Bad Code: " + error.code();
        };
        return "From " + from + " icmp_seq=" + sequence + " " + text;
    }

    /** What {@code finish} counts. Times in microseconds; {@code tripSum} and {@code tripSquares} as doubles, like iputils. */
    record Statistics(String hostname, long transmitted, long received, long duplicates, long errors, long elapsedMillis,
            long tripMin, long tripMax, double tripSum, double tripSquares, int pipe) {
    }

    /**
     * {@code finish}: a blank line, the title, the counts, then the line of the round trips and of the pipe, which
     * stays empty when neither is written (its line break is always printed).
     */
    static List<String> statistics(Statistics s) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("--- " + s.hostname() + " ping statistics ---");
        StringBuilder counts = new StringBuilder();
        counts.append(s.transmitted()).append(" packets transmitted, ").append(s.received()).append(" received");
        if (s.duplicates() > 0) {
            counts.append(", +").append(s.duplicates()).append(" duplicates");
        }
        if (s.errors() > 0) {
            counts.append(", +").append(s.errors()).append(" errors");
        }
        if (s.transmitted() > 0) {
            float loss = (float) ((s.transmitted() - s.received()) * 100.0 / s.transmitted());
            counts.append(", ").append(percent(loss)).append("% packet loss, time ").append(s.elapsedMillis()).append("ms");
        }
        lines.add(counts.toString());
        StringBuilder last = new StringBuilder();
        if (s.received() > 0) {
            long total = s.received() + s.duplicates();
            long average = (long) (s.tripSum() / total);
            long variance = s.tripSum() < Integer.MAX_VALUE
                    ? (long) ((s.tripSquares() - (s.tripSum() * s.tripSum()) / total) / total)
                    : (long) (s.tripSquares() / total - (double) average * average);
            long deviation = integerSquareRoot(variance);
            last.append("rtt min/avg/max/mdev = ").append(millis(s.tripMin())).append('/').append(millis(average))
                    .append('/').append(millis(s.tripMax())).append('/').append(millis(deviation)).append(" ms");
        }
        if (s.pipe() > 1) {
            last.append(last.isEmpty() ? "" : ", ").append("pipe ").append(s.pipe());
        }
        lines.add(last.toString());
        return lines;
    }

    /** {@code %ld.%03ld}: microseconds as milliseconds, truncated. */
    private static String millis(long micros) {
        return micros / 1000 + "." + pad(micros % 1000, 3);
    }

    /** {@code %g} of a float: six significant digits, without trailing zeros. */
    static String percent(float value) {
        if (value == 0f) {
            return "0";
        }
        return new BigDecimal(value).round(new MathContext(6)).stripTrailingZeros().toPlainString();
    }

    /** {@code llsqrt} of iputils: Newton's method on integers. */
    static long integerSquareRoot(long a) {
        long previous = Long.MAX_VALUE;
        long x = a;
        if (x > 0) {
            while (x < previous) {
                previous = x;
                x = (x + a / x) / 2;
            }
        }
        return x;
    }

    private static String pad(long value, int digits) {
        String text = Long.toString(value);
        return "0".repeat(Math.max(0, digits - text.length())) + text;
    }
}
