package io.github.maximcuynat.worldwideblocks.network;

/** Issue d'une tentative de branchement. */
public enum ConnectResult {
    OK("ok"),
    /** Une des extrémités n'est pas un appareil connu, ou le port n'existe pas sur cet appareil. */
    NOT_A_DEVICE("not_a_device"),
    /** Un des deux ports n'accepte pas ce type de câble (arrivée de ligne de la box, par exemple). */
    INCOMPATIBLE_PORT("incompatible_port"),
    /** Les deux extrémités sont le même appareil. */
    SAME_DEVICE("same_device"),
    /** Les deux appareils ne sont pas dans la même dimension. */
    OTHER_DIMENSION("other_dimension"),
    /** La distance dépasse la longueur maximale du support. */
    TOO_FAR("too_far"),
    /** Un des deux ports a déjà un câble. */
    PORT_IN_USE("port_in_use");

    private final String key;

    ConnectResult(String key) {
        this.key = key;
    }

    /** Suffixe de la clé de traduction du message montré au joueur. */
    public String key() {
        return this.key;
    }
}
