package dev.netcraft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.netcraft.network.DeviceId;
import dev.netcraft.network.PortRef;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Première extrémité choisie avec un cordon, mémorisée sur l'objet en attendant le second clic.
 *
 * @param port numéro du port choisi, à partir de 0
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

    /** Le port de départ, tel que le moteur réseau le désigne. */
    public PortRef portRef() {
        DeviceId id = new DeviceId(this.device.dimension().identifier().toString(),
                this.device.pos().getX(), this.device.pos().getY(), this.device.pos().getZ());
        return new PortRef(id, this.port);
    }
}
