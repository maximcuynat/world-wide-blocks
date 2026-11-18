package io.github.maximcuynat.worldwideblocks.network;

/** Outcome of an attempt to connect. */
public enum ConnectResult {
    OK("ok"),
    /** One end is not a known device, or the port does not exist on that device. */
    NOT_A_DEVICE("not_a_device"),
    /** One of the two ports does not accept this kind of cable (the box's line input, for example). */
    INCOMPATIBLE_PORT("incompatible_port"),
    /** Both ends are the same device. */
    SAME_DEVICE("same_device"),
    /** The two devices are not in the same dimension. */
    OTHER_DIMENSION("other_dimension"),
    /** The distance exceeds the maximum length of the medium. */
    TOO_FAR("too_far"),
    /** One of the two ports already has a cable. */
    PORT_IN_USE("port_in_use");

    private final String key;

    ConnectResult(String key) {
        this.key = key;
    }

    /** Suffix of the translation key of the message shown to the player. */
    public String key() {
        return this.key;
    }
}
