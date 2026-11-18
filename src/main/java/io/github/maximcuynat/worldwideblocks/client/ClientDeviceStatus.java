package io.github.maximcuynat.worldwideblocks.client;

import io.github.maximcuynat.worldwideblocks.network.sync.DeviceStatusPayload;
import net.minecraft.core.BlockPos;

/**
 * Last device state received from the server, for the indicator. The server only sends it for the targeted
 * device; it is only used if the player still targets this position. Forgotten on disconnection and on
 * arrival in a dimension.
 */
final class ClientDeviceStatus {
    private DeviceStatusPayload status;

    void set(DeviceStatusPayload status) {
        this.status = status;
    }

    void clear() {
        this.status = null;
    }

    /** The state of the device at this position, or {@code null} if it is not known. */
    DeviceStatusPayload at(BlockPos pos) {
        return this.status != null && this.status.pos().equals(pos) ? this.status : null;
    }
}
