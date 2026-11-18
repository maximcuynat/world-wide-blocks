package io.github.maximcuynat.worldwideblocks.network;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

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

    static final Codec<DeviceRole> ROLE = Codec.STRING.comapFlatMap(
            name -> DeviceRole.bySerializedName(name)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Rôle d'appareil inconnu : " + name)),
            DeviceRole::serializedName);

    static final Codec<PortKind> PORT_KIND = Codec.STRING.comapFlatMap(
            name -> PortKind.bySerializedName(name)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Type de port inconnu : " + name)),
            PortKind::serializedName);

    /** Écrite en texte ({@code 02:4f:a0:00:1b:7c}) pour rester lisible dans un éditeur NBT. */
    static final Codec<MacAddress> MAC = Codec.STRING.comapFlatMap(
            text -> MacAddress.parse(text)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Adresse MAC illisible : " + text)),
            MacAddress::toString);

    /**
     * {@code role}, {@code port_kinds} et {@code macs} n'existent que depuis le lot 1b : ils sont facultatifs pour
     * relire les mondes plus anciens (voir {@link SavedNode#decode}).
     */
    static final Codec<SavedNode> NODE = RecordCodecBuilder.create(i -> i.group(
            DEVICE_ID.fieldOf("id").forGetter(saved -> saved.node().id()),
            Codec.STRING.fieldOf("type").forGetter(saved -> saved.node().deviceType()),
            ROLE.optionalFieldOf("role").forGetter(saved -> Optional.of(saved.node().role())),
            Codec.INT.fieldOf("ports").forGetter(saved -> saved.node().portCount()),
            PORT_KIND.listOf().optionalFieldOf("port_kinds").forGetter(saved -> Optional.of(saved.node().ports())),
            MAC.listOf().optionalFieldOf("macs", List.of()).forGetter(SavedNode::macs)
    ).apply(i, SavedNode::decode));

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
