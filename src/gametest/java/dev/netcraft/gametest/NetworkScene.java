package dev.netcraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.netcraft.network.ConnectOutcome;
import dev.netcraft.network.DeviceId;
import dev.netcraft.network.Link;
import dev.netcraft.network.LinkMedium;
import dev.netcraft.network.NetworkDevices;
import dev.netcraft.network.NetworkManager;
import dev.netcraft.network.PortRef;
import dev.netcraft.registry.ModDataComponents;
import dev.netcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Décor réseau d'un test : appareils posés comme de vrais blocs, câbles, joueurs de test.
 *
 * <p>Tous les tests partagent le même moteur réseau. À la fin du test, réussi ou non, la scène
 * déconnecte ses joueurs et retire ses appareils du monde et du moteur : rien de ce test ne continue
 * à émettre des trames pendant les suivants.
 */
public final class NetworkScene implements GameTestListener {
    private final GameTestHelper helper;
    private final List<BlockPos> devices = new ArrayList<>();
    private final List<TestPlayer> players = new ArrayList<>();

    private NetworkScene(GameTestHelper helper) {
        this.helper = helper;
    }

    public static NetworkScene of(GameTestHelper helper) {
        NetworkScene scene = new NetworkScene(helper);
        helper.testInfo.addListener(scene);
        return scene;
    }

    public GameTestHelper helper() {
        return this.helper;
    }

    public NetworkManager network() {
        return NetworkManager.of(this.helper.getLevel().getServer());
    }

    /** Pose un appareil (bloc réel : il s'enregistre dans le moteur comme posé par un joueur). */
    public DeviceId place(BlockPos relative, Block block) {
        this.helper.setBlock(relative, block);
        BlockPos absolute = this.helper.absolutePos(relative);
        this.devices.add(absolute);
        DeviceId id = this.id(relative);
        this.helper.assertTrue(this.network().node(id).isPresent(), "Appareil non enregistré dans le moteur : " + id);
        return id;
    }

    public DeviceId id(BlockPos relative) {
        return NetworkDevices.idOf(this.helper.getLevel(), this.helper.absolutePos(relative));
    }

    /** Relie le premier port libre de chaque appareil, sans cordon consommé (comme en créatif). */
    public Link cable(DeviceId a, DeviceId b) {
        PortRef portA = this.firstFreePort(a);
        PortRef portB = this.firstFreePort(b);
        ConnectOutcome outcome = this.network().connect(portA, portB, LinkMedium.COPPER_ETHERNET, false);
        this.helper.assertTrue(outcome.isSuccess(), "Câble refusé entre " + portA + " et " + portB + " : " + outcome.result());
        return outcome.link().orElseThrow();
    }

    private PortRef firstFreePort(DeviceId device) {
        return this.network().firstFreePort(device, LinkMedium.COPPER_ETHERNET)
                .orElseThrow(() -> this.helper.assertionException(Component.literal("Aucun port libre sur " + device)));
    }

    /** Connecte un nouveau joueur de test, debout dans la zone du test. */
    public TestPlayer player(GameType gameType) {
        return this.player(UUID.randomUUID(), gameType);
    }

    /** Connecte un joueur avec cet identifiant : s'il a déjà joué dans ce monde, c'est une reconnexion. */
    public TestPlayer player(UUID id, GameType gameType) {
        TestPlayer player = TestPlayer.connect(this.helper, id, gameType, new Vec3(1.5, 0.0, 1.5));
        this.players.add(player);
        return player;
    }

    /**
     * Clic droit sur un appareil avec ce qui est en main, par le même chemin qu'un clic reçu d'un vrai
     * client (événement de NeoForge, {@code onItemUseFirst}, bloc, puis objet).
     */
    public InteractionResult rightClick(TestPlayer player, BlockPos relative) {
        BlockPos absolute = this.helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        return player.gameMode.useItemOn(player, this.helper.getLevel(),
                player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hit);
    }

    /** Accroupi + clic droit sur un appareil. */
    public InteractionResult sneakRightClick(TestPlayer player, BlockPos relative) {
        player.setShiftKeyDown(true);
        try {
            return this.rightClick(player, relative);
        } finally {
            player.setShiftKeyDown(false);
        }
    }

    /** Le joueur casse l'appareil, par le même chemin que dans le jeu (gouttes selon son mode de jeu). */
    public void breakAsPlayer(TestPlayer player, BlockPos relative) {
        this.helper.assertTrue(player.gameMode.destroyBlock(this.helper.absolutePos(relative)), "Casse refusée en " + relative);
    }

    /** Explosion centrée sur ce bloc, qui détruit les blocs comme une créature explosive. */
    public void explodeAt(BlockPos relative, float power) {
        Vec3 center = Vec3.atCenterOf(this.helper.absolutePos(relative));
        this.helper.getLevel().explode(null, center.x, center.y, center.z, power, Level.ExplosionInteraction.BLOCK);
    }

    /** Temps de jeu en ticks, celui de la simulation réseau. */
    public long gameTime() {
        return this.helper.getLevel().getServer().overworld().getGameTime();
    }

    /** Vrai si un cordon de l'inventaire attend encore d'être branché. */
    public boolean hasPendingCord(TestPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).has(ModDataComponents.CABLE_START.get())) {
                return true;
            }
        }
        return false;
    }

    /** Cordons dans l'inventaire du joueur. */
    public int cordsHeldBy(TestPlayer player) {
        return player.getInventory().countItem(ModItems.ETHERNET_CABLE.get());
    }

    /** Cordons tombés au sol dans la zone du test. */
    public int cordsOnGround() {
        int count = 0;
        for (ItemEntity item : this.helper.getLevel().getEntitiesOfClass(ItemEntity.class, this.helper.getBoundsWithPadding())) {
            if (item.getItem().is(ModItems.ETHERNET_CABLE.get())) {
                count += item.getItem().getCount();
            }
        }
        return count;
    }

    /** Vérifie que le joueur a reçu exactement {@code expected} messages de cette clé. */
    public void assertMessages(TestPlayer player, String key, int expected) {
        int received = player.messages(key).size();
        this.helper.assertTrue(received == expected, "Message " + key + " reçu " + received + " fois au lieu de "
                + expected + ". Messages reçus : " + player.messages().stream().map(TestPlayer.Message::key).toList());
    }

    private void cleanUp() {
        this.players.forEach(TestPlayer::leaveServer);
        this.players.clear();
        NetworkManager network = this.network();
        for (BlockPos pos : this.devices) {
            DeviceId id = NetworkDevices.idOf(this.helper.getLevel(), pos);
            this.helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            // Sans effet si le retrait du bloc l'a déjà fait (cas normal) : filet de sécurité.
            network.removeDevice(id);
        }
        this.devices.clear();
    }

    @Override
    public void testStructureLoaded(GameTestInfo testInfo) {
    }

    @Override
    public void testPassed(GameTestInfo testInfo, GameTestRunner runner) {
        this.cleanUp();
    }

    @Override
    public void testFailed(GameTestInfo testInfo, GameTestRunner runner) {
        this.cleanUp();
    }

    @Override
    public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
    }
}
