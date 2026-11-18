package io.github.maximcuynat.worldwideblocks.network.sync;

import java.util.Objects;

/**
 * Last device state sent to a player, on the server side: only what changed is sent again.
 * One instance per player, held by the player, not saved.
 */
public final class PlayerStatusView {
    private DeviceStatusPayload lastSent;

    /**
     * @param status state of the targeted device, or {@code null} if the player is aiming at no device
     * @return true if {@code status} must be sent to the player
     */
    public boolean update(DeviceStatusPayload status) {
        if (Objects.equals(status, this.lastSent)) {
            return false;
        }
        this.lastSent = status;
        return status != null;
    }

    /** The client just forgot what it knew (arrival in a dimension): everything must be sent again. */
    public void reset() {
        this.lastSent = null;
    }
}
