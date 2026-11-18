package io.github.maximcuynat.worldwideblocks.client;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.message.MessageArg;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Turns a key and values received from the server into text, in the player's language (messages and panel). */
final class ClientText {

    private ClientText() {
    }

    static MutableComponent of(String key, List<MessageArg> args) {
        return Component.translatable(key, args.stream().map(ClientText::arg).toArray());
    }

    static Component arg(MessageArg arg) {
        return switch (arg) {
            case MessageArg.Text text -> Component.literal(text.value());
            case MessageArg.Number number -> Component.literal(Long.toString(number.value()));
            case MessageArg.Key key -> Component.translatable(key.key());
            case MessageArg.Device device -> Component.translatable("message.worldwideblocks.device",
                    Component.translatable(device.nameKey()), device.x(), device.y(), device.z());
        };
    }
}
