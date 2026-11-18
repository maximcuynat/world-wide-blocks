package io.github.maximcuynat.worldwideblocks.gametest.scene;

import java.util.List;
import java.util.function.Supplier;

import io.github.maximcuynat.worldwideblocks.registry.ModBlocks;
import net.minecraft.world.level.block.Block;

/**
 * Décors des essais à l'œil, construits devant le joueur par {@code /worldwideblocks test <scénario>}.
 *
 * <p>Coordonnées locales : {@code right} vers la droite du joueur, {@code forward} droit devant lui,
 * à partir d'un point situé 3 blocs devant ses pieds.
 */
public enum TrialScenario {
    /** Box seule : 4 ports réseau local libres, arrivée de ligne inactive, façade vers le joueur. */
    BOX("box", "box seule",
            List.of(device(0, 0, ModBlocks.INTERNET_BOX::get)),
            List.of()),
    /** Box et 3 postes : MAC de chaque poste, MAC apprises par la box sur 3 ports. */
    BOX_POSTES("box_postes", "box et 3 postes branchés",
            List.of(device(0, 0, ModBlocks.INTERNET_BOX::get), device(-3, 3, ModBlocks.PC_TOWER::get),
                    device(0, 4, ModBlocks.LAPTOP::get), device(3, 3, ModBlocks.PC_TOWER::get)),
            List.of(cable(0, 1), cable(0, 2), cable(0, 3))),
    /**
     * Deux box reliées, un poste sur celle de gauche et deux sur celle de droite : chaque box apprend
     * aussi les postes de l'autre, et le port 1 de la box de gauche porte 2 MAC (lisibilité du panneau).
     */
    CHAINE("chaine", "deux box reliées, un poste à gauche, deux à droite",
            List.of(device(-3, 0, ModBlocks.INTERNET_BOX::get), device(3, 0, ModBlocks.INTERNET_BOX::get),
                    device(-3, 3, ModBlocks.PC_TOWER::get), device(3, 3, ModBlocks.LAPTOP::get),
                    device(6, 3, ModBlocks.PC_TOWER::get)),
            List.of(cable(0, 1), cable(0, 2), cable(1, 3), cable(1, 4))),
    /** Deux box reliées par deux câbles, un poste : message de boucle dans le chat, une fois par minute. */
    BOUCLE("boucle", "deux box reliées par deux câbles, un poste",
            List.of(device(-3, 0, ModBlocks.INTERNET_BOX::get), device(3, 0, ModBlocks.INTERNET_BOX::get),
                    device(-3, 3, ModBlocks.PC_TOWER::get)),
            List.of(cable(0, 1), cable(0, 1), cable(0, 2))),
    /** Box aux 4 ports réseau local occupés : un cordon de plus affiche « aucun port libre ». */
    PORTS_PLEINS("ports_pleins", "box aux 4 ports occupés",
            List.of(device(0, 0, ModBlocks.INTERNET_BOX::get), device(-3, 3, ModBlocks.PC_TOWER::get),
                    device(-1, 4, ModBlocks.PC_TOWER::get), device(1, 4, ModBlocks.LAPTOP::get),
                    device(3, 3, ModBlocks.LAPTOP::get)),
            List.of(cable(0, 1), cable(0, 2), cable(0, 3), cable(0, 4))),
    /** Câble de 99 blocs, juste sous la limite de 100 : dessin d'un long câble. */
    DISTANCE("distance", "câble de 99 blocs",
            List.of(device(0, 0, ModBlocks.PC_TOWER::get), device(0, 99, ModBlocks.LAPTOP::get)),
            List.of(cable(0, 1)));

    /** Un appareil du décor. */
    public record Device(int right, int forward, Supplier<Block> block) {
    }

    /** Un câble entre deux appareils, désignés par leur rang dans la liste. */
    public record Cable(int from, int to) {
    }

    private final String id;
    private final String description;
    private final List<Device> devices;
    private final List<Cable> cables;

    TrialScenario(String id, String description, List<Device> devices, List<Cable> cables) {
        this.id = id;
        this.description = description;
        this.devices = devices;
        this.cables = cables;
    }

    public String id() {
        return this.id;
    }

    public String description() {
        return this.description;
    }

    public List<Device> devices() {
        return this.devices;
    }

    public List<Cable> cables() {
        return this.cables;
    }

    private static Device device(int right, int forward, Supplier<Block> block) {
        return new Device(right, forward, block);
    }

    private static Cable cable(int from, int to) {
        return new Cable(from, to);
    }
}
