package me.valkeea.fishyaddons.render;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class BeaconRenderer {
    private BeaconRenderer() {}
    private static class BeaconData {
        final BlockPos pos;
        final int color;
        final String label;
        final long setTime;

        BeaconData(BlockPos pos, int color, @Nullable String label) {
            this.pos = pos;
            this.color = color;
            this.label = label;
            this.setTime = System.currentTimeMillis();
        }
    }

    private static final List<BeaconData> beacons = new ArrayList<>();

    public static void setBeacon(BlockPos pos, int colorARGB, @Nullable String displayLabel) {
        beacons.add(new BeaconData(pos, colorARGB, displayLabel));
    }

    // subtract 1 if negative, do nothing if positive
    public static BlockPos getActualPos(Vec3d pos) {
        return new BlockPos(
            (int) (pos.x < 0 ? Math.floor(pos.x) - 1 : Math.floor(pos.x)),
            (int) (Math.floor(pos.y) - 1),
            (int) (pos.z < 0 ? Math.floor(pos.z) - 1 : Math.floor(pos.z))
        );
    }

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (beacons.isEmpty()) return;

            long now = System.currentTimeMillis();
            beacons.removeIf(beacon -> now - beacon.setTime > 60_000);

            for (BeaconData beacon : beacons) {
                renderBeacon(context, beacon);
            }
        });
    }

    private static void renderBeacon(WorldRenderContext context, BeaconData beacon) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Vec3d camPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();

        double x = beacon.pos.getX();
        double y = beacon.pos.getY();
        double z = beacon.pos.getZ();

        // Convert ARGB to float RGBA
        float r = ((beacon.color >> 16) & 0xFF) / 255f;
        float g = ((beacon.color >> 8) & 0xFF) / 255f;
        float b = (beacon.color & 0xFF) / 255f;
        float a = ((beacon.color >> 24) & 0xFF) / 255f;

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);      

        // Draw filled box 
        VertexConsumer fillConsumer = context.consumers().getBuffer(RenderLayer.getDebugQuads());
        WorldElements.boxFill(matrices, new Box(beacon.pos), fillConsumer, r, g, b, 0.3f);
        
        // Draw block outline
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getLines());
        WorldElements.boxOutline(matrices, new Box(beacon.pos), consumer, r, g, b, a);

        // Draw label text with transparent black bg
        if (beacon.label != null) {
            WorldElements.text(context, matrices, beacon.label, x + 0.5, y + 0.5, z + 0.5, beacon.color);
        }          

        // Render beacon beam
        matrices.push();
        matrices.translate(x, y + 1, z);
        BeaconBlockEntityRenderer.renderBeam(
            matrices,
            context.consumers(),
            BeaconBlockEntityRenderer.BEAM_TEXTURE,
            1.0f,
            1.0F,
            client.world.getTime(),
            0,
            256,
            beacon.color,
            0.2F,
            0.25F
        );

        matrices.pop();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        matrices.pop();
    }

    public static void clearBeacons() {
        if (!beacons.isEmpty()) {
            beacons.clear();
        }
    }
}