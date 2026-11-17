package dev.netcraft.network;

/**
 * Bloc qui est un appareil réseau. Le nombre de ports est fixe pour un type d'appareil :
 * il ne dépend ni de l'état du bloc ni de sa configuration.
 */
public interface NetworkDevice {

    int portCount();
}
