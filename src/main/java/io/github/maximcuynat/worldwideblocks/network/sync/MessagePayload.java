package io.github.maximcuynat.worldwideblocks.network.sync;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.message.Message;
import io.github.maximcuynat.worldwideblocks.message.MessageArg;
import io.github.maximcuynat.worldwideblocks.message.MessageChannel;
import io.github.maximcuynat.worldwideblocks.message.MessageLevel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Un message pour un joueur : le canal, puis le niveau, la clé et les valeurs. Aucun texte rédigé ni
 * aucune mise en forme : le client décide seul de l'apparence, si bien que le dessin peut changer
 * sans toucher au serveur.
 */
public record MessagePayload(MessageChannel channel, Message message) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MessagePayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "message"));

    private static final StreamCodec<ByteBuf, MessageChannel> CHANNEL = ByteBufCodecs.idMapper(i -> MessageChannel.values()[i], MessageChannel::ordinal);
    private static final StreamCodec<ByteBuf, MessageLevel> LEVEL = ByteBufCodecs.idMapper(i -> MessageLevel.values()[i], MessageLevel::ordinal);

    private static final int TEXT = 0;
    private static final int NUMBER = 1;
    private static final int KEY = 2;
    private static final int DEVICE = 3;

    /** Une valeur : son genre, puis son contenu. */
    private static final StreamCodec<ByteBuf, MessageArg> ARG = StreamCodec.of(MessagePayload::encodeArg, MessagePayload::decodeArg);

    private static final StreamCodec<ByteBuf, Message> MESSAGE = StreamCodec.composite(
            LEVEL, Message::level,
            ByteBufCodecs.STRING_UTF8, Message::key,
            ARG.apply(ByteBufCodecs.list()), Message::args,
            Message::new);

    public static final StreamCodec<ByteBuf, MessagePayload> STREAM_CODEC = StreamCodec.composite(
            CHANNEL, MessagePayload::channel,
            MESSAGE, MessagePayload::message,
            MessagePayload::new);

    private static void encodeArg(ByteBuf buffer, MessageArg arg) {
        switch (arg) {
            case MessageArg.Text text -> {
                ByteBufCodecs.VAR_INT.encode(buffer, TEXT);
                ByteBufCodecs.STRING_UTF8.encode(buffer, text.value());
            }
            case MessageArg.Number number -> {
                ByteBufCodecs.VAR_INT.encode(buffer, NUMBER);
                ByteBufCodecs.VAR_LONG.encode(buffer, number.value());
            }
            case MessageArg.Key key -> {
                ByteBufCodecs.VAR_INT.encode(buffer, KEY);
                ByteBufCodecs.STRING_UTF8.encode(buffer, key.key());
            }
            case MessageArg.Device device -> {
                ByteBufCodecs.VAR_INT.encode(buffer, DEVICE);
                ByteBufCodecs.STRING_UTF8.encode(buffer, device.type());
                ByteBufCodecs.STRING_UTF8.encode(buffer, device.dimension());
                ByteBufCodecs.VAR_INT.encode(buffer, device.x());
                ByteBufCodecs.VAR_INT.encode(buffer, device.y());
                ByteBufCodecs.VAR_INT.encode(buffer, device.z());
            }
        }
    }

    private static MessageArg decodeArg(ByteBuf buffer) {
        int kind = ByteBufCodecs.VAR_INT.decode(buffer);
        return switch (kind) {
            case TEXT -> new MessageArg.Text(ByteBufCodecs.STRING_UTF8.decode(buffer));
            case NUMBER -> new MessageArg.Number(ByteBufCodecs.VAR_LONG.decode(buffer));
            case KEY -> new MessageArg.Key(ByteBufCodecs.STRING_UTF8.decode(buffer));
            case DEVICE -> new MessageArg.Device(ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer));
            default -> throw new IllegalArgumentException("Unknown message value kind: " + kind);
        };
    }

    @Override
    public CustomPacketPayload.Type<MessagePayload> type() {
        return TYPE;
    }
}
