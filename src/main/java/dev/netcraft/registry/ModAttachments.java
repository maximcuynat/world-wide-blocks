package dev.netcraft.registry;

import dev.netcraft.NetCraft;
import dev.netcraft.network.sync.PlayerLinkView;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NetCraft.MODID);

    /**
     * Câbles que le client d'un joueur connaît, côté serveur. Non sauvegardé et non copié à la mort :
     * un joueur qui réapparaît repart de zéro et reçoit de nouveau ce qu'il voit.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerLinkView>> PLAYER_LINK_VIEW = ATTACHMENT_TYPES.register("player_link_view",
            () -> AttachmentType.builder(() -> new PlayerLinkView()).build());

    private ModAttachments() {
    }
}
