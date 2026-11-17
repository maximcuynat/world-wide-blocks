package dev.netcraft.network.sim;

/** Sens d'une trame vue par le point d'écoute, du point de vue de l'appareil. */
public enum FrameDirection {
    /** La trame arrive sur un port de l'appareil. */
    RECEIVED,
    /** La trame part d'un port de l'appareil (qu'un câble y soit branché ou non). */
    SENT
}
