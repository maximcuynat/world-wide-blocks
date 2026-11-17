package dev.netcraft.network.sync;

import java.util.List;

import dev.netcraft.NetCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * État de l'appareil que le joueur vise, pour l'indicateur : il n'est envoyé que pour cet appareil,
 * et seulement quand il change. Lot 1b-2 : s'y ajouteront l'état DHCP du poste (le bail sera envoyé
 * comme une heure d'expiration en temps de jeu, pas comme un compte à rebours) et les adresses
 * prêtées par la box.
 *
 * @param switching vrai pour un appareil qui commute (box, switch) : l'indicateur montre alors les
 *                  MAC apprises derrière chaque port plutôt que la MAC de chaque port
 * @param portMacs  MAC de chaque port, dans l'ordre des ports (valeurs sur 48 bits)
 * @param learned   MAC apprises, pour un appareil qui commute
 */
public record DeviceStatusPayload(BlockPos pos, boolean switching, List<Long> portMacs, List<LearnedMac> learned)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DeviceStatusPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NetCraft.MODID, "device_status"));

    public static final StreamCodec<ByteBuf, DeviceStatusPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DeviceStatusPayload::pos,
            ByteBufCodecs.BOOL, DeviceStatusPayload::switching,
            ByteBufCodecs.LONG.apply(ByteBufCodecs.list()), DeviceStatusPayload::portMacs,
            LearnedMac.STREAM_CODEC.apply(ByteBufCodecs.list()), DeviceStatusPayload::learned,
            DeviceStatusPayload::new);

    public DeviceStatusPayload {
        portMacs = List.copyOf(portMacs);
        learned = List.copyOf(learned);
    }

    @Override
    public CustomPacketPayload.Type<DeviceStatusPayload> type() {
        return TYPE;
    }
}
