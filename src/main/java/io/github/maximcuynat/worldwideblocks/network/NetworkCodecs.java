package io.github.maximcuynat.worldwideblocks.network;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpBinding;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

import net.minecraft.core.UUIDUtil;

/**
 * Save format of the network. The field names are written in the player's world:
 * never rename them without planning how to reread old saves.
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
                    .orElseGet(() -> DataResult.error(() -> "Unknown device role: " + name)),
            DeviceRole::serializedName);

    static final Codec<PortKind> PORT_KIND = Codec.STRING.comapFlatMap(
            name -> PortKind.bySerializedName(name)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown port kind: " + name)),
            PortKind::serializedName);

    /** Written as text ({@code 02:4f:a0:00:1b:7c}) to stay readable in an NBT editor. */
    static final Codec<MacAddress> MAC = Codec.STRING.comapFlatMap(
            text -> MacAddress.parse(text)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Adresse MAC illisible : " + text)),
            MacAddress::toString);

    /** Written as text ({@code 192.168.1.10}), like the MAC addresses. */
    static final Codec<Ipv4Address> IPV4 = Codec.STRING.comapFlatMap(
            text -> Ipv4Address.parse(text)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unreadable IPv4 address: " + text)),
            Ipv4Address::toString);

    /**
     * {@code role}, {@code port_kinds} and {@code macs} only exist since lot 1b, {@code service_mac} since lot 1b-2:
     * they are optional to reread older worlds (see {@link SavedNode#decode}).
     */
    static final Codec<SavedNode> NODE = RecordCodecBuilder.create(i -> i.group(
            DEVICE_ID.fieldOf("id").forGetter(saved -> saved.node().id()),
            Codec.STRING.fieldOf("type").forGetter(saved -> saved.node().deviceType()),
            ROLE.optionalFieldOf("role").forGetter(saved -> Optional.of(saved.node().role())),
            Codec.INT.fieldOf("ports").forGetter(saved -> saved.node().portCount()),
            PORT_KIND.listOf().optionalFieldOf("port_kinds").forGetter(saved -> Optional.of(saved.node().ports())),
            MAC.listOf().optionalFieldOf("macs", List.of()).forGetter(SavedNode::macs),
            MAC.optionalFieldOf("service_mac").forGetter(SavedNode::serviceMac)
    ).apply(i, SavedNode::decode));

    static final Codec<DhcpBinding> DHCP_BINDING = RecordCodecBuilder.create(i -> i.group(
            MAC.fieldOf("client").forGetter(DhcpBinding::client),
            IPV4.fieldOf("address").forGetter(DhcpBinding::address),
            Codec.LONG.fieldOf("expires_at").forGetter(DhcpBinding::expiresAt),
            Codec.LONG.fieldOf("assigned_at").forGetter(DhcpBinding::assignedAt),
            Codec.BOOL.fieldOf("offered").forGetter(DhcpBinding::offered)
    ).apply(i, DhcpBinding::new));

    /** Memory of a box's DHCP server, since lot 1b-2. */
    static final Codec<SavedDhcpServer> DHCP_SERVER = RecordCodecBuilder.create(i -> i.group(
            DEVICE_ID.fieldOf("box").forGetter(SavedDhcpServer::box),
            Codec.BOOL.fieldOf("enabled").forGetter(SavedDhcpServer::enabled),
            DHCP_BINDING.listOf().fieldOf("bindings").forGetter(SavedDhcpServer::bindings)
    ).apply(i, SavedDhcpServer::new));

    static final Codec<LinkMedium> MEDIUM = Codec.STRING.comapFlatMap(
            name -> LinkMedium.bySerializedName(name)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown link medium: " + name)),
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
