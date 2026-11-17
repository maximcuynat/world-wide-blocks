package dev.netcraft.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Bug relevé en jeu le 2026-11-17 : on relie une box A à un appareil B, on retire le bout côté B
 * (le cordon reste accroché à A, en attente), on change d'emplacement dans la barre d'objets, puis on
 * casse A. Le cordon restait en attente alors que son point de départ n'existait plus.
 */
class PendingCableTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    private NetworkGraph graph;
    private PortRef depart;
    private PortRef autre;

    @BeforeEach
    void setUp() {
        this.graph = new NetworkGraph(new SplittableRandom(3));
        DeviceId box = new DeviceId(OVERWORLD, 0, 64, 0);
        DeviceId tour = new DeviceId(OVERWORLD, 5, 64, 0);
        this.graph.putNode(new NetworkNode(box, "netcraft:internet_box", DeviceRole.BOX,
                List.of(PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE)));
        this.graph.putNode(new NetworkNode(tour, "netcraft:pc_tower", 1));
        this.depart = new PortRef(box, 0);
        this.autre = new PortRef(tour, 0);
    }

    /** Étapes 1 et 2 du bug : A relié à B, puis le bout côté B retiré ; le cordon attend, accroché à A. */
    private void relierPuisRetirerLeBoutCoteB() {
        this.graph.connect(this.depart, this.autre, LinkMedium.COPPER_ETHERNET, true);
        this.graph.disconnect(this.autre);
    }

    @Test
    void lAttenteResteValableTantQueLeDepartExiste() {
        relierPuisRetirerLeBoutCoteB();

        assertEquals(PendingCable.Status.VALID, PendingCable.check(graph, depart, OVERWORLD));
    }

    @Test
    void casserLeDepartAnnuleLAttente() {
        relierPuisRetirerLeBoutCoteB();

        // Casse ou explosion : même chemin, l'appareil quitte le moteur.
        graph.removeDevice(depart.device());

        assertEquals(PendingCable.Status.START_GONE, PendingCable.check(graph, depart, OVERWORLD));
    }

    @Test
    void unDepartRemplaceParUnAppareilSansCePortAnnuleLAttente() {
        PortRef port4 = new PortRef(depart.device(), 3);

        graph.putNode(new NetworkNode(depart.device(), "netcraft:pc_tower", 1));

        assertEquals(PendingCable.Status.START_GONE, PendingCable.check(graph, port4, OVERWORLD));
    }

    @Test
    void changerDeDimensionAnnuleLAttente() {
        relierPuisRetirerLeBoutCoteB();

        assertEquals(PendingCable.Status.OTHER_DIMENSION, PendingCable.check(graph, depart, NETHER));
    }

    @Test
    void unDepartDisparuPrimeSurLeChangementDeDimension() {
        relierPuisRetirerLeBoutCoteB();
        graph.removeDevice(depart.device());

        assertEquals(PendingCable.Status.START_GONE, PendingCable.check(graph, depart, NETHER));
    }
}
