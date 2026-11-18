package io.github.maximcuynat.worldwideblocks.command;

import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevices;
import io.github.maximcuynat.worldwideblocks.network.NetworkManager;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Network control commands, reserved for operators. {@code /worldwideblocks links} reads the
 * server state directly: it is used to check that a link still exists when its area is not loaded
 * or after a restart.
 */
public final class WorldWideBlocksCommands {

    private WorldWideBlocksCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("worldwideblocks")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("links").executes(WorldWideBlocksCommands::listLinks)));
        // Short alias /wwb. It redirects to the node actually in the tree: Brigadier merges every later
        // "worldwideblocks" registration into that node (/worldwideblocks test in development), so the
        // alias sees those subcommands too, whatever the registration order.
        dispatcher.register(Commands.literal("wwb")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .redirect(dispatcher.getRoot().getChild("worldwideblocks")));
    }

    private static int listLinks(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        NetworkManager network = NetworkManager.of(source.getServer());
        Collection<Link> links = network.links();
        if (links.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.worldwideblocks.links.none"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.worldwideblocks.links.header", links.size()), false);
        for (Link link : links) {
            Component line = Component.translatable("commands.worldwideblocks.links.entry",
                    describe(network, link.a()), link.a().displayNumber(),
                    describe(network, link.b()), link.b().displayNumber(),
                    Math.round(link.length()),
                    Component.translatable(link.cableConsumed() ? "commands.worldwideblocks.links.consumed" : "commands.worldwideblocks.links.not_consumed"));
            source.sendSuccess(() -> line, false);
        }
        return links.size();
    }

    /** Device in the common message format, followed by its dimension: "PC Tower [12, 64, -3] (minecraft:overworld)". */
    private static Component describe(NetworkManager network, PortRef port) {
        DeviceId id = port.device();
        Component name = network.node(id).map(NetworkDevices::displayName).orElse(Component.literal("?"));
        Component device = Component.translatable("message.worldwideblocks.device", name, id.x(), id.y(), id.z());
        return Component.translatable("commands.worldwideblocks.links.device", device, id.dimension());
    }
}
