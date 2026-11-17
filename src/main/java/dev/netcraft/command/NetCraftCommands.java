package dev.netcraft.command;

import java.util.Collection;
import java.util.Locale;

import com.mojang.brigadier.context.CommandContext;

import dev.netcraft.network.DeviceId;
import dev.netcraft.network.Link;
import dev.netcraft.network.NetworkDevices;
import dev.netcraft.network.NetworkManager;
import dev.netcraft.network.PortRef;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Commandes de contrôle du réseau, réservées aux opérateurs. {@code /netcraft liens} lit directement
 * l'état du serveur : elle sert à vérifier qu'un lien existe toujours quand sa zone n'est pas chargée
 * ou après un redémarrage.
 */
public final class NetCraftCommands {

    private NetCraftCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("netcraft")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("liens").executes(NetCraftCommands::listLinks)));
    }

    private static int listLinks(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        NetworkManager network = NetworkManager.of(source.getServer());
        Collection<Link> links = network.links();
        if (links.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.netcraft.links.none"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.netcraft.links.header", links.size()), false);
        for (Link link : links) {
            Component line = Component.translatable("commands.netcraft.links.entry",
                    describe(network, link.a()), link.a().displayNumber(),
                    describe(network, link.b()), link.b().displayNumber(),
                    String.format(Locale.ROOT, "%.1f", link.length()),
                    Component.translatable(link.cableConsumed() ? "commands.netcraft.links.consumed" : "commands.netcraft.links.not_consumed"));
            source.sendSuccess(() -> line, false);
        }
        return links.size();
    }

    /** Nom de l'appareil et position, par exemple « Tour PC [12, 64, -3] (minecraft:overworld) ». */
    private static Component describe(NetworkManager network, PortRef port) {
        DeviceId id = port.device();
        Component name = network.node(id).map(NetworkDevices::displayName).orElse(Component.literal("?"));
        return Component.translatable("commands.netcraft.links.device", name, id.x(), id.y(), id.z(), id.dimension());
    }
}
