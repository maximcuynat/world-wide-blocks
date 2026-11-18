package io.github.maximcuynat.worldwideblocks.message;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * State of the message module for the running server: repeat limit and listeners. Held by the
 * server data (no static state), it disappears with it.
 */
public final class MessageHub {

    /** Subscription of a listener; closing it removes it. */
    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    private final MessageThrottle throttle;
    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();

    public MessageHub(long reportIntervalTicks) {
        this.throttle = new MessageThrottle(reportIntervalTicks);
    }

    public MessageThrottle throttle() {
        return this.throttle;
    }

    public Subscription listen(MessageListener listener) {
        this.listeners.add(listener);
        return () -> this.listeners.remove(listener);
    }

    /** Notifies the listeners of a sent message. */
    public void publish(SentMessage message) {
        for (MessageListener listener : this.listeners) {
            listener.onSent(message);
        }
    }
}
