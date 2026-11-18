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

    /**
     * True for a device that relays frames between its ports (switch, box): the simulation
     * checks that the same frame does not come back to it twice.
     */
    default boolean bridges() {
        return false;
    }

    /** MACs learned behind each port, for a device that switches (status panel); empty otherwise. */
    default List<MacTable.Entry> learnedMacs() {
        return List.of();
    }
}
