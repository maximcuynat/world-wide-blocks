package dev.netcraft.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Réglages du client, dans {@code config/netcraft-client.toml}. Instance portée par le client (aucun
 * état statique) ; NeoForge relit le fichier quand il change, sans relancer le jeu.
 */
final class NetCraftClientConfig {
    private final ModConfigSpec spec;
    private final ModConfigSpec.BooleanValue messageSymbols;

    NetCraftClientConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        this.messageSymbols = builder
                .comment("Show the level of NetCraft messages as a symbol (true) or as a word (false), for fonts that render the symbols badly.")
                .translation("netcraft.configuration.messageSymbols")
                .define("messageSymbols", true);
        this.spec = builder.build();
    }

    ModConfigSpec spec() {
        return this.spec;
    }

    /** Vrai : symbole devant les messages ; faux : mot traduit (« Attention : … »). */
    boolean messageSymbols() {
        return this.messageSymbols.getAsBoolean();
    }
}
