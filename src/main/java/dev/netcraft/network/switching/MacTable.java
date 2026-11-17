package dev.netcraft.network.switching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Predicate;

import dev.netcraft.network.protocol.MacAddress;

/**
 * Table d'apprentissage d'un switch : pour chaque MAC source vue, le port derrière lequel elle se
 * trouve. Une entrée expire si sa MAC n'a plus émis depuis le délai d'expiration, et disparaît
 * dès que le câble de son port est retiré.
 *
 * <p>Le temps est celui du jeu, en ticks, fourni par l'appelant. Le compteur {@link #version()}
 * augmente à chaque changement visible (entrée ajoutée, déplacée ou retirée), pas quand une entrée
 * est simplement rafraîchie : l'indicateur ne renvoie l'état que s'il a changé.
 *
 * <p>Java pur, sans classe Minecraft. Pas thread-safe.
 */
public final class MacTable {
    /** 300 s, délai d'expiration recommandé par IEEE 802.1D, en ticks de jeu (20 par seconde). */
    public static final long DEFAULT_AGING_TICKS = 300L * 20L;

    /** Ce que la table sait d'une MAC. */
    public record Entry(MacAddress mac, int port, long lastSeen) {
    }

    private final long agingTicks;
    private final Map<MacAddress, Entry> entries = new LinkedHashMap<>();
    private long version;

    public MacTable(long agingTicks) {
        if (agingTicks <= 0) {
            throw new IllegalArgumentException("Délai d'expiration non positif : " + agingTicks);
        }
        this.agingTicks = agingTicks;
    }

    /**
     * Retient que {@code mac} vient d'émettre depuis {@code port}. Une adresse de groupe n'est jamais
     * apprise : elle ne désigne pas un appareil.
     */
    public void learn(MacAddress mac, int port, long now) {
        if (mac.isMulticast()) {
            return;
        }
        Entry previous = this.entries.put(mac, new Entry(mac, port, now));
        if (previous == null || previous.port() != port || this.isExpired(previous, now)) {
            this.version++;
        }
    }

    /** Port derrière lequel se trouve {@code mac}, si elle est connue et n'a pas expiré. */
    public OptionalInt lookup(MacAddress mac, long now) {
        Entry entry = this.entries.get(mac);
        return entry == null || this.isExpired(entry, now) ? OptionalInt.empty() : OptionalInt.of(entry.port());
    }

    /** Oublie tout ce qui était appris derrière ce port (câble retiré). */
    public void flushPort(int port) {
        this.removeIf(entry -> entry.port() == port);
    }

    /** Retire les entrées expirées. */
    public void purgeExpired(long now) {
        this.removeIf(entry -> this.isExpired(entry, now));
    }

    /** Entrées encore présentes, dans l'ordre d'apprentissage. Elles peuvent inclure des entrées expirées pas encore purgées. */
    public List<Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(this.entries.values()));
    }

    /** Augmente à chaque changement visible du contenu de la table. */
    public long version() {
        return this.version;
    }

    private boolean isExpired(Entry entry, long now) {
        return now - entry.lastSeen() >= this.agingTicks;
    }

    private void removeIf(Predicate<Entry> condition) {
        boolean removed = false;
        for (Iterator<Entry> it = this.entries.values().iterator(); it.hasNext(); ) {
            if (condition.test(it.next())) {
                it.remove();
                removed = true;
            }
        }
        if (removed) {
            this.version++;
        }
    }
}
