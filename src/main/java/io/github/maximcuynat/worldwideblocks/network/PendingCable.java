package io.github.maximcuynat.worldwideblocks.network;

/**
 * Règle d'un branchement en attente : un cordon tenu en main dont un bout est déjà branché sur un
 * appareil (premier clic fait, ou bout retiré d'un appareil). Java pur, testable sans le jeu.
 */
public final class PendingCable {

    /** État d'un branchement en attente. */
    public enum Status {
        /** Le branchement peut encore être terminé. */
        VALID,
        /** L'appareil de départ n'existe plus (cassé, explosé, remplacé) : l'attente est annulée. */
        START_GONE,
        /** Celui qui tient le cordon n'est plus dans la dimension du départ : l'attente est annulée. */
        OTHER_DIMENSION
    }

    private PendingCable() {
    }

    /**
     * @param start           port de départ mémorisé sur le cordon
     * @param holderDimension dimension où se trouve celui qui tient le cordon
     */
    public static Status check(NetworkGraph graph, PortRef start, String holderDimension) {
        // Un départ disparu prime : revenir dans la bonne dimension n'y changerait rien.
        if (graph.node(start.device()).filter(node -> node.hasPort(start.index())).isEmpty()) {
            return Status.START_GONE;
        }
        if (!start.device().dimension().equals(holderDimension)) {
            return Status.OTHER_DIMENSION;
        }
        return Status.VALID;
    }
}
