package io.github.maximcuynat.worldwideblocks.os;

import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.ip.NetInterface;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.os.command.CommandRegistry;
import io.github.maximcuynat.worldwideblocks.os.process.ProcessTable;

/**
 * The operating system of a computer: its IP stack, the commands it knows, its processes and its local console.
 * Runtime state of the server, not saved: a server restart starts it again, like a real computer that restarts.
 *
 * <p>Plain Java, no Minecraft class.
 */
public final class OperatingSystem {
    private final Ipv4Stack stack;
    private final CommandRegistry commands;
    private final ProcessTable processes;
    private final ConsoleSession localConsole;

    public OperatingSystem(Ipv4Stack stack, CommandRegistry commands) {
        this.stack = stack;
        this.commands = commands;
        this.processes = new ProcessTable(this);
        this.localConsole = new ConsoleSession(this);
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

    /** The console of the screen and keyboard of the machine. */
    public ConsoleSession localConsole() {
        return this.localConsole;
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
