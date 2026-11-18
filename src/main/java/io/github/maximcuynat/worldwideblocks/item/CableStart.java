package io.github.maximcuynat.worldwideblocks.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * First end chosen with a cord, remembered on the item while waiting for the second click.
 *
 * @param port number of the chosen port, starting at 0
 */
public record CableStart(GlobalPos device, int port) {
    public static final Codec<CableStart> CODEC = RecordCodecBuilder.create(i -> i.group(
            GlobalPos.CODEC.fieldOf("device").forGetter(CableStart::device),
            Codec.INT.fieldOf("port").forGetter(CableStart::port)
    ).apply(i, CableStart::new));

    public static final StreamCodec<ByteBuf, CableStart> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, CableStart::device,
            ByteBufCodecs.VAR_INT, CableStart::port,
            CableStart::new);

    /** The start port, as the network engine designates it. */
    public PortRef portRef() {
        DeviceId id = new DeviceId(this.device.dimension().identifier().toString(),
                this.device.pos().getX(), this.device.pos().getY(), this.device.pos().getZ());
        return new PortRef(id, this.port);
    }
}
