package dev.netcraft.network.protocol;

/**
 * Trame de présence : un poste la diffuse au branchement de son câble puis toutes les 30 s, pour
 * qu'un switch apprenne sa MAC alors que le poste n'a encore aucun autre trafic.
 *
 * <p><b>Échafaudage du lot 1b-1, à retirer au lot 1c</b>, quand le DHCP et l'ARP produiront du vrai
 * trafic. Elle n'imite aucun protocole réel : elle utilise le type réservé aux essais
 * ({@link EtherType#LOCAL_EXPERIMENTAL}) et ne transporte rien.
 */
public record PresencePayload() implements EthernetPayload {
    public static final PresencePayload INSTANCE = new PresencePayload();

    @Override
    public EtherType etherType() {
        return EtherType.LOCAL_EXPERIMENTAL;
    }
}
