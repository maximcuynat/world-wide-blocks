package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.message.MessageChannel;
import io.github.maximcuynat.worldwideblocks.message.MessageHub;
import io.github.maximcuynat.worldwideblocks.message.server.MessageCenter;
import io.github.maximcuynat.worldwideblocks.network.ConnectOutcome;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.LinkMedium;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevices;
import io.github.maximcuynat.worldwideblocks.network.NetworkManager;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.registry.ModDataComponents;
import io.github.maximcuynat.worldwideblocks.registry.ModItems;
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
 * Network scene of a test: devices placed as real blocks, cables, test players.
 *
 * <p>All tests share the same network engine. At the end of the test, passed or not, the scene
 * disconnects its players and removes its devices from the world and from the engine: nothing from
 * this test keeps sending frames during the following ones.
 *
 * <p>It listens to the message module during the whole test ({@link #messages()}).
 */
public final class NetworkScene implements GameTestListener {
    private final GameTestHelper helper;
    private final List<BlockPos> devices = new ArrayList<>();
    private final List<TestPlayer> players = new ArrayList<>();
    private final MessageRecorder messages = new MessageRecorder();
    private final MessageHub.Subscription subscription;

    private NetworkScene(GameTestHelper helper) {
        this.helper = helper;
        this.subscription = MessageCenter.of(helper.getLevel().getServer()).listen(this.messages);
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

    /** Places a device (real block: it registers in the engine as if placed by a player). */
    public DeviceId place(BlockPos relative, Block block) {
        this.helper.setBlock(relative, block);
        BlockPos absolute = this.helper.absolutePos(relative);
        this.devices.add(absolute);
        DeviceId id = this.id(relative);
        this.helper.assertTrue(this.network().node(id).isPresent(), "Device not registered in the engine: " + id);
        return id;
    }

    /** Takes over, for the cleanup at the end of the test, devices placed by other code. */
    public void adopt(List<BlockPos> absolutePositions) {
        this.devices.addAll(absolutePositions);
    }

    public DeviceId id(BlockPos relative) {
        return NetworkDevices.idOf(this.helper.getLevel(), this.helper.absolutePos(relative));
    }

    /** Links the first free port of each device, with no cord used (as in creative). */
    public Link cable(DeviceId a, DeviceId b) {
        PortRef portA = this.firstFreePort(a);
        PortRef portB = this.firstFreePort(b);
        ConnectOutcome outcome = this.network().connect(portA, portB, LinkMedium.COPPER_ETHERNET, false);
        this.helper.assertTrue(outcome.isSuccess(), "Cable refused between " + portA + " and " + portB + ": " + outcome.result());
        return outcome.link().orElseThrow();
    }

    private PortRef firstFreePort(DeviceId device) {
        return this.network().firstFreePort(device, LinkMedium.COPPER_ETHERNET)
                .orElseThrow(() -> this.helper.assertionException(Component.literal("No free port on " + device)));
    }

    /** Connects a new test player, standing in the test area. */
    public TestPlayer player(GameType gameType) {
        return this.player(UUID.randomUUID(), gameType);
    }

    /** Connects a player with this id: if they already played in this world, it is a reconnection. */
    public TestPlayer player(UUID id, GameType gameType) {
        TestPlayer player = TestPlayer.connect(this.helper, id, gameType, new Vec3(1.5, 0.0, 1.5));
        this.players.add(player);
        return player;
    }

    /**
     * Right-click on a device with what is in hand, through the same path as a click received from a
     * real client (NeoForge event, {@code onItemUseFirst}, block, then item).
     */
    public InteractionResult rightClick(TestPlayer player, BlockPos relative) {
        BlockPos absolute = this.helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        return player.gameMode.useItemOn(player, this.helper.getLevel(),
                player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hit);
    }

    /**
     * Right-click on a device at a given height above the bottom of its block (the centre of the block otherwise), as
     * when the player aims at a part of it: the keyboard or the screen of the laptop.
     */
    public InteractionResult rightClickAt(TestPlayer player, BlockPos relative, double height) {
        BlockPos absolute = this.helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(new Vec3(absolute.getX() + 0.5, absolute.getY() + height, absolute.getZ() + 0.5),
                Direction.UP, absolute, false);
        return player.gameMode.useItemOn(player, this.helper.getLevel(),
                player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hit);
    }

    /** Sneaking + right-click on a device. */
    public InteractionResult sneakRightClick(TestPlayer player, BlockPos relative) {
        player.setShiftKeyDown(true);
        try {
            return this.rightClick(player, relative);
        } finally {
            player.setShiftKeyDown(false);
        }
    }

    /** The player breaks the device, through the same path as in the game (drops depend on the game mode). */
    public void breakAsPlayer(TestPlayer player, BlockPos relative) {
        this.helper.assertTrue(player.gameMode.destroyBlock(this.helper.absolutePos(relative)), "Break refused at " + relative);
    }

    /** Explosion centered on this block, which destroys blocks like an explosive mob. */
    public void explodeAt(BlockPos relative, float power) {
        Vec3 center = Vec3.atCenterOf(this.helper.absolutePos(relative));
        this.helper.getLevel().explode(null, center.x, center.y, center.z, power, Level.ExplosionInteraction.BLOCK);
    }

    /** Game time in ticks, the one of the network simulation. */
    public long gameTime() {
        return this.helper.getLevel().getServer().overworld().getGameTime();
    }

    /** True if a cord in the inventory is still waiting to be plugged in. */
    public boolean hasPendingCord(TestPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).has(ModDataComponents.CABLE_START.get())) {
                return true;
            }
        }
        return false;
    }

    /** Cords in the player's inventory. */
    public int cordsHeldBy(TestPlayer player) {
        return player.getInventory().countItem(ModItems.ETHERNET_CABLE.get());
    }

    /** Cords dropped on the ground in the test area. */
    public int cordsOnGround() {
        int count = 0;
        for (ItemEntity item : this.helper.getLevel().getEntitiesOfClass(ItemEntity.class, this.helper.getBoundsWithPadding())) {
            if (item.getItem().is(ModItems.ETHERNET_CABLE.get())) {
                count += item.getItem().getCount();
            }
        }
        return count;
    }

    /** Messages sent by the module since the start of the test. */
    public MessageRecorder messages() {
        return this.messages;
    }

    /** Checks that the player received exactly {@code expected} messages of this key on this channel. */
    public void assertMessages(TestPlayer player, MessageChannel channel, String key, int expected) {
        int received = this.messages.to(player.getUUID(), channel, key).size();
        this.helper.assertTrue(received == expected, "Message " + key + " (" + channel + ") received " + received
                + " times instead of " + expected + ". Messages received: "
                + this.messages.to(player.getUUID()).stream().map(m -> m.channel() + " " + m.key()).toList());
    }

    private void cleanUp() {
        this.subscription.close();
        this.players.forEach(TestPlayer::leaveServer);
        this.players.clear();
        NetworkManager network = this.network();
        for (BlockPos pos : this.devices) {
            DeviceId id = NetworkDevices.idOf(this.helper.getLevel(), pos);
            this.helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            // No effect if removing the block already did it (normal case): safety net.
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
