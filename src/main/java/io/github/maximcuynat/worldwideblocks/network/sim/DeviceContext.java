package io.github.maximcuynat.worldwideblocks.network.sim;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

/**
 * What a device can do while it reacts to a simulation event. A context
 * is only valid during the call that receives it.
 */
public interface DeviceContext {

    /** Game time, in ticks. */
    long now();

    DeviceId device();

    /** MAC address of the port. */
    MacAddress mac(int port);

    /** MAC address of the device's own service (the DHCP server of a box); only for a device that has one. */
    MacAddress serviceMac();

    /** True if a cable is connected to this port. */
    boolean isLinked(int port);

    /** Sends a new frame through this port. Without a cable, it is lost. */
    void send(int port, EthernetFrame frame);

    /**
     * Relays the frame being processed through this port, like a switch: the simulation tracks it
     * to detect loops. Only during {@link DeviceLogic#onFrame}.
     */
    void relay(int port, EthernetFrame frame);

    /**
     * Time spent since the start of the tick when the device handles this call, in nanoseconds ({@code Latency}):
     * for a received frame, the latency it carries plus the processing time of the device and what {@link #elapse}
     * added; 0 in a timer or a console call. A frame sent during the call leaves at that time.
     */
    default long latency() {
        return 0L;
    }

    /** Time passes inside the device during this call (a computer behind the switch of a box). */
    default void elapse(long nanos) {
    }

    /** Requests a call to {@link DeviceLogic#onTimer} with this key, in {@code delayTicks} ticks. */
    void schedule(long delayTicks, Object key);

    /** Reports something the game turns into a message (see {@link NetworkSimulation#drainEvents()}). */
    void report(SimulationEvent event);
}
