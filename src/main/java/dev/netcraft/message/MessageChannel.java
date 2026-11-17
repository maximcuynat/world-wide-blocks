package dev.netcraft.message;

/**
 * Où un message est montré. Le panneau d'état n'en fait pas partie : il n'est pas envoyé comme un
 * message, le client le construit à partir de l'état de l'appareil visé.
 */
public enum MessageChannel {
    /** Au-dessus de la barre d'objets, au joueur qui vient d'agir. */
    ACTION_BAR,
    /** Dans le chat, pour les événements rares et importants. */
    CHAT,
    /** Dans le log du serveur, en anglais. */
    LOG
}
