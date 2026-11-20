package io.github.maximcuynat.worldwideblocks.terminal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;

/**
 * Who is at the keyboard of each computer: one player at a time, and one computer at a time per player. Each watch
 * keeps the updates of the console not sent yet. Runtime state of the server, never saved.
 */
public final class Keyboards {

    /** Result of a player's attempt to take a keyboard. */
    public enum Take {
        /** The keyboard was free: the player has it now. */
        TAKEN,
        /** The player already had it: the screen is sent again. */
        AGAIN,
        /** Another player has it ("computer busy"). */
        BUSY
    }

    /** A player at a keyboard, watching one session of its console. */
    public static final class Watch {
        private final DeviceId device;
        private final UUID player;
        private final int session;
        private final List<TerminalUpdate> pending = new ArrayList<>();

        Watch(DeviceId device, UUID player, int session) {
            this.device = device;
            this.player = player;
            this.session = session;
        }

        public DeviceId device() {
            return this.device;
        }

        public UUID player() {
            return this.player;
        }

        public int session() {
            return this.session;
        }

        /** True once the shell watched has ended: the screen is closed after its last updates. */
        public boolean ended() {
            return this.pending.stream().anyMatch(TerminalUpdate.Ended.class::isInstance);
        }

        public void offer(TerminalUpdate update) {
            this.pending.add(update);
        }

        /** The updates not sent yet, in packets of at most {@code perPacket}; the watch keeps none. */
        public List<List<TerminalUpdate>> drain(int perPacket) {
            List<List<TerminalUpdate>> packets = new ArrayList<>();
            for (int from = 0; from < this.pending.size(); from += perPacket) {
                packets.add(List.copyOf(this.pending.subList(from, Math.min(this.pending.size(), from + perPacket))));
            }
            this.pending.clear();
            return packets;
        }
    }

    private final Map<DeviceId, Watch> byDevice = new LinkedHashMap<>();
    private final Map<UUID, DeviceId> byPlayer = new HashMap<>();

    /**
     * A player opens the screen of a computer, on this session of its console. A player at another keyboard leaves it
     * first ({@link #release} of that one is the caller's, who must also stop listening to it).
     */
    public Take take(DeviceId device, UUID player, int session) {
        Watch current = this.byDevice.get(device);
        if (current != null && !current.player.equals(player)) {
            return Take.BUSY;
        }
        Take result = current == null ? Take.TAKEN : Take.AGAIN;
        this.byDevice.put(device, new Watch(device, player, session));
        this.byPlayer.put(player, device);
        return result;
    }

    public Optional<Watch> watch(DeviceId device) {
        return Optional.ofNullable(this.byDevice.get(device));
    }

    /** The computer whose keyboard this player has, if any. */
    public Optional<DeviceId> deviceOf(UUID player) {
        return Optional.ofNullable(this.byPlayer.get(player));
    }

    /** The keyboard of this computer is free again. */
    public Optional<Watch> release(DeviceId device) {
        Watch watch = this.byDevice.remove(device);
        if (watch != null) {
            this.byPlayer.remove(watch.player);
        }
        return Optional.ofNullable(watch);
    }

    public List<Watch> watches() {
        return List.copyOf(this.byDevice.values());
    }
}
