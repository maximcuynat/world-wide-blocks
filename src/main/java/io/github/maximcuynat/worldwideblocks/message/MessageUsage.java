package io.github.maximcuynat.worldwideblocks.message;

/** Pourquoi un message existe ; avec son niveau, il décide du canal (voir {@link MessageRouting}). */
public enum MessageUsage {
    /** Réponse immédiate au geste d'un joueur (branchement, refus, débranchement). */
    GESTURE,
    /** Événement du réseau, sans geste du joueur (boucle, réseau saturé). */
    EVENT
}
