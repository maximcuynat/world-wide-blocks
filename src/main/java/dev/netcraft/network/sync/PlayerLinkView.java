package dev.netcraft.network.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import dev.netcraft.network.ChunkKey;
import dev.netcraft.network.Link;

/**
 * Ce qu'un joueur a le droit de connaître du réseau : seuls les câbles dont une extrémité se trouve
 * dans un chunk que son client a reçu. Un joueur ne peut donc pas localiser des appareils hors de
 * sa vue en lisant les paquets.
 *
 * <p>La vue tient sa propre liste des chunks reçus (événement {@code Sent}) et retirés
 * ({@code UnWatch}) au lieu d'interroger la vue de suivi de Minecraft : pendant {@code UnWatch},
 * celle-ci n'est pas encore mise à jour et donnerait de faux résultats lors d'un téléport.
 *
 * <p>Java pur : testable sans lancer le jeu. Une instance par joueur, portée par le joueur côté serveur.
 */
public final class PlayerLinkView {
    private String dimension;
    private final Set<ChunkKey> watched = new HashSet<>();
    private final Set<UUID> known = new HashSet<>();

    /**
     * Le joueur reçoit des chunks de cette dimension. S'il vient d'en changer (ou s'il vient d'arriver),
     * la vue repart de zéro.
     *
     * @return vrai si le client doit oublier tous ses câbles
     */
    public boolean enterDimension(String newDimension) {
        if (Objects.equals(this.dimension, newDimension)) {
            return false;
        }
        this.dimension = newDimension;
        this.watched.clear();
        this.known.clear();
        return true;
    }

    /**
     * Le client vient de recevoir ce chunk.
     *
     * @param touching câbles dont une extrémité est dans ce chunk
     * @return les câbles à lui envoyer, ceux qu'il ne connaît pas encore
     */
    public List<Link> chunkSent(ChunkKey chunk, Collection<Link> touching) {
        if (!chunk.dimension().equals(this.dimension)) {
            return List.of();
        }
        this.watched.add(chunk);
        List<Link> toSend = new ArrayList<>();
        for (Link link : touching) {
            if (this.known.add(link.id())) {
                toSend.add(link);
            }
        }
        return toSend;
    }

    /**
     * Le joueur ne suit plus ce chunk. Un câble n'est oublié que si aucune de ses deux extrémités
     * n'est encore dans un chunk suivi.
     *
     * @param touching câbles dont une extrémité est dans ce chunk
     * @return les identifiants des câbles que le client doit oublier
     */
    public List<UUID> chunkUnwatched(ChunkKey chunk, Collection<Link> touching) {
        if (!this.watched.remove(chunk)) {
            return List.of();
        }
        List<UUID> toForget = new ArrayList<>();
        for (Link link : touching) {
            if (!this.isVisible(link) && this.known.remove(link.id())) {
                toForget.add(link.id());
            }
        }
        return toForget;
    }

    /** Un câble vient d'être branché : vrai s'il faut l'envoyer à ce joueur. */
    public boolean linkAdded(Link link) {
        return this.isVisible(link) && this.known.add(link.id());
    }

    /** Un câble vient d'être retiré : vrai si ce joueur le connaissait et doit l'oublier. */
    public boolean linkRemoved(UUID id) {
        return this.known.remove(id);
    }

    public boolean knows(UUID id) {
        return this.known.contains(id);
    }

    private boolean isVisible(Link link) {
        return this.watched.contains(ChunkKey.of(link.a().device())) || this.watched.contains(ChunkKey.of(link.b().device()));
    }
}
