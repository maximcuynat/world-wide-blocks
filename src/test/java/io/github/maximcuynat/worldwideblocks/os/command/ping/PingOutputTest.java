package io.github.maximcuynat.worldwideblocks.os.command.ping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.protocol.IcmpMessage;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Packet;

/**
 * The lines of {@code ping}, rebuilt from the values of the real runs of {@code src/test/resources/linux-real-outputs.txt}
 * (Debian 13, iputils 3:20240905-3): with the same numbers, the same text.
 */
class PingOutputTest {
    private static final Ipv4Address ROUTER = Ipv4Address.of(192, 168, 1, 1);
    private static final Ipv4Address PC = Ipv4Address.of(192, 168, 1, 10);

    /** The lines of a section {@code === name} of the reference file. */
    static List<String> reference(String section) {
        try (InputStream in = PingOutputTest.class.getResourceAsStream("/linux-real-outputs.txt")) {
            List<String> all = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            int start = all.indexOf("=== " + section) + 1;
            assertTrue(start > 0, "section " + section);
            int end = start;
            while (end < all.size() && !all.get(end).startsWith("=== ")) {
                end++;
            }
            return all.subList(start, end);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static IcmpMessage.Unreachable unreachable(int code) {
        return new IcmpMessage.Unreachable(code, new Ipv4Packet(PC, ROUTER, IcmpMessage.Echo.request(1, 1, 56)));
    }

    @Test
    void theRepliesToTheRouterAreTheRealOnes() {
        long[] trips = {507, 55, 49};
        List<String> lines = new ArrayList<>();
        lines.add(PingOutput.header("192.168.1.1", ROUTER, PingCommand.DATA_LENGTH));
        double sum = 0;
        double squares = 0;
        for (int i = 0; i < trips.length; i++) {
            lines.add(PingOutput.reply(64, ROUTER, i + 1, 64, trips[i], false));
            sum += trips[i];
            squares += (double) trips[i] * trips[i];
        }
        lines.addAll(PingOutput.statistics(new PingOutput.Statistics("192.168.1.1", 3, 3, 0, 0, 2043, 49, 507, sum, squares, 1)));

        assertEquals(reference("ping router"), lines);
    }

    @Test
    void errorsAreCountedApartAndThePipeEndsTheBlock() {
        List<String> lines = new ArrayList<>();
        lines.add(PingOutput.header("192.168.1.250", Ipv4Address.of(192, 168, 1, 250), 56));
        lines.add(PingOutput.error(PC, 1, unreachable(IcmpMessage.Unreachable.HOST_UNREACHABLE)));
        lines.add(PingOutput.error(PC, 2, unreachable(IcmpMessage.Unreachable.HOST_UNREACHABLE)));
        lines.addAll(PingOutput.statistics(new PingOutput.Statistics("192.168.1.250", 2, 0, 0, 2, 1006, Long.MAX_VALUE, 0, 0, 0, 2)));

        assertEquals(reference("ping absent count 2"), lines);
    }

    @Test
    void withoutRoundTripNorPipeTheBlockEndsWithAnEmptyLine() {
        List<String> lines = new ArrayList<>();
        lines.add(PingOutput.header("10.9.9.9", Ipv4Address.of(10, 9, 9, 9), 56));
        for (int sequence = 1; sequence <= 3; sequence++) {
            lines.add(PingOutput.error(ROUTER, sequence, unreachable(IcmpMessage.Unreachable.NET_UNREACHABLE)));
        }
        lines.addAll(PingOutput.statistics(new PingOutput.Statistics("10.9.9.9", 3, 0, 0, 3, 2012, Long.MAX_VALUE, 0, 0, 0, 1)));

        assertEquals(reference("ping outside"), lines);
    }

    @Test
    void theRoundTripsOfALostRunAreTheRealOnes() {
        List<String> statistics = PingOutput.statistics(
                new PingOutput.Statistics("192.168.1.1", 4, 2, 0, 0, 3081, 58, 100, 158, 100 * 100 + 58 * 58, 1));

        List<String> real = reference("ping route lost");
        assertEquals(real.subList(real.size() - 4, real.size()), statistics);
    }

    @Test
    void theTimeKeepsThreeSignificantDigits() {
        assertEquals("0.104 ms", PingOutput.time(104));
        assertEquals("0.999 ms", PingOutput.time(999));
        assertEquals("1.00 ms", PingOutput.time(1000));
        assertEquals("2.99 ms", PingOutput.time(2992), "real line: time=2.99 ms (ping own address), rtt max 2.992");
        assertEquals("10.0 ms", PingOutput.time(9995));
        assertEquals("99.9 ms", PingOutput.time(99_949));
        assertEquals("100 ms", PingOutput.time(99_950));
        assertEquals("1235 ms", PingOutput.time(1_234_567));
    }

    @Test
    void theLossIsWrittenLikePrintfG() {
        assertEquals("0", PingOutput.percent(0f));
        assertEquals("100", PingOutput.percent(100f));
        assertEquals("50", PingOutput.percent(50f));
        assertEquals("33.3333", PingOutput.percent((float) (1 * 100.0 / 3)));
        assertEquals("66.6667", PingOutput.percent((float) (2 * 100.0 / 3)));
    }

    @Test
    void anOtherCodeGetsTheBadCodeText() {
        assertEquals("From 192.168.1.1 icmp_seq=4 Dest Unreachable, Bad Code: 9",
                PingOutput.error(ROUTER, 4, unreachable(9)));
    }
}
