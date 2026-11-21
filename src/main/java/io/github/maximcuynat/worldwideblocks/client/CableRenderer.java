package io.github.maximcuynat.worldwideblocks.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.block.DevicePorts;
import io.github.maximcuynat.worldwideblocks.item.CableStart;
import io.github.maximcuynat.worldwideblocks.network.sync.ClientLink;
import io.github.maximcuynat.worldwideblocks.port.CordColour;
import io.github.maximcuynat.worldwideblocks.port.PortLayout;
import io.github.maximcuynat.worldwideblocks.port.PortSide;
import io.github.maximcuynat.worldwideblocks.port.PxPoint;
import io.github.maximcuynat.worldwideblocks.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Draws each cable known to the client (lot 1d-1, issue #63, {@code docs/cable-design.md} section 4): an RJ45 plug pushed
 * into the port at each end, at the mouth given by the port layout of the device turned by its {@code FACING}, and between
 * the two plugs a cable of a square section of 0.25 px, lit by the light of the world, hanging along a parabola (the route
 * on the ground comes with lot 1d-2). A cord held in hand with one end plugged in goes from that plug to the hand.
 *
 * <p>The plugs come from the cables the client knows, not from block states. An end whose device the client does not
 * have (its chunk not received) keeps the old attachment point, at 0.3 block in the middle of the block, without a plug.
 *
 * <p>As Minecraft 26.x rendering requires, everything is collected during the extraction of the render state (block
 * states and light read there), then submitted as separate geometry.
 */
final class CableRenderer {
    private static final ContextKey<Frame> CABLES = new ContextKey<>(Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "cables"));
    /** The texture of the plugs: the cable takes the base tone of its colour from it. */
    private static final Identifier PLUG_TEXTURE = Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "textures/block/rj45_plug.png");

    private static final int SEGMENTS = 16;
    /** Attachment height of an end without a known device, as before lot 1d-1. */
    private static final double FALLBACK_HEIGHT = 0.3;
    /** The plug stands out of the mouth by its body (0.5 px) and its boot (0.75 px): the cable starts there. */
    private static final double PLUG_OUT = 1.25 / PxPoint.BLOCK;
    /** Half the side of the square section of the cable: 0.25 px. */
    private static final double HALF = 0.125 / PxPoint.BLOCK;
    private static final int[] NO_TINTS = new int[0];

    private final ClientLinkCache cache;

    /** A plug to draw: its mouth in world coordinates, turned as a blockstate {@code y} (0 north, 1 east...). */
    private record Plug(Vec3 mouth, int quarters, int light, CordColour colour) {
    }

    /** One end of a cable: where the cable starts, and the plug there if the device is known. */
    private record End(Vec3 point, Optional<Plug> plug) {
    }

    /** A cable to draw: its points from one plug to the other, the light at each, its colour. */
    private record Cable(List<Vec3> points, List<Integer> lights, CordColour colour) {
    }

    private record Frame(List<Cable> cables, List<Plug> plugs) {
    }

    CableRenderer(ClientLinkCache cache) {
        this.cache = cache;
    }

    void extract(ExtractLevelRenderStateEvent event) {
        ClientLevel level = event.getLevel();
        List<Cable> cables = new ArrayList<>();
        List<Plug> plugs = new ArrayList<>();
        for (ClientLink link : this.cache.links()) {
            End a = end(level, link.a(), link.portA());
            End b = end(level, link.b(), link.portB());
            AABB box = new AABB(a.point(), b.point()).inflate(0.1).expandTowards(0.0, -sag(a.point(), b.point()), 0.0);
            if (event.getFrustum().isVisible(box)) {
                cables.add(cable(level, a.point(), b.point(), CordColour.DEFAULT));
            }
            for (End end : List.of(a, b)) {
                end.plug().filter(plug -> event.getFrustum().isVisible(new AABB(plug.mouth(), plug.mouth()).inflate(0.1)))
                        .ifPresent(plugs::add);
            }
        }
        this.extractHeldCables(event, level, cables, plugs);
        event.getRenderState().setRenderData(CABLES, new Frame(cables, plugs));
    }

    /**
     * A player holding a cord with one end connected (connection started, or end taken off a device): the cable is
     * drawn from the plug of that end to their hand.
     */
    private void extractHeldCables(ExtractLevelRenderStateEvent event, ClientLevel level, List<Cable> cables, List<Plug> plugs) {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (AbstractClientPlayer player : level.players()) {
            CableStart start = heldCableStart(player);
            if (start == null || !start.device().dimension().equals(level.dimension())) {
                continue;
            }
            Vec3 eye = player.getEyePosition(partialTick);
            boolean firstPerson = player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
            Vec3 hand = firstPerson
                    ? eye.add(player.getViewVector(partialTick).scale(0.6)).add(0.0, -0.35, 0.0)
                    : eye.add(0.0, -0.7, 0.0);
            End end = end(level, start.device().pos(), start.port());
            cables.add(cable(level, end.point(), hand, CordColour.DEFAULT));
            // The plug of a started connection is drawn even when no link holds that port yet.
            end.plug().filter(plug -> this.cache.linkAt(start.device().pos(), start.port()).isEmpty()).ifPresent(plugs::add);
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

    /** The end of a cable on a port: the plug in its mouth and the cable leaving from the end of the boot. */
    private static End end(ClientLevel level, BlockPos pos, int index) {
        if (level.hasChunkAt(pos)) {
            Optional<PortLayout> layout = DevicePorts.layout(level.getBlockState(pos));
            if (layout.isPresent() && index >= 0 && index < layout.get().ports().size()) {
                PortLayout.Port port = layout.get().ports().get(index);
                PortSide face = port.outward();
                if (face.horizontal()) {
                    Vec3 mouth = DevicePorts.toWorld(pos, port.mouth());
                    Vec3 out = new Vec3(face.x(), face.y(), face.z());
                    int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(mouth.add(out.scale(0.5 / PxPoint.BLOCK))));
                    return new End(mouth.add(out.scale(PLUG_OUT)),
                            Optional.of(new Plug(mouth, face.quartersFromNorth(), light, CordColour.DEFAULT)));
                }
            }
        }
        return new End(new Vec3(pos.getX() + 0.5, pos.getY() + FALLBACK_HEIGHT, pos.getZ() + 0.5), Optional.empty());
    }

    private static Cable cable(ClientLevel level, Vec3 a, Vec3 b, CordColour colour) {
        double sag = sag(a, b);
        List<Vec3> points = new ArrayList<>(SEGMENTS + 1);
        List<Integer> lights = new ArrayList<>(SEGMENTS + 1);
        for (int i = 0; i <= SEGMENTS; i++) {
            Vec3 point = pointAt(a, b, sag, (double) i / SEGMENTS);
            points.add(point);
            lights.add(LightCoordsUtil.getLightCoords(level, BlockPos.containing(point)));
        }
        return new Cable(points, lights, colour);
    }

    void submit(SubmitCustomGeometryEvent event) {
        Frame frame = event.getLevelRenderState().getRenderData(CABLES);
        if (frame == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        var models = Minecraft.getInstance().getModelManager();
        for (Plug plug : frame.plugs()) {
            BlockStateModelPart model = PlugModels.get(models, plug.colour());
            if (model == null) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(plug.mouth().x - camera.x, plug.mouth().y - camera.y, plug.mouth().z - camera.z);
            // Clockwise seen from above, as the y of a blockstate: north goes east.
            poseStack.rotateDegrees(Axis.YP, -90.0F * plug.quarters());
            // The model is a plug in a port facing north, its mouth at the centre of the block (8, 8, 8).
            poseStack.translate(-0.5, -0.5, -0.5);
            event.getSubmitNodeCollector().submitBlockModel(poseStack, Sheets.cutoutBlockItemSheet(), List.of(model), NO_TINTS,
                    plug.light(), OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (frame.cables().isEmpty()) {
            return;
        }
        double pixel = pixelPerBlockOfDistance(event.getLevelRenderState().cameraRenderState.projectionMatrix.m11());
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.entitySolid(PLUG_TEXTURE), (pose, buffer) -> {
            for (Cable cable : frame.cables()) {
                drawCable(pose, buffer, cable, camera, pixel);
            }
        });
    }

    /**
     * The size of one pixel of the screen, in blocks, for each block of distance from the camera: the projection gives
     * {@code 1 / tan(fov / 2)} as its {@code m11}, and the screen is {@code height} pixels for {@code 2 tan(fov / 2)}
     * blocks at a distance of one block.
     */
    private static double pixelPerBlockOfDistance(float m11) {
        int height = Math.max(1, Minecraft.getInstance().getWindow().getHeight());
        return m11 > 0 ? 2.0 / (m11 * height) : 0.0;
    }

    /**
     * Four faces along each segment, a square section of 0.25 px around the line of the cable, widened far from the
     * camera so that the cable is never less than one pixel of the screen wide (Max, 2026-11-21: a 0.25 px cable is
     * thinner than a pixel beyond about 12 blocks at 1920 x 1080).
     *
     * @param pixel size of a pixel of the screen, in blocks, per block of distance from the camera
     */
    private static void drawCable(PoseStack.Pose pose, VertexConsumer buffer, Cable cable, Vec3 camera, double pixel) {
        List<Vec3> points = cable.points();
        int n = points.size();
        Vec3[][] corners = new Vec3[n][4];
        Vec3[][] normals = new Vec3[n][4];
        for (int i = 0; i < n; i++) {
            Vec3 tangent = points.get(Math.min(i + 1, n - 1)).subtract(points.get(Math.max(i - 1, 0)));
            if (tangent.lengthSqr() < 1.0e-12) {
                tangent = new Vec3(1, 0, 0);
            }
            tangent = tangent.normalize();
            Vec3 side = tangent.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1.0e-6) {
                side = tangent.cross(new Vec3(1, 0, 0));
            }
            side = side.normalize();
            Vec3 up = side.cross(tangent).normalize();
            Vec3 p = points.get(i).subtract(camera);
            Vec3[] offsets = {up.add(side), up.subtract(side), up.scale(-1).subtract(side), up.scale(-1).add(side)};
            for (int k = 0; k < 4; k++) {
                corners[i][k] = p.add(offsets[k].scale(halfWidth(p.length(), pixel)));
            }
            normals[i] = new Vec3[] {up, side.scale(-1), up.scale(-1), side};
        }
        float u = (cable.colour().baseTexelX() + 0.5F) / CordColour.PLUG_TEXTURE_SIZE;
        float v = (cable.colour().baseTexelY() + 0.5F) / CordColour.PLUG_TEXTURE_SIZE;
        for (int i = 0; i < n - 1; i++) {
            for (int k = 0; k < 4; k++) {
                int next = (k + 1) % 4;
                Vec3 normal = normals[i][k];
                Vec3[] quad = {corners[i][k], corners[i + 1][k], corners[i + 1][next], corners[i][next]};
                int[] light = {cable.lights().get(i), cable.lights().get(i + 1), cable.lights().get(i + 1), cable.lights().get(i)};
                // Counterclockwise seen from outside, whatever the direction of the cable.
                boolean outward = quad[1].subtract(quad[0]).cross(quad[2].subtract(quad[0])).dot(normal) > 0;
                for (int c = 0; c < 4; c++) {
                    int j = outward ? c : 3 - c;
                    Vec3 q = quad[j];
                    buffer.addVertex(pose, (float) q.x, (float) q.y, (float) q.z)
                            .setColor(0xFFFFFFFF)
                            .setUv(u, v)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light[j])
                            .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
                }
            }
        }
    }

    /** Half the side of the section at this distance from the camera: 0.25 px, or one pixel of the screen if that is wider. */
    static double halfWidth(double distance, double pixel) {
        return Math.max(HALF, distance * pixel / 2.0);
    }

    private static Vec3 pointAt(Vec3 a, Vec3 b, double sag, double t) {
        return a.lerp(b, t).add(0.0, -sag * 4.0 * t * (1.0 - t), 0.0);
    }

    /** Sag in the middle of the cable, proportional to its length and capped. */
    private static double sag(Vec3 a, Vec3 b) {
        return Math.min(0.04 * a.distanceTo(b), 1.5);
    }
}
