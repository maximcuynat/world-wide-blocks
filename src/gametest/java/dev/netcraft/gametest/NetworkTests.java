package dev.netcraft.gametest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.netcraft.NetCraft;
import dev.netcraft.network.DeviceId;
import dev.netcraft.network.Link;
import dev.netcraft.network.NetworkManager;
import dev.netcraft.network.NetworkSavedData;
import dev.netcraft.network.PortRef;
import dev.netcraft.network.protocol.MacAddress;
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
     * H de B : B l'oublie aussitôt (câble retiré), A n'en sait rien et le garde jusqu'à 300 s de jeu
     * après la dernière trame de H, puis l'oublie.
     */
    static void macExpiresAfter300Seconds(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId boxA = scene.place(new BlockPos(4, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId boxB = scene.place(new BlockPos(12, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId host = scene.place(new BlockPos(12, 0, 12), ModBlocks.PC_TOWER.get());
        Link ab = scene.cable(boxA, boxB);
        Link bh = scene.cable(boxB, host);
        MacAddress hostMac = scene.network().macsOf(host).getFirst();

        // La présence de H, émise au branchement, a eu le temps de traverser les deux box.
        helper.runAfterDelay(5, () -> {
            MacTable.Entry learned = learned(scene, boxA, hostMac).orElseThrow(
                    () -> helper.assertionException(Component.literal("A n'a pas appris la MAC de H")));
            helper.assertValueEqual(learned.port(), endOn(ab, boxA).index(), "port de A derrière lequel se trouve H");

            scene.network().disconnect(endOn(bh, host));
            helper.assertTrue(learned(scene, boxB, hostMac).isEmpty(), "B garde la MAC de H alors que son câble est retiré");

            long expiry = learned.lastSeen() + MacTable.DEFAULT_AGING_TICKS;
            long now = scene.gameTime();
            helper.runAfterDelay(expiry - now - 20, () -> helper.assertTrue(learned(scene, boxA, hostMac).isPresent(),
                    "A a oublié la MAC de H avant 300 s"));
            helper.runAfterDelay(expiry - now + 40, () -> {
                helper.assertTrue(learned(scene, boxA, hostMac).isEmpty(), "A garde la MAC de H plus de 300 s");
                helper.succeed();
            });
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
     * Deux box reliées par deux câbles, un poste sur A : chaque trame diffusée tourne en boucle. La
     * boucle est signalée aux joueurs proches, au plus une fois par minute de jeu et par appareil, et
     * les signalements cessent quand on retire un des deux câbles.
     *
     * <p>Lot à part : la limite des signalements et les limites de trames sont globales au moteur.
     */
    static void loopReportedAtMostOncePerMinute(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId boxA = scene.place(new BlockPos(4, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId boxB = scene.place(new BlockPos(12, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId host = scene.place(new BlockPos(4, 0, 12), ModBlocks.PC_TOWER.get());
        Link first = scene.cable(boxA, boxB);
        scene.cable(boxA, boxB);
        TestPlayer player = scene.player(GameType.CREATIVE);
        // Branché en dernier : sa présence, diffusée au branchement puis toutes les 30 s, trouve la boucle.
        scene.cable(host, boxA);

        long interval = SimulationLimits.DEFAULT.reportIntervalTicks();
        long observation = 2 * interval + 200;
        helper.runAfterDelay(observation, () -> {
            Map<String, List<Long>> reports = loopReportTimes(player);
            NetCraft.LOGGER.info("[GameTest] signalements de boucle reçus (temps de jeu) : {}", reports);
            helper.assertTrue(!reports.isEmpty(), "Aucun signalement de boucle en " + observation + " ticks");
            reports.forEach((device, times) -> {
                helper.assertTrue(times.size() >= 2, "Boucle signalée une seule fois en " + observation + " ticks pour " + device);
                for (int i = 1; i < times.size(); i++) {
                    long gap = times.get(i) - times.get(i - 1);
                    helper.assertTrue(gap >= interval, "Deux signalements à " + gap + " ticks d'écart pour " + device);
                }
            });

            int before = player.messages(LOOP).size();
            scene.network().disconnect(first.a());
            helper.runAfterDelay(interval + 100, () -> {
                scene.assertMessages(player, LOOP, before);
                helper.succeed();
            });
        });
    }

    private static Optional<MacTable.Entry> learned(NetworkScene scene, DeviceId device, MacAddress mac) {
        return scene.network().logic(device).map(DeviceLogic::learnedMacs).orElse(List.of()).stream()
                .filter(entry -> entry.mac().equals(mac))
                .findFirst();
    }

    private static PortRef endOn(Link link, DeviceId device) {
        return link.a().device().equals(device) ? link.a() : link.b();
    }

    /** Instants de réception des messages de boucle, par appareil (coordonnées du message). */
    private static Map<String, List<Long>> loopReportTimes(TestPlayer player) {
        Map<String, List<Long>> times = new LinkedHashMap<>();
        for (TestPlayer.Message message : player.messages(LOOP)) {
            List<Object> args = message.args();
            String device = args.size() >= 4 ? args.get(1) + ", " + args.get(2) + ", " + args.get(3) : "?";
            times.computeIfAbsent(device, key -> new ArrayList<>()).add(message.gameTime());
        }
        return times;
    }
}
