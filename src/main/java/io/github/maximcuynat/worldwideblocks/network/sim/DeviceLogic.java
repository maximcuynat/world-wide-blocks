package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.switching.MacTable;

/**
 * Behavior of a device in the simulation: what it does when a cable is connected or removed,
 * when a frame arrives and when a timer expires. One instance per device, recreated if
 * the device is replaced; its state is not saved, like the memory of a real device
 * that is restarted.
 */
public interface DeviceLogic {

    /** The device enters the simulation (placed, or server started). */
    default void onStart(DeviceContext context) {
    }

    void onLinkUp(DeviceContext context, int port);

    void onLinkDown(DeviceContext context, int port);

    void onFrame(DeviceContext context, int port, EthernetFrame frame);

    default void onTimer(DeviceContext context, Object key) {
    }

    /** A line was typed in the local console of the device and Enter pressed; ignored by a device without one. */
    default void onConsoleLine(DeviceContext context, String line) {
    }

    /**
     * True for a device that relays frames between its ports (switch, box): the simulation
     * checks that the same frame does not come back to it twice.
     */
    default boolean bridges() {
        return false;
    }

    /**
     * Time the device takes to handle a frame it receives, in nanoseconds, added to the latency the frame carries
     * ({@code Latency}).
     */
    default long processingNanos() {
        return 0L;
    }

    /**
     * Ctrl+C typed in the local console of the device; ignored by a device without one.
     *
     * @param typed the text typed at the prompt and not sent yet, shown before {@code ^C} when no command runs
     */
    default void onConsoleInterrupt(DeviceContext context, String typed) {
    }

    /** Ctrl+D on an empty line in the local console of the device; ignored by a device without one. */
    default void onConsoleEndOfInput(DeviceContext context) {
    }

    /** Ctrl+L in the local console of the device; ignored by a device without one. */
    default void onConsoleClear(DeviceContext context) {
    }

    /** MACs learned behind each port, for a device that switches (status panel); empty otherwise. */
    default List<MacTable.Entry> learnedMacs() {
        return List.of();
    }
}
