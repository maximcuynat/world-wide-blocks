package io.github.maximcuynat.worldwideblocks.network.sync;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * State of the device the player is aiming at, for the status panel: it is sent only for that device,
 * and only when it changes. Lot 1b-2: the computer's DHCP state will be added (the lease will be sent
 * as an expiry time in game time, not as a countdown) and the addresses
 * leased by the box.
 *
 * @param switching true for a device that switches (box, switch): the status panel then shows the
 *                  MACs learned behind each port rather than the MAC of each port
 * @param portMacs  MAC of each port, in port order (48-bit values)
 * @param learned   learned MACs, for a device that switches
 */
public record DeviceStatusPayload(BlockPos pos, boolean switching, List<Long> portMacs, List<LearnedMac> learned)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DeviceStatusPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "device_status"));

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
