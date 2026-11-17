package dev.netcraft.gametest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.netcraft.NetCraft;
import dev.netcraft.message.MessageArg;
import dev.netcraft.message.MessageChannel;
import dev.netcraft.message.SentMessage;
import dev.netcraft.network.DeviceId;
import dev.netcraft.network.Link;
import dev.netcraft.network.NetworkManager;
import dev.netcraft.network.NetworkSavedData;
import dev.netcraft.network.PortRef;
import dev.netcraft.network.protocol.MacAddress;
import dev.netcraft.network.sim.BoxLogic;
import dev.netcraft.network.sim.DeviceLogic;
import dev.netcraft.network.sim.SimulationLimits;
import dev.netcraft.network.switching.MacTable;
import dev.netcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

/** Tests en jeu du moteur réseau : commutation, sauvegarde, boucle. */
final class NetworkTests {
    private static final String LOOP = "message.netcraft.network.loop";

    private NetworkTests() {
    }

    /**
     * Box A reliée à la box B, un poste H sur B. A apprend H derrière son port vers B. On débranche
     * H de B : B l'oublie au tick suivant (câble retiré), A n'en sait rien et le garde exactement
     * 300 s de jeu après la dernière trame de H, puis le retire au nettoyage suivant (au plus une
     * seconde après).
     *
     * <p>Vérifié à chaque tick sur le temps de jeu : un rappel de GameTest s'exécute pendant le tick
     * {@code T}, avant que la simulation traite ce tick ; elle a donc traité les ticks jusqu'à {@code T - 1}.
     */
    static void macExpiresAfter300Seconds(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId boxA = scene.place(new BlockPos(4, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId boxB = scene.place(new BlockPos(12, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId host = scene.place(new BlockPos(12, 0, 12), ModBlocks.PC_TOWER.get());
        Link ab = scene.cable(boxA, boxB);
        Link bh = scene.cable(boxB, host);
        MacAddress hostMac = scene.network().macsOf(host).getFirst();

        long[] unplugged = {-1};
        long[] expiry = {-1};
        helper.onEachTick(() -> {
            long processed = scene.gameTime() - 1;
            if (unplugged[0] < 0) {
                Optional<MacTable.Entry> learned = learned(scene, boxA, hostMac);
                if (learned.isEmpty()) {
                    return; // la présence de H n'a pas encore été livrée
                }
                helper.assertValueEqual(learned.get().port(), endOn(ab, boxA).index(), "port de A derrière lequel se trouve H");
                scene.network().disconnect(endOn(bh, host));
                unplugged[0] = processed + 1; // appliqué par la simulation à ce tick
                expiry[0] = learned.get().lastSeen() + MacTable.DEFAULT_AGING_TICKS;
                return;
            }
            if (processed >= unplugged[0]) {
                helper.assertTrue(learned(scene, boxB, hostMac).isEmpty(), "B garde la MAC de H un tick après le retrait de son câble");
            }
            if (processed < expiry[0]) {
                helper.assertTrue(learned(scene, boxA, hostMac).isPresent(),
                        "A a oublié la MAC de H au tick " + processed + ", avant son expiration au tick " + expiry[0]);
            } else if (processed >= expiry[0] + BoxLogic.PURGE_INTERVAL_TICKS - 1) {
                helper.assertTrue(learned(scene, boxA, hostMac).isEmpty(),
                        "A garde la MAC de H au tick " + processed + ", plus d'une seconde après son expiration au tick " + expiry[0]);
                helper.succeed();
            }
        });
    }

    /**
     * Le réseau est écrit dans {@code data/netcraft/network.dat} comme à une sauvegarde du monde,
     * puis relu par un stockage neuf, comme au chargement du monde : appareils, MAC et câbles
     * identiques. (Le serveur, lui, ne redémarre pas.)
     */
    static void networkSurvivesSaveAndReload(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(8, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        DeviceId laptop = scene.place(new BlockPos(12, 0, 8), ModBlocks.LAPTOP.get());
        scene.cable(box, tower);
        scene.cable(box, laptop);

        MinecraftServer server = helper.getLevel().getServer();
        server.getDataStorage().saveAndJoin();
        try (SavedDataStorage disk = new SavedDataStorage(server.overworld(), server.getWorldPath(LevelResource.DATA),
                server.getFixerUpper(), server.registryAccess())) {
            NetworkSavedData reloaded = disk.get(NetworkSavedData.TYPE);
            helper.assertTrue(reloaded != null, "network.dat absent ou illisible");
            NetworkManager live = scene.network();
            for (DeviceId id : List.of(box, tower, laptop)) {
                helper.assertValueEqual(reloaded.node(id), live.node(id), "appareil " + id + " relu");
                helper.assertValueEqual(reloaded.macsOf(id), live.macsOf(id), "MAC de " + id + " relues");
                helper.assertValueEqual(Set.copyOf(reloaded.linksOf(id)), Set.copyOf(live.linksOf(id)), "câbles de " + id + " relus");
            }
        }
        helper.succeed();
    }

    /**
     * Deux box reliées par deux câbles, un poste sur A : chaque trame diffusée tourne en boucle, et les
     * deux box la voient. Le joueur proche reçoit **un seul** message par minute de jeu (pas un par
     * box), qui nomme une des deux box, et le log n'a qu'une ligne par message. La présence du poste
     * revient toutes les 30 s : la limite d'une minute est donc vraiment mise à l'épreuve. Les
     * messages cessent quand on retire un des deux câbles.
     *
     * <p>Lot à part : la limite des trames est globale au moteur.
     */
    static void loopReportedOncePerMinute(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId boxA = scene.place(new BlockPos(4, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId boxB = scene.place(new BlockPos(12, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId host = scene.place(new BlockPos(4, 0, 12), ModBlocks.PC_TOWER.get());
        Link firstCable = scene.cable(boxA, boxB);
        scene.cable(boxA, boxB);
        TestPlayer player = scene.player(GameType.CREATIVE);
        // Branché en dernier : sa présence, diffusée au branchement puis toutes les 30 s, trouve la boucle.
        scene.cable(host, boxA);
        Set<DeviceId> boxes = Set.of(boxA, boxB);

        long interval = SimulationLimits.DEFAULT.reportIntervalTicks();
        long observation = 2 * interval + 200;
        helper.runAfterDelay(observation, () -> {
            List<SentMessage> chat = loopChat(scene, player, boxes);
            List<Long> times = chat.stream().map(SentMessage::gameTime).toList();
            NetCraft.LOGGER.info("[GameTest] loop messages received at game times {}", times);
            helper.assertTrue(!times.isEmpty(), "Aucun message de boucle en " + observation + " ticks");
            // Exactement un message par minute, à 0, 1 200 et 2 400 ticks du premier. Un écart plus grand
            // (1 800 avant la correction de l'horloge du 2026-11-18) est un défaut, pas une marge.
            long firstReport = times.getFirst();
            helper.assertValueEqual(times, List.of(firstReport, firstReport + interval, firstReport + 2 * interval),
                    "instants des messages de boucle (un seul par minute pour les deux box)");
            long logLines = scene.messages().logs(LOOP).stream().filter(message -> names(message, boxes)).count();
            helper.assertValueEqual(logLines, 3L, "lignes du log pour cette boucle");

            int before = chat.size();
            scene.network().disconnect(firstCable.a());
            helper.runAfterDelay(interval + 100, () -> {
                helper.assertValueEqual(loopChat(scene, player, boxes).size(), before, "messages de boucle après le retrait d'un câble");
                helper.succeed();
            });
        });
    }

    /**
     * Un changement du réseau en boucle fait oublier sa limite (la boucle est resignalée aussitôt) ;
     * un changement dans un autre réseau ne la touche pas.
     *
     * <ul>
     * <li>S+1 : présence du premier poste, premier message ;</li>
     * <li>S+100 : un second poste branché sur la box B (ce réseau change) : second message au tick suivant ;</li>
     * <li>S+200 : deux tours reliées ailleurs (un autre réseau change) ;</li>
     * <li>S+601 et S+701 : présences des deux postes, dans la minute du second message : rien.</li>
     * </ul>
     * Un oubli global donnerait un troisième message à S+601 ; aucun oubli, un seul message en tout.
     *
     * <p>Lot à part, comme le test précédent.
     */
    static void loopForgottenOnlyForChangedNetwork(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId boxA = scene.place(new BlockPos(4, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId boxB = scene.place(new BlockPos(12, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId host = scene.place(new BlockPos(4, 0, 12), ModBlocks.PC_TOWER.get());
        DeviceId secondHost = scene.place(new BlockPos(12, 0, 12), ModBlocks.PC_TOWER.get());
        scene.cable(boxA, boxB);
        scene.cable(boxA, boxB);
        TestPlayer player = scene.player(GameType.CREATIVE);
        scene.cable(host, boxA);
        Set<DeviceId> boxes = Set.of(boxA, boxB);

        helper.runAfterDelay(100, () -> {
            helper.assertValueEqual(loopChat(scene, player, boxes).size(), 1, "messages de boucle avant le second poste");
            scene.cable(secondHost, boxB);
            helper.runAfterDelay(100, () -> {
                helper.assertValueEqual(loopChat(scene, player, boxes).size(), 2, "messages de boucle après le changement de ce réseau");
                DeviceId towerA = scene.place(new BlockPos(1, 0, 1), ModBlocks.PC_TOWER.get());
                DeviceId towerB = scene.place(new BlockPos(3, 0, 1), ModBlocks.PC_TOWER.get());
                scene.cable(towerA, towerB);
                helper.runAfterDelay(600, () -> {
                    helper.assertValueEqual(loopChat(scene, player, boxes).size(), 2,
                            "messages de boucle après un changement dans un autre réseau");
                    helper.succeed();
                });
            });
        });
    }

    /** Messages de boucle reçus par le joueur et qui nomment une de ces box (le lot est partagé avec d'autres boucles). */
    private static List<SentMessage> loopChat(NetworkScene scene, TestPlayer player, Set<DeviceId> boxes) {
        return scene.messages().to(player.getUUID(), MessageChannel.CHAT, LOOP).stream()
                .filter(message -> names(message, boxes))
                .toList();
    }

    private static boolean names(SentMessage message, Set<DeviceId> devices) {
        return deviceOf(message).filter(devices::contains).isPresent();
    }

    /** Appareil nommé par la première valeur d'un message. */
    private static Optional<DeviceId> deviceOf(SentMessage message) {
        if (message.args().isEmpty() || !(message.args().getFirst() instanceof MessageArg.Device device)) {
            return Optional.empty();
        }
        return Optional.of(new DeviceId(device.dimension(), device.x(), device.y(), device.z()));
    }

    private static Optional<MacTable.Entry> learned(NetworkScene scene, DeviceId device, MacAddress mac) {
        return scene.network().logic(device).map(DeviceLogic::learnedMacs).orElse(List.of()).stream()
                .filter(entry -> entry.mac().equals(mac))
                .findFirst();
    }

    private static PortRef endOn(Link link, DeviceId device) {
        return link.a().device().equals(device) ? link.a() : link.b();
    }
}
