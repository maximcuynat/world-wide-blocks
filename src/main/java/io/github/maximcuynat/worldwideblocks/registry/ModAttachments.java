package io.github.maximcuynat.worldwideblocks.registry;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.network.sync.PlayerLinkView;
import io.github.maximcuynat.worldwideblocks.network.sync.PlayerStatusView;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, WorldWideBlocks.MODID);

    /**
     * Cables that a player's client knows about, on the server side. Not saved and not copied on death:
     * a player who respawns starts from zero and receives again what they see.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerLinkView>> PLAYER_LINK_VIEW = ATTACHMENT_TYPES.register("player_link_view",
            () -> AttachmentType.builder(() -> new PlayerLinkView()).build());

    /** Last device state sent to the player's indicator. Not saved, not copied on death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerStatusView>> PLAYER_STATUS_VIEW = ATTACHMENT_TYPES.register("player_status_view",
            () -> AttachmentType.builder(() -> new PlayerStatusView()).build());

    private ModAttachments() {
    }
}
