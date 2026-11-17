package dev.netcraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import org.jspecify.annotations.Nullable;

/**
 * Joueur de test connecté au serveur par une connexion en mémoire, sur le modèle du
 * {@code GameTestPlayer} des tests de NeoForge : il est dans la liste des joueurs et dans son
 * niveau, le serveur le fait vivre à chaque tick (inventaire compris), il peut changer de dimension
 * et se déconnecter pour de vrai.
 *
 * <p>Il retient chaque message reçu (barre d'objets et chat) pour que les tests le vérifient.
 */
public final class TestPlayer extends ServerPlayer {
    /** Un message reçu : son texte et s'il s'affichait au-dessus de la barre d'objets. */
    public record Message(Component text, boolean overlay) {
        /** Clé de traduction du message, ou chaîne vide s'il n'est pas traduisible. */
        public String key() {
            return this.text.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
        }
    }

    private final List<Message> messages = new ArrayList<>();

    private TestPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation information) {
        super(server, level, profile, information);
    }

    /**
     * Connecte un joueur de test, comme un client qui rejoint le serveur, puis le place à
     * {@code relativePos} dans la zone du test. Un {@code id} déjà connu du monde reprend les données
     * sauvegardées de ce joueur (inventaire compris) : c'est une reconnexion.
     */
    public static TestPlayer connect(GameTestHelper helper, UUID id, GameType gameType, Vec3 relativePos) {
        MinecraftServer server = helper.getLevel().getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(id, "netcraft-test"), false);
        TestPlayer player = new TestPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        // Nom complet : dans une sous-classe de ServerPlayer, « Connection » désigne le type hérité
        // WaypointTransmitter.Connection, qui masque l'import.
        net.minecraft.network.Connection memory = new net.minecraft.network.Connection(PacketFlow.SERVERBOUND) {
            @Override
            public boolean isMemoryConnection() {
                return true;
            }

            @Override
            public void send(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
                super.send(packet, listener, flush);
                // Le client répond aussitôt aux paquets de maintien, sinon le serveur le déconnecterait.
                if (packet instanceof ClientboundKeepAlivePacket keepAlive) {
                    player.connection.handleKeepAlive(new ServerboundKeepAlivePacket(keepAlive.getId()));
                }
            }
        };
        // Le constructeur associe le canal à la connexion.
        new EmbeddedChannel(memory);
        NetworkRegistry.configureMockConnection(memory);
        server.getPlayerList().placeNewPlayer(memory, player, cookie);
        // Inscrite parmi les connexions du serveur, elle est tickée : le joueur vit comme un vrai.
        server.getConnection().getConnections().add(memory);
        player.gameMode.changeGameModeForPlayer(gameType);
        player.placeAt(helper.absoluteVec(relativePos));
        player.connection.markClientLoaded();
        return player;
    }

    /** Déplace le joueur dans son niveau actuel et lui envoie les chunks autour. */
    public void placeAt(Vec3 absolutePos) {
        this.snapTo(absolutePos.x, absolutePos.y, absolutePos.z);
        this.level().getChunkSource().move(this);
        this.connection.chunkSender.sendNextChunks(this);
        this.connection.chunkSender.onChunkBatchReceivedByClient(64.0F);
    }

    /**
     * Déconnexion, comme un client qui quitte : le serveur sauvegarde le joueur et le retire au
     * tick suivant. (Pas {@code disconnect()}, qui est une étape interne de {@code ServerPlayer}.)
     */
    public void leaveServer() {
        if (this.connection.getConnection().isConnected()) {
            this.connection.disconnect(Component.literal("Test NetCraft terminé"));
        }
    }

    @Override
    public void sendSystemMessage(Component message, boolean overlay) {
        this.messages.add(new Message(message, overlay));
        super.sendSystemMessage(message, overlay);
    }

    public List<Message> messages() {
        return List.copyOf(this.messages);
    }

    /** Messages reçus dont la clé de traduction est {@code key}. */
    public List<Message> messages(String key) {
        return this.messages.stream().filter(message -> message.key().equals(key)).toList();
    }

    public void clearMessages() {
        this.messages.clear();
    }
}
