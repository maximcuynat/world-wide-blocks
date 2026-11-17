package dev.netcraft.network.sim;

/**
 * Garde-fous de la simulation, pour qu'un réseau mal câblé ne fasse jamais tomber le serveur.
 *
 * @param maxDeliveriesPerTick trames livrées au plus par tick ; les suivantes attendent le tick d'après
 * @param maxQueued            trames en attente au plus ; au-delà, les nouvelles sont perdues
 * @param reportIntervalTicks  délai minimal entre deux signalements du même problème, appliqué par le
 *                             module des messages (la simulation, elle, signale chaque tick où elle voit le problème)
 */
public record SimulationLimits(int maxDeliveriesPerTick, int maxQueued, long reportIntervalTicks) {
    /** Valeurs validées par Max le 2026-11-17 : 10 000 trames par tick, 50 000 en file, un signalement par minute. */
    public static final SimulationLimits DEFAULT = new SimulationLimits(10_000, 50_000, 60L * 20L);

    public SimulationLimits {
        if (maxDeliveriesPerTick <= 0 || maxQueued <= 0 || reportIntervalTicks < 0) {
            throw new IllegalArgumentException("Invalid limits");
        }
    }
}
