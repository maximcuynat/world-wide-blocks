package io.github.maximcuynat.worldwideblocks.os;

import java.util.Objects;

import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.os.command.CommandRegistry;
import io.github.maximcuynat.worldwideblocks.os.process.ProcessTable;
import io.github.maximcuynat.worldwideblocks.os.shell.ShellHistory;

/**
 * The operating system of a computer: its IP stack, the commands it knows, its processes, its host name, the
 * history of its account and its local console. Runtime state of the server, not saved: a server restart starts it
 * again, like a real computer that restarts.
 *
 * <p>Plain Java, no Minecraft class.
 */
public final class OperatingSystem {
    private static final ConsoleSession.Listener NO_LISTENER = (session, change) -> {
    };

    private final Ipv4Stack stack;
    private final CommandRegistry commands;
    private final ProcessTable processes;
    private final ShellHistory history = new ShellHistory();
    private String hostName = HostName.UNKNOWN;
    private ConsoleSession.Listener consoleListener = NO_LISTENER;
    private int sessions;
    private ConsoleSession localConsole;

    public OperatingSystem(Ipv4Stack stack, CommandRegistry commands) {
        this.stack = stack;
        this.commands = commands;
        this.processes = new ProcessTable(this);
        this.localConsole = new ConsoleSession(this, ++this.sessions);
    }

    public Ipv4Stack stack() {
        return this.stack;
    }

    public CommandRegistry commands() {
        return this.commands;
    }

    public ProcessTable processes() {
        return this.processes;
    }

    public String hostName() {
        return this.hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = Objects.requireNonNull(hostName, "hostName");
    }

    /** The history of the account, shared by its shells ({@code ~/.bash_history}). */
    public ShellHistory history() {
        return this.history;
    }

    /** The console of the screen and keyboard of the machine, possibly ended ({@link ConsoleSession#ended}). */
    public ConsoleSession localConsole() {
        return this.localConsole;
    }

    /**
     * The local console for a player who opens the screen: the current one, or a new shell on a clear screen if the
     * previous one ended ({@code exit}, Ctrl+D). The other processes of the machine are not touched.
     */
    public ConsoleSession openLocalConsole() {
        if (this.localConsole.ended()) {
            this.localConsole = new ConsoleSession(this, ++this.sessions);
        }
        return this.localConsole;
    }

    /** Receives every change of the consoles of this machine, for the players who watch them. */
    public void setConsoleListener(ConsoleSession.Listener listener) {
        this.consoleListener = listener == null ? NO_LISTENER : listener;
    }

    void consoleChanged(ConsoleSession session, ConsoleSession.Change change) {
        this.consoleListener.changed(session, change);
    }

    /** A timer asked through {@link MachineContext#schedule}, by a process or by the IP stack, is due. */
    public void onTimer(MachineContext machine, Object key) {
        if (key instanceof ProcessTable.Timer timer) {
            this.processes.onTimer(machine, timer);
        } else {
            this.stack.onTimer(machine.network(), key);
            this.processes.dispatch(machine);
        }
    }

    /** A frame reached the network card of an interface: the stack takes it, then the processes what it brought. */
    public void receive(MachineContext machine, NetInterface in, EthernetFrame frame) {
        this.stack.receive(machine.network(), in, frame);
        this.processes.dispatch(machine);
    }
}
