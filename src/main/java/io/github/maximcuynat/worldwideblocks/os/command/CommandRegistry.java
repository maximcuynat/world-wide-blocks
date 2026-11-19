package io.github.maximcuynat.worldwideblocks.os.command;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

import io.github.maximcuynat.worldwideblocks.os.command.ip.IpCommand;
import io.github.maximcuynat.worldwideblocks.os.process.Process;

/** The commands a machine knows, by name: a later lot adds its commands here without touching the shell. */
public final class CommandRegistry {
    private final Map<String, Supplier<Process>> commands = new TreeMap<>();

    /** The commands of the computers. */
    public static CommandRegistry standard() {
        return new CommandRegistry().register("ip", IpCommand::new);
    }

    public CommandRegistry register(String name, Supplier<Process> factory) {
        if (this.commands.putIfAbsent(name, Objects.requireNonNull(factory, "factory")) != null) {
            throw new IllegalArgumentException("Command " + name + " already registered");
        }
        return this;
    }

    /** A new process of this command, if the machine knows it. */
    public Optional<Process> create(String name) {
        return Optional.ofNullable(this.commands.get(name)).map(Supplier::get);
    }
}
