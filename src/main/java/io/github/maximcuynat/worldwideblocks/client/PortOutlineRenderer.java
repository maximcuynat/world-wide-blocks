package io.github.maximcuynat.worldwideblocks.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.block.DevicePorts;
import io.github.maximcuynat.worldwideblocks.port.PortLayout;
import io.github.maximcuynat.worldwideblocks.port.PortSide;
import io.github.maximcuynat.worldwideblocks.port.PxPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Outline of the aimed port (issue #62, {@code docs/cable-design.md} section 3): a two-tone frame around its hole, a
 * light line against the hole and a dark one outside it, so that it shows on light and dark devices; drawn without
 * shading, like the block outline, just in front of the face, with a cord in hand or sneaking with an empty hand. The
 * sizes and colours are those of the preview chosen on 2026-11-20 ({@code frame} of {@code design/tools/cables.py}).
 */
final class PortOutlineRenderer {
    private static final ContextKey<List<Quad>> QUADS = new ContextKey<>(Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "port_outline"));

    /** Width of each line of the frame, in model pixels. */
    static final double LINE = 1.0 / 16.0;
    /** How far in front of the face the frame is drawn, in model pixels. */
    static final double GAP = 1.0 / 64.0;
    /** Light plastic, highlight tone (design/palette.json), against the hole. */
    static final int LIGHT = 0xFFDDE0E4;
    /** Socket (design/palette.json), outside the light line. */
    static final int DARK = 0xFF1C1E22;

    /** A flat quad of one colour, in world coordinates. */
    private record Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int colour) {
    }

    void extract(ExtractLevelRenderStateEvent event) {
        List<Quad> quads = new ArrayList<>();
        PortAim.current(Minecraft.getInstance()).ifPresent(aimed -> frame(aimed, quads));
        event.getRenderState().setRenderData(QUADS, quads);
    }

    void submit(SubmitCustomGeometryEvent event) {
        List<Quad> quads = event.getLevelRenderState().getRenderData(QUADS);
        if (quads == null || quads.isEmpty()) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
            for (Quad quad : quads) {
                for (Vec3 corner : List.of(quad.a(), quad.b(), quad.c(), quad.d())) {
                    Vec3 p = corner.subtract(camera);
                    buffer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(quad.colour());
                }
            }
        });
    }

    /** The two rings of four bars around the hole, in the plane just in front of the face of the port. */
    private static void frame(PortAim.Aimed aimed, List<Quad> quads) {
        PortLayout.Port port = aimed.port();
        PortSide face = port.face();
        PxPoint mouth = port.mouth().moved(face, GAP);
        // Half sizes of the hole across the face (horizontal) and up it; a port on the top or the bottom has none yet.
        double halfAcross = (face.x() != 0 ? port.hole().maxZ() - port.hole().minZ() : port.hole().maxX() - port.hole().minX()) / 2;
        double halfUp = (port.hole().maxY() - port.hole().minY()) / 2;
        if (!face.horizontal()) {
            return;
        }
        int[] colours = {LIGHT, DARK};
        for (int ring = 0; ring < 2; ring++) {
            double inA = halfAcross + ring * LINE;
            double inU = halfUp + ring * LINE;
            double outA = inA + LINE;
            double outU = inU + LINE;
            int colour = colours[ring];
            bar(aimed, face, mouth, -outA, outA, inU, outU, colour, quads);    // top
            bar(aimed, face, mouth, -outA, outA, -outU, -inU, colour, quads);  // bottom
            bar(aimed, face, mouth, -outA, -inA, -inU, inU, colour, quads);    // one side
            bar(aimed, face, mouth, inA, outA, -inU, inU, colour, quads);      // the other side
        }
    }

    /** A rectangle of the plane of the face, from {@code a0} to {@code a1} across it and {@code u0} to {@code u1} up it. */
    private static void bar(PortAim.Aimed aimed, PortSide face, PxPoint centre, double a0, double a1, double u0, double u1,
            int colour, List<Quad> quads) {
        quads.add(new Quad(world(aimed, face, centre, a0, u0), world(aimed, face, centre, a1, u0),
                world(aimed, face, centre, a1, u1), world(aimed, face, centre, a0, u1), colour));
    }

    private static Vec3 world(PortAim.Aimed aimed, PortSide face, PxPoint centre, double across, double up) {
        PxPoint point = face.x() != 0
                ? new PxPoint(centre.x(), centre.y() + up, centre.z() + across)
                : new PxPoint(centre.x() + across, centre.y() + up, centre.z());
        return DevicePorts.toWorld(aimed.pos(), point);
    }
}
