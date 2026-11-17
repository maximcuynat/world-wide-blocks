package dev.netcraft.message;

import dev.netcraft.network.DeviceId;

/**
 * Une valeur d'un message. Le serveur n'envoie jamais de texte rédigé : seulement des valeurs, que le
 * client met en forme dans sa langue, et que le log écrit en anglais ({@link #logText()}).
 */
public sealed interface MessageArg {

    /** Écriture dans le log : toujours en anglais, avec des identifiants plutôt que des noms traduits. */
    String logText();

    /** Valeur montrée telle quelle, dans toutes les langues : une adresse MAC, un identifiant. */
    record Text(String value) implements MessageArg {
        @Override
        public String logText() {
            return this.value;
        }
    }

    /** Nombre entier. Les distances sont arrondies à l'entier : aucun séparateur décimal, dans aucune langue. */
    record Number(long value) implements MessageArg {
        @Override
        public String logText() {
            return Long.toString(this.value);
        }
    }

    /** Texte à traduire par le client, désigné par sa clé de langue. */
    record Key(String key) implements MessageArg {
        @Override
        public String logText() {
            return this.key;
        }
    }

    /**
     * Un appareil, écrit partout de la même façon : son nom traduit et ses coordonnées côté joueur,
     * son identifiant de bloc, ses coordonnées et sa dimension dans le log.
     *
     * @param type identifiant du bloc, par exemple {@code netcraft:internet_box}
     */
    record Device(String type, String dimension, int x, int y, int z) implements MessageArg {
        /** Clé de langue du nom de l'appareil, par exemple {@code block.netcraft.internet_box}. */
        public String nameKey() {
            return "block." + this.type.replace(':', '.');
        }

        @Override
        public String logText() {
            return this.type + " [" + this.x + ", " + this.y + ", " + this.z + "] " + this.dimension;
        }
    }

    static MessageArg text(String value) {
        return new Text(value);
    }

    static MessageArg number(long value) {
        return new Number(value);
    }

    /** Distance en blocs, arrondie à l'entier le plus proche. */
    static MessageArg blocks(double distance) {
        return new Number(Math.round(distance));
    }

    static MessageArg key(String key) {
        return new Key(key);
    }

    static MessageArg device(DeviceId id, String type) {
        return new Device(type, id.dimension(), id.x(), id.y(), id.z());
    }
}
