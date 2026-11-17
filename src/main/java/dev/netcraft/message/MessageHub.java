package dev.netcraft.message;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * État du module des messages pour le serveur en cours : anti-répétition et écouteurs. Porté par les
 * données du serveur (aucun état statique), il disparaît avec lui.
 */
public final class MessageHub {

    /** Abonnement d'un écouteur ; le fermer le retire. */
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

    /** Prévient les écouteurs d'un message envoyé. */
    public void publish(SentMessage message) {
        for (MessageListener listener : this.listeners) {
            listener.onSent(message);
        }
    }
}
