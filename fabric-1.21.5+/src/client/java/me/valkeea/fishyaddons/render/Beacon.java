package me.valkeea.fishyaddons.render;

import me.valkeea.fishyaddons.handler.ActiveBeacons.BeaconData;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Beacon {
    private Beacon() {}

    public static void renderBeacon(WorldRenderContext context, BeaconData beacon) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Vec3d camPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();

        double x = beacon.getPos().getX();
        double y = beacon.getPos().getY();
        double z = beacon.getPos().getZ();

        float r = ((beacon.getColor() >> 16) & 0xFF) / 255f;
        float g = ((beacon.getColor() >> 8) & 0xFF) / 255f;
        float b = (beacon.getColor() & 0xFF) / 255f;
        float a = ((beacon.getColor() >> 24) & 0xFF) / 255f;

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);      

        VertexConsumer fillConsumer = context.consumers().getBuffer(RenderLayer.getDebugQuads());
        WorldElements.boxFill(matrices, new Box(beacon.getPos()), fillConsumer, r, g, b, 0.3f);

        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getLines());
        WorldElements.boxOutline(matrices, new Box(beacon.getPos()), consumer, r, g, b, a);

        if (beacon.getLabel() != null && !beacon.getLabel().isEmpty()) {
            WorldElements.text(context, matrices, beacon.getLabel(), x + 0.5, y + 0.5, z + 0.5, beacon.getColor());
        }

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
            beacon.getColor(),
            0.2F,
            0.25F
        );

        matrices.pop();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        matrices.pop();
    }
}