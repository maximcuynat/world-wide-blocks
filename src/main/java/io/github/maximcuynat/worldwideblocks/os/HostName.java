package io.github.maximcuynat.worldwideblocks.os;

import java.util.Locale;

import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * The host name given to a computer: its type, then the last four hexadecimal digits of the MAC of its first port
 * ({@code laptop-3f2a}, {@code tower-07c1}), as some systems name a machine at installation. It will change through
 * {@code hostname} and {@code /etc/hostname} when the operating system has files ({@code docs/lot-plan.md}, lot 1c-1).
 */
public final class HostName {
    /** Name of a machine before its network card is known. Debian names an unnamed machine {@code localhost}. */
    public static final String UNKNOWN = "localhost";

    private HostName() {
    }

    /**
     * @param deviceType block identifier, for example {@code worldwideblocks:pc_tower}
     */
    public static String of(String deviceType, MacAddress firstPort) {
        return prefix(deviceType) + "-" + String.format(Locale.ROOT, "%02x%02x", firstPort.octet(4), firstPort.octet(5));
    }

    /** {@code tower} for a PC tower, {@code laptop} for a laptop, otherwise the path of the identifier. */
    static String prefix(String deviceType) {
        String path = deviceType.substring(deviceType.indexOf(':') + 1);
        if (path.endsWith("tower")) {
            return "tower";
        }
        return path.replace('_', '-');
    }
}
