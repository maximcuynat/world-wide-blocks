package dev.netcraft.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

/**
 * Format de sauvegarde du réseau. Les noms de champs sont écrits dans le monde du joueur :
 * ne jamais les renommer sans prévoir la relecture des anciennes sauvegardes.
 */
final class NetworkCodecs {
    static final Codec<DeviceId> DEVICE_ID = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("dimension").forGetter(DeviceId::dimension),
            Codec.INT.fieldOf("x").forGetter(DeviceId::x),
            Codec.INT.fieldOf("y").forGetter(DeviceId::y),
            Codec.INT.fieldOf("z").forGetter(DeviceId::z)
    ).apply(i, DeviceId::new));

    static final Codec<PortRef> PORT_REF = RecordCodecBuilder.create(i -> i.group(
            DEVICE_ID.fieldOf("device").forGetter(PortRef::device),
            Codec.INT.fieldOf("port").forGetter(PortRef::index)
    ).apply(i, PortRef::new));

    static final Codec<NetworkNode> NODE = RecordCodecBuilder.create(i -> i.group(
            DEVICE_ID.fieldOf("id").forGetter(NetworkNode::id),
            Codec.STRING.fieldOf("type").forGetter(NetworkNode::deviceType),
            Codec.INT.fieldOf("ports").forGetter(NetworkNode::portCount)
    ).apply(i, NetworkNode::new));

    static final Codec<LinkMedium> MEDIUM = Codec.STRING.comapFlatMap(
            name -> LinkMedium.bySerializedName(name)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Support de lien inconnu : " + name)),
            LinkMedium::serializedName);

    static final Codec<Link> LINK = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Link::id),
            PORT_REF.fieldOf("a").forGetter(Link::a),
            PORT_REF.fieldOf("b").forGetter(Link::b),
            MEDIUM.fieldOf("medium").forGetter(Link::medium),
            Codec.BOOL.fieldOf("cable_consumed").forGetter(Link::cableConsumed)
    ).apply(i, Link::new));

    private NetworkCodecs() {
    }
}
