package io.github.maximcuynat.worldwideblocks.network.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.network.ChunkKey;
import io.github.maximcuynat.worldwideblocks.network.Link;

/**
 * What a player is allowed to know about the network: only the cables with one end
 * in a chunk its client has received. A player therefore cannot locate devices outside
 * their view by reading the packets.
 *
 * <p>The view keeps its own list of received chunks ({@code Sent} event) and removed ones
 * ({@code UnWatch}) instead of querying Minecraft's tracking view: during {@code UnWatch},
 * it is not updated yet and would give wrong results during a teleport.
 *
 * <p>Plain Java: testable without the game. One instance per player, held by the player server side.
 */
public final class PlayerLinkView {
    private String dimension;
    private final Set<ChunkKey> watched = new HashSet<>();
    private final Set<UUID> known = new HashSet<>();

    /**
     * The player receives chunks of this dimension. If the player just changed dimension (or just arrived),
     * the view starts again from zero.
     *
     * @return true if the client must forget all its cables
     */
    public boolean enterDimension(String newDimension) {
        if (Objects.equals(this.dimension, newDimension)) {
            return false;
        }
        this.dimension = newDimension;
        this.watched.clear();
        this.known.clear();
        return true;
    }

    /**
     * The client just received this chunk.
     *
     * @param touching cables with one end in this chunk
     * @return the cables to send to it, those it does not know yet
     */
    public List<Link> chunkSent(ChunkKey chunk, Collection<Link> touching) {
        if (!chunk.dimension().equals(this.dimension)) {
            return List.of();
        }
        this.watched.add(chunk);
        List<Link> toSend = new ArrayList<>();
        for (Link link : touching) {
            if (this.known.add(link.id())) {
                toSend.add(link);
            }
        }
        return toSend;
    }

    /**
     * The player no longer tracks this chunk. A cable is forgotten only if neither of its two ends
     * is still in a tracked chunk.
     *
     * @param touching cables with one end in this chunk
     * @return the identifiers of the cables the client must forget
     */
    public List<UUID> chunkUnwatched(ChunkKey chunk, Collection<Link> touching) {
        if (!this.watched.remove(chunk)) {
            return List.of();
        }
        List<UUID> toForget = new ArrayList<>();
        for (Link link : touching) {
            if (!this.isVisible(link) && this.known.remove(link.id())) {
                toForget.add(link.id());
            }
        }
        return toForget;
    }

    /** A cable was just connected: true if it must be sent to this player. */
    public boolean linkAdded(Link link) {
        return this.isVisible(link) && this.known.add(link.id());
    }

    /** A cable was just removed: true if this player knew it and must forget it. */
    public boolean linkRemoved(UUID id) {
        return this.known.remove(id);
    }

    public boolean knows(UUID id) {
        return this.known.contains(id);
    }

    private boolean isVisible(Link link) {
        return this.watched.contains(ChunkKey.of(link.a().device())) || this.watched.contains(ChunkKey.of(link.b().device()));
    }
}
