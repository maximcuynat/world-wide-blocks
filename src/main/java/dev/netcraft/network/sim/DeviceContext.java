package dev.netcraft.network.sim;

import dev.netcraft.network.DeviceId;
import dev.netcraft.network.protocol.EthernetFrame;
import dev.netcraft.network.protocol.MacAddress;

/**
 * Ce qu'un appareil peut faire pendant qu'il réagit à un événement de la simulation. Un contexte
 * n'est valable que pendant l'appel qui le reçoit.
 */
public interface DeviceContext {

    /** Temps de jeu, en ticks. */
    long now();

    DeviceId device();

    /** Adresse MAC du port. */
    MacAddress mac(int port);

    /** Vrai si un câble est branché sur ce port. */
    boolean isLinked(int port);

    /** Émet une nouvelle trame par ce port. Sans câble, elle est perdue. */
    void send(int port, EthernetFrame frame);

    /**
     * Relaie par ce port la trame en cours de traitement, comme un switch : la simulation la suit
     * pour détecter les boucles. Seulement pendant {@link DeviceLogic#onFrame}.
     */
    void relay(int port, EthernetFrame frame);

    /** Demande un appel à {@link DeviceLogic#onTimer} avec cette clé, dans {@code delayTicks} ticks. */
    void schedule(long delayTicks, Object key);
}
