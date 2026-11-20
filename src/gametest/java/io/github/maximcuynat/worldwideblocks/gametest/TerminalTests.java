package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.block.LaptopBlock;
import io.github.maximcuynat.worldwideblocks.message.MessageChannel;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.sim.HostLogic;
import io.github.maximcuynat.worldwideblocks.network.sync.TerminalClosePayload;
import io.github.maximcuynat.worldwideblocks.network.sync.TerminalInputPayload;
import io.github.maximcuynat.worldwideblocks.network.sync.TerminalUpdatesPayload;
import io.github.maximcuynat.worldwideblocks.os.ConsoleLine;
import io.github.maximcuynat.worldwideblocks.os.ConsoleSession;
import io.github.maximcuynat.worldwideblocks.os.shell.Prompt;
import io.github.maximcuynat.worldwideblocks.registry.ModBlocks;
import io.github.maximcuynat.worldwideblocks.registry.ModItems;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalRules;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalServer;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalUpdate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Game tests of the terminals on the server (lot 1c-1, issue #11, step 5b): the gestures that open them, one player at
 * the keyboard, the keyboard freed when the player leaves, the packets of the screen checked again by the server, the
 * lid of the laptop kept by its item. The test server has no client: the packets of a screen are handed to
 * {@link TerminalServer#input} as the network would.
 */
final class TerminalTests {
    private static final BlockPos COMPUTER = new BlockPos(3, 0, 3);
    /** A box for the cable of the laptop, beside it. */
    private static final BlockPos BOX = new BlockPos(5, 0, 3);
    /** Height of a click on the keyboard of the open laptop (its top is at 1/16 of a block). */
    private static final double KEYBOARD = 0.05;
    /** Height of a click on the screen of the laptop. */
    private static final double SCREEN = 0.4;

    private TerminalTests() {
    }

    // ---- Helpers ----

    private static ConsoleSession console(NetworkScene scene, DeviceId computer) {
        return ((HostLogic) scene.network().logic(computer).orElseThrow()).system().localConsole();
    }

    private static Optional<UUID> atKeyboard(NetworkScene scene, DeviceId computer) {
        return TerminalServer.playerAt(scene.helper().getLevel().getServer(), computer);
    }

    private static void type(NetworkScene scene, TestPlayer player, DeviceId computer, TerminalInputPayload.Kind kind, String text) {
        BlockPos pos = scene.helper().absolutePos(COMPUTER);
        TerminalServer.input(player, new TerminalInputPayload(pos, console(scene, computer).id(), kind, text));
    }

    private static List<String> typed(NetworkScene scene, DeviceId computer) {
        return console(scene, computer).entries().stream()
                .filter(ConsoleLine.Input.class::isInstance).map(line -> ((ConsoleLine.Input) line).text()).toList();
    }

    // ---- Tests ----

    /**
     * A right-click on the tower, empty hand, opens its terminal; another player gets "computer busy" and cannot type;
     * the lines of the player at the keyboard run on the computer.
     */
    static void towerOpensForOnePlayer(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId tower = scene.place(COMPUTER, ModBlocks.PC_TOWER.get());
        TestPlayer alice = scene.player(GameType.CREATIVE);
        TestPlayer bob = scene.player(GameType.CREATIVE);

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    scene.rightClick(alice, COMPUTER);
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.of(alice.getUUID()), "player at the keyboard");
                    scene.rightClick(bob, COMPUTER);
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.of(alice.getUUID()), "player at the keyboard");
                    scene.assertMessages(bob, MessageChannel.ACTION_BAR, TerminalServer.BUSY, 1);
                    scene.assertMessages(alice, MessageChannel.ACTION_BAR, TerminalServer.BUSY, 0);
                    type(scene, alice, tower, TerminalInputPayload.Kind.LINE, "hostname");
                    type(scene, bob, tower, TerminalInputPayload.Kind.LINE, "help");
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(typed(scene, tower), List.of("hostname"), "lines run on the tower");
                    String name = ((HostLogic) scene.network().logic(tower).orElseThrow()).system().hostName();
                    helper.assertTrue(name.startsWith("tower-"), "host name " + name);
                    helper.assertValueEqual(console(scene, tower).lines(), List.of(name), "output of hostname");
                })
                .thenSucceed();
    }

    /**
     * The keyboard is freed by Esc (the session goes on: the same shell at the next opening), by walking away, by leaving
     * the server, by a dimension change.
     */
    static void keyboardFreed(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId tower = scene.place(COMPUTER, ModBlocks.PC_TOWER.get());
        TestPlayer alice = scene.player(GameType.CREATIVE);
        TestPlayer bob = scene.player(GameType.CREATIVE);
        TestPlayer carol = scene.player(GameType.CREATIVE);
        Vec3 start = alice.position();
        int[] first = new int[1];

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    scene.rightClick(alice, COMPUTER);
                    first[0] = console(scene, tower).id();
                    type(scene, alice, tower, TerminalInputPayload.Kind.LINE, "hostname");
                    type(scene, alice, tower, TerminalInputPayload.Kind.CLOSE, "");
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.empty(), "after Esc");
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(typed(scene, tower), List.of("hostname"), "the line sent before Esc ran");
                    scene.rightClick(alice, COMPUTER);
                    helper.assertValueEqual(console(scene, tower).id(), first[0], "Esc hides the screen, the session goes on");
                    alice.placeAt(start.add(40, 0, 0));
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.empty(), "after walking away");
                    scene.rightClick(bob, COMPUTER);
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.of(bob.getUUID()), "the keyboard is free again");
                    bob.leaveServer();
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.empty(), "after leaving the server");
                    scene.rightClick(carol, COMPUTER);
                    ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
                    carol.teleport(new TeleportTransition(nether, new Vec3(0.5, 80.0, 0.5), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
                })
                .thenExecuteAfter(2, () -> helper.assertValueEqual(atKeyboard(scene, tower), Optional.empty(), "after a dimension change"))
                .thenSucceed();
    }

    /** A computer opened then removed in the same tick: the keyboard is freed at the next tick, nothing breaks. */
    static void computerRemovedWhileOpen(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId tower = scene.place(COMPUTER, ModBlocks.PC_TOWER.get());
        TestPlayer alice = scene.player(GameType.CREATIVE);

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    scene.rightClick(alice, COMPUTER);
                    type(scene, alice, tower, TerminalInputPayload.Kind.LINE, "hostname");
                    helper.setBlock(COMPUTER, Blocks.AIR);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.empty(), "after the computer was removed");
                    helper.assertTrue(scene.network().node(tower).isEmpty(), "the computer left the engine");
                })
                .thenSucceed();
    }

    /** {@code exit} ends the shell: the keyboard is freed, and the next opening is a new shell on a clear screen. */
    static void exitEndsTheSession(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId tower = scene.place(COMPUTER, ModBlocks.PC_TOWER.get());
        TestPlayer alice = scene.player(GameType.CREATIVE);
        int[] first = new int[1];

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    scene.rightClick(alice, COMPUTER);
                    first[0] = console(scene, tower).id();
                    type(scene, alice, tower, TerminalInputPayload.Kind.LINE, "exit");
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.empty(), "after exit");
                    scene.rightClick(alice, COMPUTER);
                    helper.assertValueEqual(console(scene, tower).id(), first[0] + 1, "session after exit");
                    helper.assertValueEqual(console(scene, tower).entries(), List.of(), "screen of the new shell");
                })
                .thenSucceed();
    }

    /** Each packet is checked again: session, size, characters, rate; refused packets change nothing. */
    static void packetsChecked(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId tower = scene.place(COMPUTER, ModBlocks.PC_TOWER.get());
        TestPlayer alice = scene.player(GameType.CREATIVE);

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    scene.rightClick(alice, COMPUTER);
                    BlockPos pos = helper.absolutePos(COMPUTER);
                    int session = console(scene, tower).id();
                    TerminalServer.input(alice, new TerminalInputPayload(pos, session + 1, TerminalInputPayload.Kind.LINE, "stale"));
                    TerminalServer.input(alice, new TerminalInputPayload(pos, session, TerminalInputPayload.Kind.LINE,
                            "x".repeat(TerminalRules.MAX_LINE + 1)));
                    TerminalServer.input(alice, new TerminalInputPayload(pos, session, TerminalInputPayload.Kind.LINE, "ip a\nreboot"));
                    TerminalServer.input(alice, new TerminalInputPayload(pos, session, TerminalInputPayload.Kind.INTERRUPT, "\u001b[2J"));
                })
                // Long enough for the bucket of the player to be full again: one token per tick, BURST at most.
                .thenExecuteAfter(TerminalRules.BURST + 5, () -> {
                    helper.assertValueEqual(typed(scene, tower), List.of(), "refused packets");
                    BlockPos pos = helper.absolutePos(COMPUTER);
                    int session = console(scene, tower).id();
                    for (int i = 0; i < TerminalRules.BURST + 5; i++) {
                        TerminalServer.input(alice, new TerminalInputPayload(pos, session, TerminalInputPayload.Kind.LINE, "n" + i));
                    }
                })
                .thenExecuteAfter(2, () -> helper.assertValueEqual(typed(scene, tower).size(), TerminalRules.BURST,
                        "lines read out of " + (TerminalRules.BURST + 5) + " sent in one tick"))
                .thenSucceed();
    }

    /** A packet sent from beyond the reach is not read, and frees the keyboard at once. */
    static void packetFromAfar(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId tower = scene.place(COMPUTER, ModBlocks.PC_TOWER.get());
        TestPlayer alice = scene.player(GameType.CREATIVE);

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    scene.rightClick(alice, COMPUTER);
                    alice.placeAt(alice.position().add(40, 0, 0));
                    type(scene, alice, tower, TerminalInputPayload.Kind.LINE, "hostname");
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.empty(), "keyboard freed by the packet");
                })
                .thenExecuteAfter(2, () -> helper.assertValueEqual(typed(scene, tower), List.of(), "line sent from afar"))
                .thenSucceed();
    }

    /** Ctrl+C with the text typed, Ctrl+L and Ctrl+D reach the console of the computer through the server. */
    static void keysReachTheConsole(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId tower = scene.place(COMPUTER, ModBlocks.PC_TOWER.get());
        TestPlayer alice = scene.player(GameType.CREATIVE);

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    scene.rightClick(alice, COMPUTER);
                    type(scene, alice, tower, TerminalInputPayload.Kind.LINE, "hostname");
                    type(scene, alice, tower, TerminalInputPayload.Kind.INTERRUPT, "hostn");
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(typed(scene, tower), List.of("hostname", "hostn^C"), "lines and Ctrl+C at the prompt");
                    helper.assertValueEqual(console(scene, tower).lastStatus(), ConsoleSession.KILLED_BY_INTERRUPT, "status after Ctrl+C");
                    type(scene, alice, tower, TerminalInputPayload.Kind.CLEAR, "");
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(console(scene, tower).snapshot().screenStart(), 3, "Ctrl+L: the 3 lines above the screen");
                    type(scene, alice, tower, TerminalInputPayload.Kind.END_OF_INPUT, "");
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(console(scene, tower).ended(), "Ctrl+D on an empty line ends the shell");
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.empty(), "keyboard freed when the shell ends");
                })
                .thenSucceed();
    }

    /** The three packets of the terminal come out of their codec as they went in, every kind of update and of line. */
    static void payloadsRoundTrip(GameTestHelper helper) {
        Prompt prompt = new Prompt("user", "tower-0007", "~");
        BlockPos pos = new BlockPos(1, -60, -2);
        List<TerminalUpdate> updates = List.of(
                new TerminalUpdate.Reset(3, prompt, "^C", true),
                new TerminalUpdate.Line(new ConsoleLine.Output("é ê ✓ 64 bytes")),
                new TerminalUpdate.Line(new ConsoleLine.Input(prompt, "ping -c 1 192.168.1.1")),
                new TerminalUpdate.Line(new ConsoleLine.Game("terminal.worldwideblocks.help.summary.ip", List.of("ip        "))),
                new TerminalUpdate.Line(new ConsoleLine.Advice("terminal.worldwideblocks.advice.ping.no_reply", List.of("192.168.1.9"))),
                new TerminalUpdate.Current("x"),
                new TerminalUpdate.Cleared(true),
                new TerminalUpdate.ScreenStart(4),
                new TerminalUpdate.Running(false),
                new TerminalUpdate.History("hostname"),
                new TerminalUpdate.HistoryCleared(),
                new TerminalUpdate.Ended(2));
        roundTrip(helper, TerminalUpdatesPayload.STREAM_CODEC, new TerminalUpdatesPayload(pos, updates));
        roundTrip(helper, TerminalClosePayload.STREAM_CODEC, new TerminalClosePayload(pos));
        for (TerminalInputPayload.Kind kind : TerminalInputPayload.Kind.values()) {
            roundTrip(helper, TerminalInputPayload.STREAM_CODEC, new TerminalInputPayload(pos, 7, kind, "hostn é"));
        }
        helper.succeed();
    }

    private static <T> void roundTrip(GameTestHelper helper, StreamCodec<ByteBuf, T> codec, T value) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            codec.encode(buffer, value);
            T back = codec.decode(buffer);
            helper.assertValueEqual(back, value, "decoded");
            helper.assertTrue(buffer.readableBytes() == 0, buffer.readableBytes() + " bytes left after " + value);
        } finally {
            buffer.release();
        }
    }

    /**
     * Laptop: the keyboard of the open laptop opens the terminal, the screen closes the lid, which frees the keyboard;
     * on the closed laptop, any click opens the lid.
     */
    static void laptopGestures(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId laptop = scene.place(COMPUTER, ModBlocks.LAPTOP.get());
        TestPlayer alice = scene.player(GameType.CREATIVE);

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(lid(helper), "a new laptop is open");
                    scene.rightClickAt(alice, COMPUTER, KEYBOARD);
                    helper.assertValueEqual(atKeyboard(scene, laptop), Optional.of(alice.getUUID()), "keyboard clicked");
                    helper.assertTrue(lid(helper), "the keyboard does not move the lid");
                    scene.rightClickAt(alice, COMPUTER, SCREEN);
                    helper.assertTrue(!lid(helper), "the screen closes the lid");
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(atKeyboard(scene, laptop), Optional.empty(), "after the lid closed");
                    scene.rightClickAt(alice, COMPUTER, KEYBOARD);
                    helper.assertTrue(lid(helper), "closed: the click opens the lid");
                    helper.assertValueEqual(atKeyboard(scene, laptop), Optional.empty(), "closed: no terminal");
                })
                .thenSucceed();
    }

    /** A closed laptop broken in survival drops a closed laptop, placed closed; an open one drops a plain laptop. */
    static void laptopItemKeepsItsLid(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        BlockPos open = new BlockPos(6, 0, 3);
        scene.place(COMPUTER, ModBlocks.LAPTOP.get());
        scene.place(open, ModBlocks.LAPTOP.get());
        TestPlayer alice = scene.player(GameType.SURVIVAL);

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    scene.rightClickAt(alice, COMPUTER, SCREEN);
                    helper.assertTrue(!lid(helper), "closed before breaking");
                    scene.breakAsPlayer(alice, COMPUTER);
                    scene.breakAsPlayer(alice, open);
                })
                .thenExecuteAfter(2, () -> {
                    ItemStack closedItem = drop(helper, COMPUTER);
                    ItemStack openItem = drop(helper, open);
                    BlockItemStateProperties closedState = closedItem.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
                    helper.assertValueEqual(closedState.get(LaptopBlock.OPEN), Boolean.FALSE, "lid of the item of the closed laptop");
                    helper.assertTrue(!openItem.has(DataComponents.BLOCK_STATE), "the item of an open laptop is a plain laptop");
                    helper.assertTrue(ItemStack.isSameItemSameComponents(openItem, new ItemStack(ModItems.LAPTOP.get())),
                            "it stacks with a new laptop");
                    helper.killAllEntitiesOfClass(ItemEntity.class);
                    alice.setItemInHand(InteractionHand.MAIN_HAND, closedItem);
                    scene.rightClick(alice, COMPUTER.below());
                })
                .thenExecuteAfter(2, () -> {
                    BlockState placed = helper.getBlockState(COMPUTER);
                    helper.assertTrue(placed.getBlock() instanceof LaptopBlock, "laptop placed again: " + placed);
                    helper.assertTrue(!placed.getValue(LaptopBlock.OPEN), "placed closed, as its item");
                    scene.adopt(List.of(helper.absolutePos(COMPUTER)));
                })
                .thenSucceed();
    }

    /**
     * The tower opens as a chest: with a block or any other item in hand, the terminal opens and nothing is placed;
     * sneaking with a block places it against the tower; the cord in hand connects a cable instead.
     */
    static void towerOpensAsAChest(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId tower = scene.place(COMPUTER, ModBlocks.PC_TOWER.get());
        TestPlayer alice = scene.player(GameType.CREATIVE);

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
                    scene.rightClick(alice, COMPUTER);
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.of(alice.getUUID()), "a block in hand opens the terminal");
                    helper.assertBlockPresent(Blocks.AIR, COMPUTER.above());
                    type(scene, alice, tower, TerminalInputPayload.Kind.CLOSE, "");
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.empty(), "after Esc");

                    alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                    scene.rightClick(alice, COMPUTER);
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.of(alice.getUUID()), "a stick in hand opens the terminal");
                    type(scene, alice, tower, TerminalInputPayload.Kind.CLOSE, "");

                    alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
                    scene.sneakRightClick(alice, COMPUTER);
                    helper.assertBlockPresent(Blocks.STONE, COMPUTER.above());
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.empty(), "sneaking with a block: no terminal");

                    alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ETHERNET_CABLE.get()));
                    scene.rightClick(alice, COMPUTER);
                    helper.assertTrue(scene.hasPendingCord(alice), "the cord in hand starts a cable");
                    helper.assertValueEqual(atKeyboard(scene, tower), Optional.empty(), "the cord in hand: no terminal");
                })
                .thenSucceed();
    }

    /**
     * Sneak + right-click with an empty hand on the keyboard of the open laptop takes off the end of its cable (lot 1a),
     * without opening the terminal or moving the lid.
     */
    static void laptopSneakUnplugs(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId laptop = scene.place(COMPUTER, ModBlocks.LAPTOP.get());
        DeviceId box = scene.place(BOX, ModBlocks.INTERNET_BOX.get());
        scene.cable(box, laptop);
        TestPlayer alice = scene.player(GameType.CREATIVE);

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(scene.network().linksOf(laptop).size(), 1, "cables of the laptop before");
                    alice.setShiftKeyDown(true);
                    try {
                        scene.rightClickAt(alice, COMPUTER, KEYBOARD);
                    } finally {
                        alice.setShiftKeyDown(false);
                    }
                    scene.assertMessages(alice, MessageChannel.ACTION_BAR, "message.worldwideblocks.cable.end_taken", 1);
                    helper.assertTrue(scene.hasPendingCord(alice), "the end taken off waits in hand");
                    helper.assertValueEqual(scene.network().linksOf(laptop).size(), 0, "cables of the laptop after");
                    helper.assertValueEqual(atKeyboard(scene, laptop), Optional.empty(), "sneaking: no terminal");
                    helper.assertTrue(lid(helper), "sneaking: the lid does not move");
                })
                .thenSucceed();
    }

    private static boolean lid(GameTestHelper helper) {
        return helper.getBlockState(COMPUTER).getValue(LaptopBlock.OPEN);
    }

    private static ItemStack drop(GameTestHelper helper, BlockPos pos) {
        List<ItemEntity> items = helper.getEntities(EntityTypes.ITEM, pos, 1.0);
        helper.assertTrue(items.size() == 1, "one item dropped at " + pos + ": " + items);
        return items.getFirst().getItem();
    }
}
