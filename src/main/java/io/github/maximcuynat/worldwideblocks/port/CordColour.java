package io.github.maximcuynat.worldwideblocks.port;

/**
 * The eight cord colours of {@code design/palette.json} (key {@code cords}, design guide section 4), in its order. The
 * client draws the plug model of the colour of a cable ({@code worldwideblocks:block/rj45_plug_<colour>}) and its cable
 * in the base tone of that colour, taken from the texture of the plugs. A colour carries no rule of the game. Until the
 * colour becomes a property of the link ({@code docs/cable-design.md}, section 7), every cable is {@link #DEFAULT}.
 */
public enum CordColour {
    BLUE("blue"),
    GREY("grey"),
    YELLOW("yellow"),
    GREEN("green"),
    RED("red"),
    ORANGE("orange"),
    BLACK("black"),
    WHITE("white");

    /** The colour of every cord today. */
    public static final CordColour DEFAULT = BLUE;
    /** Size of the texture of the plugs, {@code textures/block/rj45_plug.png}, in texels. */
    public static final int PLUG_TEXTURE_SIZE = 32;

    private final String serializedName;

    CordColour(String serializedName) {
        this.serializedName = serializedName;
    }

    /** Its key in {@code design/palette.json} and in the names of the plug models. */
    public String serializedName() {
        return this.serializedName;
    }

    /** Path of its plug model in the assets of the mod: {@code block/rj45_plug_<colour>}. */
    public String plugModel() {
        return "block/rj45_plug_" + this.serializedName;
    }

    /**
     * A texel of the plug texture in the base tone of this colour: the first texel of the area of its boot
     * ({@code design/blockbench/rj45_plug.md}, x 16 x (n mod 2), y 6 + 6 x (n div 2)).
     */
    public int baseTexelX() {
        return 16 * (this.ordinal() % 2);
    }

    public int baseTexelY() {
        return 6 + 6 * (this.ordinal() / 2);
    }
}
