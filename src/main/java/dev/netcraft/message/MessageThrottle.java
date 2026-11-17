package dev.netcraft.message;

import java.util.HashMap;
import java.util.Map;

/**
 * Anti-répétition des événements : un même problème, désigné par sa clé, n'est transmis qu'une fois
 * par intervalle, quel que soit le nombre d'appareils qui le voient. Le temps est celui des ticks de la
 * simulation, en temps de jeu.
 *
 * <p>Les clés d'un problème lié au réseau sont oubliées quand le réseau change
 * ({@link #forget(String)}) : le joueur qui croit avoir réparé l'apprend au signalement suivant, sans
 * attendre la fin de l'intervalle. Le nombre de clés reste donc petit : une par problème en cours.
 */
public final class MessageThrottle {
    private final long intervalTicks;
    private final Map<String, Long> lastSent = new HashMap<>();

    public MessageThrottle(long intervalTicks) {
        if (intervalTicks < 0) {
            throw new IllegalArgumentException("Negative interval: " + intervalTicks);
        }
        this.intervalTicks = intervalTicks;
    }

    /** Vrai si le problème peut être transmis maintenant ; le cas échéant, l'instant est retenu. */
    public boolean allow(String problemKey, long now) {
        Long last = this.lastSent.get(problemKey);
        if (last != null && now - last < this.intervalTicks) {
            return false;
        }
        this.lastSent.put(problemKey, now);
        return true;
    }

    /** Oublie tous les problèmes dont la clé commence par {@code prefix}. */
    public void forget(String prefix) {
        this.lastSent.keySet().removeIf(key -> key.startsWith(prefix));
    }
}
