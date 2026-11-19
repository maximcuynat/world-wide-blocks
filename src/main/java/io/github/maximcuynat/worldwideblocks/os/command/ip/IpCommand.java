package io.github.maximcuynat.worldwideblocks.os.command.ip;

import java.util.List;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.os.TerminalOutput;
import io.github.maximcuynat.worldwideblocks.os.process.Process;
import io.github.maximcuynat.worldwideblocks.os.process.ProcessContext;

/**
 * The {@code ip} command of iproute2, as Debian 13 ships it (iproute2 6.15.0), on the real state of the machine:
 * {@code ip address}, {@code ip link} and {@code ip route}, with their abbreviations ({@code ip a}, {@code ip l},
 * {@code ip r}). Names, order of matching and error messages read in the sources of iproute2 v6.15.0
 * ({@code ip/ip.c}, {@code ip/ipaddress.c}, {@code ip/iplink.c}, {@code ip/iproute.c}, {@code lib/utils.c}) on
 * 2026-11-19; the lines themselves in {@link IpOutput}.
 *
 * <p>Known differences with the real tool, in lot 1c-1:
 * <ul>
 * <li>Options: only {@code -4} and {@code -help}. The other options of the real tool are answered as unknown.</li>
 * <li>Objects: only {@code address}, {@code link} and {@code route} ({@code neighbour} comes with ARP). The other
 * objects of the real tool are answered as unknown.</li>
 * <li>Listing of {@code ip address} and {@code ip link}: only a device name ({@code dev eth0} or {@code eth0}). Like
 * iproute2, any other word is taken as a device name, here also the filter keywords of the real tool ({@code up},
 * {@code scope}...). Listing of {@code ip route}: only {@code dev eth0}; any other word gets the error of an
 * unreadable prefix.</li>
 * <li>{@code ip address flush}, {@code save}, {@code showdump}, {@code restore}, and the other commands of
 * {@code ip link} and {@code ip route}: answered as unknown commands.</li>
 * <li>Changes ({@code add}, {@code delete}, {@code set}...): the account {@code user} is not root, and the kernel
 * refuses them ("RTNETLINK answers: Operation not permitted", from memory: netlink changes need
 * {@code CAP_NET_ADMIN}), whatever the arguments, which are not checked.</li>
 * <li>{@code help}: a short usage of what this machine supports, written for the mod in the layout of iproute2. The
 * usage texts of iproute2 are under GPL-2.0-or-later and are not copied into the mod (LGPL-3.0-only).</li>
 * </ul>
 *
 * <p>Error messages go to the console like the output (the terminal shows standard error too). Exit statuses:
 * iproute2 exits with {@code exit(-1)} for most errors, which a shell sees as 255.
 */
public final class IpCommand implements Process {
    /** {@code exit(-1)}, as seen by the shell. */
    static final int EXIT_USAGE = 255;
    /** Error returned by a command function ({@code return -1}, turned into 1 by {@code do_cmd}). */
    static final int EXIT_FAILURE = 1;
    /** Exit status of {@code rtnl_talk} errors ({@code return -2} in the callers, 2 for the shell). */
    static final int EXIT_NETLINK = 2;

    /** Options of {@code ip} in the order {@code main} of {@code ip/ip.c} tries them (prefix match). */
    private static final List<String> OPTIONS = List.of("-loops", "-family", "-4", "-6", "-0", "-M", "-B", "-all",
            "-echo", "-human", "-human-readable", "-iec", "-stats", "-statistics", "-details", "-resolve", "-oneline",
            "-timestamp", "-tshort", "-Version", "-force", "-batch", "-brief", "-json", "-pretty", "-rcvbuf", "-color",
            "-help", "-netns", "-Numeric");
    /** Options compared whole by {@code strcmp}, not by prefix. */
    private static final List<String> EXACT_OPTIONS = List.of("-4", "-6", "-0", "-M", "-B", "-echo");

    /** Objects of {@code ip} in the order of the {@code cmds} table of {@code ip/ip.c} (prefix match). */
    private static final List<String> OBJECTS = List.of("address", "addrlabel", "maddress", "route", "rule", "neighbor",
            "neighbour", "ntable", "ntbl", "link", "l2tp", "fou", "ila", "macsec", "tunnel", "tunl", "tuntap", "tap",
            "token", "tcpmetrics", "tcp_metrics", "monitor", "xfrm", "mroute", "mrule", "netns", "netconf", "vrf", "sr",
            "nexthop", "mptcp", "ioam", "help", "stats");

    /**
     * Usages of what this machine supports, written for the mod in the layout of the usages of iproute2 (whose
     * texts are under GPL-2.0-or-later and are not copied): first line {@code Usage: ip ...}, next lines aligned
     * under it, {@code :=} for the definitions.
     */
    static final List<String> IP_USAGE = List.of(
            "Usage: ip [ OPTIONS ] OBJECT { COMMAND | help }",
            "where  OBJECT := { address | help | link | route }",
            "       OPTIONS := { -4 | -help }");
    static final List<String> ADDRESS_USAGE = List.of(
            "Usage: ip address [ show [ dev IFNAME ] ]",
            "       ip address help");
    static final List<String> LINK_USAGE = List.of(
            "Usage: ip link [ show [ dev IFNAME ] ]",
            "       ip link help");
    static final List<String> ROUTE_USAGE = List.of(
            "Usage: ip route [ show [ dev IFNAME ] ]",
            "       ip route help");

    @Override
    public void start(ProcessContext context) {
        context.exit(run(context.system().stack(), context.arguments(), context.now(), context.out()));
    }

    /**
     * Runs {@code ip} with these arguments.
     *
     * @param argv the words of the command line, {@code ip} first
     * @return the exit status
     */
    public static int run(Ipv4Stack stack, List<String> argv, long now, TerminalOutput out) {
        boolean inetOnly = false;
        int next = 1;
        while (next < argv.size()) {
            String option = argv.get(next);
            if (option.equals("--")) {
                next++;
                break;
            }
            if (!option.startsWith("-")) {
                break;
            }
            String name = option.startsWith("--") ? option.substring(1) : option;
            Optional<String> matched = matchOption(name);
            if (matched.isPresent() && matched.get().equals("-4")) {
                inetOnly = true;
            } else if (matched.isPresent() && matched.get().equals("-help")) {
                return usage(out, IP_USAGE);
            } else {
                out.line("Option \"" + name + "\" is unknown, try \"ip -help\".");
                return EXIT_USAGE;
            }
            next++;
        }
        if (next >= argv.size()) {
            return usage(out, IP_USAGE);
        }
        String object = argv.get(next);
        List<String> rest = argv.subList(next + 1, argv.size());
        Optional<String> matched = OBJECTS.stream().filter(candidate -> matches(object, candidate)).findFirst();
        String found = matched.orElse("");
        return switch (found) {
            case "address" -> address(stack, rest, now, inetOnly, out);
            case "link" -> link(stack, rest, out);
            case "route" -> route(stack, rest, out);
            case "help" -> usage(out, IP_USAGE);
            default -> {
                out.line("Object \"" + object + "\" is unknown, try \"ip help\".");
                yield EXIT_FAILURE;
            }
        };
    }

    // ---- ip address (do_ipaddr, ip/ipaddress.c) ----

    private static int address(Ipv4Stack stack, List<String> args, long now, boolean inetOnly, TerminalOutput out) {
        if (args.isEmpty()) {
            return listAddresses(stack, List.of(), now, inetOnly, out);
        }
        String command = args.getFirst();
        if (matches(command, "add") || matches(command, "change") || command.equals("chg") || matches(command, "replace")
                || matches(command, "delete")) {
            return notPermitted(out);
        }
        if (matches(command, "list") || matches(command, "show") || matches(command, "lst")) {
            return listAddresses(stack, args.subList(1, args.size()), now, inetOnly, out);
        }
        if (matches(command, "help")) {
            return usage(out, ADDRESS_USAGE);
        }
        // flush, save, showdump and restore are not part of lot 1c-1.
        out.line("Command \"" + command + "\" is unknown, try \"ip address help\".");
        return EXIT_USAGE;
    }

    private static int listAddresses(Ipv4Stack stack, List<String> filter, long now, boolean inetOnly, TerminalOutput out) {
        Device device = device(stack, filter, out, ADDRESS_USAGE);
        if (device.status() >= 0) {
            return device.status();
        }
        stack.interfaces().stream().filter(link -> device.name().isEmpty() || link.name().equals(device.name().get()))
                // With -4, an interface without an IPv4 address is left out (the filter on the family drops it).
                .filter(link -> !inetOnly || !link.addresses().isEmpty())
                .forEach(link -> IpOutput.address(link, now).forEach(out::line));
        return 0;
    }

    // ---- ip link (do_iplink, ip/iplink.c) ----

    private static int link(Ipv4Stack stack, List<String> args, TerminalOutput out) {
        if (args.isEmpty()) {
            return listLinks(stack, List.of(), out);
        }
        String command = args.getFirst();
        if (matches(command, "add") || matches(command, "set") || matches(command, "change") || matches(command, "replace")
                || matches(command, "delete")) {
            return notPermitted(out);
        }
        if (matches(command, "show") || matches(command, "lst") || matches(command, "list")) {
            return listLinks(stack, args.subList(1, args.size()), out);
        }
        if (matches(command, "help")) {
            return usage(out, LINK_USAGE);
        }
        out.line("Command \"" + command + "\" is unknown, try \"ip link help\".");
        return EXIT_USAGE;
    }

    private static int listLinks(Ipv4Stack stack, List<String> filter, TerminalOutput out) {
        Device device = device(stack, filter, out, LINK_USAGE);
        if (device.status() >= 0) {
            return device.status();
        }
        stack.interfaces().stream().filter(link -> device.name().isEmpty() || link.name().equals(device.name().get()))
                .forEach(link -> IpOutput.link(link).forEach(out::line));
        return 0;
    }

    // ---- ip route (do_iproute, ip/iproute.c) ----

    private static int route(Ipv4Stack stack, List<String> args, TerminalOutput out) {
        if (args.isEmpty()) {
            return listRoutes(stack, List.of(), out);
        }
        String command = args.getFirst();
        if (matches(command, "add") || matches(command, "change") || matches(command, "replace")
                || matches(command, "prepend") || matches(command, "append") || matches(command, "delete")) {
            return notPermitted(out);
        }
        if (matches(command, "list") || matches(command, "show") || matches(command, "lst")) {
            return listRoutes(stack, args.subList(1, args.size()), out);
        }
        if (matches(command, "help")) {
            return usage(out, ROUTE_USAGE);
        }
        out.line("Command \"" + command + "\" is unknown, try \"ip route help\".");
        return EXIT_USAGE;
    }

    /**
     * {@code iproute_list_flush_or_save}: {@code dev NAME} (or {@code oif NAME}) keeps the routes of that interface.
     * Any other word is read there as a destination prefix; the filters on a prefix and the other keywords of the
     * real tool are not part of lot 1c-1 and get the error of an unreadable prefix ({@code get_prefix},
     * {@code lib/utils.c}; "any valid" is the name of the unspecified family).
     */
    private static int listRoutes(Ipv4Stack stack, List<String> args, TerminalOutput out) {
        String device = null;
        for (int i = 0; i < args.size(); i++) {
            String word = args.get(i);
            if (word.equals("dev") || word.equals("oif")) {
                if (i + 1 >= args.size()) {
                    out.line("Command line is not complete. Try option \"help\"");
                    return EXIT_USAGE;
                }
                device = args.get(++i);
            } else {
                out.line("Error: any valid prefix is expected rather than \"" + word + "\".");
                return EXIT_FAILURE;
            }
        }
        if (device != null && stack.interfaceNamed(device).isEmpty()) {
            out.line("Cannot find device \"" + device + "\"");
            return EXIT_FAILURE;
        }
        String kept = device;
        IpOutput.routes(stack.routes().routes().stream().filter(route -> kept == null || route.device().equals(kept))
                .toList()).forEach(out::line);
        return 0;
    }

    // ---- Shared ----

    /** Device name of a listing, or an exit status when the arguments end the command. */
    private record Device(Optional<String> name, int status) {
    }

    /**
     * Arguments of a listing, as {@code ipaddr_list_flush_or_save} reads them: {@code dev NAME}, or a word taken as
     * the name; {@code help} shows the usage.
     */
    private static Device device(Ipv4Stack stack, List<String> args, TerminalOutput out, List<String> usage) {
        String name = null;
        for (int i = 0; i < args.size(); i++) {
            String word = args.get(i);
            if (word.equals("dev")) {
                if (i + 1 >= args.size()) {
                    out.line("Command line is not complete. Try option \"help\"");
                    return new Device(Optional.empty(), EXIT_USAGE);
                }
                word = args.get(++i);
            } else if (matches(word, "help")) {
                return new Device(Optional.empty(), usage(out, usage));
            }
            if (name != null) {
                out.line("Error: either \"dev\" is duplicate, or \"" + word + "\" is garbage.");
                return new Device(Optional.empty(), EXIT_USAGE);
            }
            name = word;
        }
        if (name != null && stack.interfaceNamed(name).isEmpty()) {
            out.line("Device \"" + name + "\" does not exist.");
            return new Device(Optional.empty(), EXIT_FAILURE);
        }
        return new Device(Optional.ofNullable(name), -1);
    }

    /**
     * A change asked by an account that is not root: refused by the kernel (EPERM, from memory), printed by
     * {@code lib/libnetlink.c} ("RTNETLINK answers: %s"); the change functions of iproute2 then return -2
     * ({@code ipaddr_modify}, {@code iplink_modify}, {@code iproute_modify}), exit status 2.
     */
    private static int notPermitted(TerminalOutput out) {
        out.line("RTNETLINK answers: Operation not permitted");
        return EXIT_NETLINK;
    }

    private static int usage(TerminalOutput out, List<String> usage) {
        usage.forEach(out::line);
        return EXIT_USAGE;
    }

    private static Optional<String> matchOption(String option) {
        for (String candidate : OPTIONS) {
            if (EXACT_OPTIONS.contains(candidate) ? option.equals(candidate) : matches(option, candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * {@code matches} of iproute2 ({@code lib/utils.c}): true when {@code word} is a prefix of {@code name}, the empty
     * word excepted.
     */
    static boolean matches(String word, String name) {
        return !word.isEmpty() && name.startsWith(word);
    }
}
