package io.github.maximcuynat.worldwideblocks.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.item.CableStart;
import io.github.maximcuynat.worldwideblocks.network.sync.ClientLink;
import io.github.maximcuynat.worldwideblocks.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Dessine chaque câble comme une ligne tendue entre ses deux appareils, avec un léger affaissement
 * au milieu, ainsi que le câble tenu en main par un joueur dont l'autre bout est branché. Rendu provisoire du lot 1a : l'épaisseur et la texture du câble viendront au lot de finition.
 *
 * <p>Comme l'exige le rendu de Minecraft 26.x, les câbles à dessiner sont relevés pendant
 * l'extraction de l'état de rendu, puis soumis comme géométrie à part.
 */
final class CableRenderer {
    private static final ContextKey<List<Cable>> CABLES = new ContextKey<>(Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "cables"));

    /** Bleu des cordons Ethernet courants, opaque. */
    private static final int COLOR = 0xFF2F6FD6;
    private static final int SEGMENTS = 16;
    /** Hauteur de raccordement dans le bloc : un peu sous le centre, pour aller à la tour comme au portable. */
    private static final double ATTACH_HEIGHT = 0.3;

    private final ClientLinkCache cache;

    /** Un câble à dessiner, en coordonnées du monde. */
    private record Cable(Vec3 a, Vec3 b) {
    }

    CableRenderer(ClientLinkCache cache) {
        this.cache = cache;
    }

    void extract(ExtractLevelRenderStateEvent event) {
        List<Cable> visible = new ArrayList<>();
        for (ClientLink link : this.cache.links()) {
            Vec3 a = attachPoint(link.a());
            Vec3 b = attachPoint(link.b());
            AABB box = new AABB(a, b).inflate(0.1).expandTowards(0.0, -sag(a, b), 0.0);
            if (event.getFrustum().isVisible(box)) {
                visible.add(new Cable(a, b));
            }
        }
        this.extractHeldCables(event, visible);
        event.getRenderState().setRenderData(CABLES, visible);
    }

    /**
     * Un joueur qui tient un cordon dont un bout est branché (branchement commencé, ou bout retiré
     * d'un appareil) : on dessine le câble de l'appareil jusqu'à sa main.
     */
    private void extractHeldCables(ExtractLevelRenderStateEvent event, List<Cable> visible) {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (AbstractClientPlayer player : event.getLevel().players()) {
            CableStart start = heldCableStart(player);
            if (start == null || !start.device().dimension().equals(event.getLevel().dimension())) {
                continue;
            }
            Vec3 eye = player.getEyePosition(partialTick);
            boolean firstPerson = player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
            Vec3 hand = firstPerson
                    ? eye.add(player.getViewVector(partialTick).scale(0.6)).add(0.0, -0.35, 0.0)
                    : eye.add(0.0, -0.7, 0.0);
            visible.add(new Cable(attachPoint(start.device().pos()), hand));
        }
    }

    private static CableStart heldCableStart(AbstractClientPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            CableStart start = player.getItemInHand(hand).get(ModDataComponents.CABLE_START.get());
            if (start != null) {
                return start;
            }
        }
        return null;
    }

    void submit(SubmitCustomGeometryEvent event) {
        List<Cable> cables = event.getLevelRenderState().getRenderData(CABLES);
        if (cables == null || cables.isEmpty()) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth * 2.0F;
        PoseStack poseStack = event.getPoseStack();
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            for (Cable cable : cables) {
                drawCable(pose, buffer, cable.a().subtract(camera), cable.b().subtract(camera), sag(cable.a(), cable.b()), width);
            }
        });
    }

    /** Une ligne brisée de {@link #SEGMENTS} segments, qui suit une parabole entre les deux extrémités. */
    private static void drawCable(PoseStack.Pose pose, VertexConsumer buffer, Vec3 a, Vec3 b, double sag, float width) {
        Vec3 previous = pointAt(a, b, sag, 0.0);
        for (int i = 1; i <= SEGMENTS; i++) {
            Vec3 next = pointAt(a, b, sag, (double) i / SEGMENTS);
            Vec3 direction = next.subtract(previous).normalize();
            vertex(pose, buffer, previous, direction, width);
            vertex(pose, buffer, next, direction, width);
            previous = next;
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 point, Vec3 normal, float width) {
        buffer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(COLOR)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                .setLineWidth(width);
    }

    private static Vec3 pointAt(Vec3 a, Vec3 b, double sag, double t) {
        return a.lerp(b, t).add(0.0, -sag * 4.0 * t * (1.0 - t), 0.0);
    }

    /** Affaissement au milieu du câble, proportionnel à sa longueur et plafonné. */
    private static double sag(Vec3 a, Vec3 b) {
        return Math.min(0.04 * a.distanceTo(b), 1.5);
    }

    private static Vec3 attachPoint(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + ATTACH_HEIGHT, pos.getZ() + 0.5);
    }
}
