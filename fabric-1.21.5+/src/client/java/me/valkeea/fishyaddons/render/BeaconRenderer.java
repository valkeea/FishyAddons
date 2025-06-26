package me.valkeea.fishyaddons.render;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class BeaconRenderer {
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

        // Draw block outline at (x, y, z)
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getLines());
        drawBoxOutline(matrices, new Box(beacon.pos), consumer, r, g, b, a);

        matrices.push();
        matrices.translate(x, y, z);
        BeaconBlockEntityRenderer.renderBeam(
            matrices,
            context.consumers(),
            BeaconBlockEntityRenderer.BEAM_TEXTURE,
            1.0f, // tickDelta
            1.0F,
            client.world.getTime(),
            0,
            256,
            beacon.color,
            0.2F,
            0.25F
        );
        matrices.pop();

        // Draw label
            TextRenderer textRenderer = client.textRenderer;
            float scale = 0.025f;

            matrices.push();
            matrices.translate(x + 0.5, y + 1.5, z + 0.5);
            matrices.multiply(context.camera().getRotation());
            matrices.scale(-scale, -scale, scale);

            float width = textRenderer.getWidth(beacon.label);
            textRenderer.draw(
                beacon.label,
                -width / 2f,
                0,
                0xFFFFFF, // white text
                true,
                matrices.peek().getPositionMatrix(),
                context.consumers(),
                TextRenderer.TextLayerType.NORMAL,
                0,
                0xF000F0
            );
            matrices.pop();

        matrices.pop();
    }

    private static void drawBoxOutline(MatrixStack matrices, Box box, VertexConsumer consumer, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        // Bottom square
        line(consumer, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(consumer, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(consumer, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(consumer, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);

        // Top square
        line(consumer, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(consumer, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(consumer, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(consumer, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);

        // Vertical lines
        line(consumer, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(consumer, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(consumer, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(consumer, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(VertexConsumer consumer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
    }
}