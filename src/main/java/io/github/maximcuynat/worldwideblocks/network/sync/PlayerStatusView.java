package io.github.maximcuynat.worldwideblocks.network.sync;

import java.util.Objects;

/**
 * Dernier état d'appareil envoyé à un joueur, côté serveur : on ne renvoie que ce qui a changé.
 * Une instance par joueur, portée par le joueur, non sauvegardée.
 */
public final class PlayerStatusView {
    private DeviceStatusPayload lastSent;

    /**
     * @param status état de l'appareil visé, ou {@code null} si le joueur ne vise aucun appareil
     * @return vrai s'il faut envoyer {@code status} au joueur
     */
    public boolean update(DeviceStatusPayload status) {
        if (Objects.equals(status, this.lastSent)) {
            return false;
        }
        this.lastSent = status;
        return status != null;
    }

    /** Le client vient d'oublier ce qu'il savait (arrivée dans une dimension) : tout est à renvoyer. */
    public void reset() {
        this.lastSent = null;
    }
}
