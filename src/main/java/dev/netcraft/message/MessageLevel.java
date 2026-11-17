package dev.netcraft.message;

/**
 * Gravité d'un message. Le serveur n'envoie que le niveau : c'est le client qui en tire l'apparence
 * (symbole ou mot, couleur), toujours avec un symbole ou un mot, jamais la couleur seule.
 */
public enum MessageLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR;

    /** Avertissement ou erreur : un problème que le joueur doit connaître. */
    public boolean isProblem() {
        return this == WARNING || this == ERROR;
    }
}
