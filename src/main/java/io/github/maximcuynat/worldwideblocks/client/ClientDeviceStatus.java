package io.github.maximcuynat.worldwideblocks.client;

import io.github.maximcuynat.worldwideblocks.network.sync.DeviceStatusPayload;
import net.minecraft.core.BlockPos;

/**
 * Dernier état d'appareil reçu du serveur, pour l'indicateur. Le serveur ne l'envoie que pour l'appareil
 * visé ; il ne sert que si le joueur vise toujours cette position. Oublié à la déconnexion et à
 * l'arrivée dans une dimension.
 */
final class ClientDeviceStatus {
    private DeviceStatusPayload status;

    void set(DeviceStatusPayload status) {
        this.status = status;
    }

    void clear() {
        this.status = null;
    }

    /** L'état de l'appareil à cette position, ou {@code null} s'il n'est pas connu. */
    DeviceStatusPayload at(BlockPos pos) {
        return this.status != null && this.status.pos().equals(pos) ? this.status : null;
    }
}
