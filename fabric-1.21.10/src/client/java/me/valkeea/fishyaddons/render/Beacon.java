package me.valkeea.fishyaddons.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public class Beacon {
    private Beacon() {}
    private static final Identifier BEAM_TEXTURE = Identifier.of("fishyaddons", "textures/block/beam.png");    

    public static void renderBeacon(WorldRenderContext context, IBeaconData beacon) {
        
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        var camPos = client.gameRenderer.getCamera().getPos();
        var matrices = context.matrices();

        double x = beacon.getPos().getX();
        double y = beacon.getPos().getY();
        double z = beacon.getPos().getZ();

        float r = ((beacon.getColor() >> 16) & 0xFF) / 255f;
        float g = ((beacon.getColor() >> 8) & 0xFF) / 255f;
        float b = (beacon.getColor() & 0xFF) / 255f;
        float a = Math.max(0.2f, ((beacon.getColor() >> 24) & 0xFF) / 255f);

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        boolean wouldObstructView = client.player.getEntityPos().distanceTo(beacon.getPos().toCenterPos()) < 5.0;
        if (!wouldObstructView) renderBeam(context, beacon, matrices);

        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        
        VertexConsumer fillConsumer = context.consumers().getBuffer(RenderLayer.getDebugQuads());
        WorldElements.boxFill(matrices, new Box(beacon.getPos()), fillConsumer, r, g, b, 0.3f);

        VertexConsumer lineConsumer = context.consumers().getBuffer(RenderLayer.getLines());
        WorldElements.boxOutline(matrices, new Box(beacon.getPos()), lineConsumer, r, g, b, a);
        
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);

        if (beacon.getLabel() != null && !beacon.getLabel().isEmpty()) {
            WorldElements.text(context, matrices, beacon.getLabel(), x + 0.5, y + 1.5, z + 0.5, beacon.getColor());
        }
        
        matrices.pop();
    }

    private static void renderBeam(WorldRenderContext context, IBeaconData beacon, MatrixStack matrices) {
        matrices.push();
        matrices.translate(beacon.getPos().getX() + 0.5, (double)beacon.getPos().getY() + 1, beacon.getPos().getZ() + 0.5);

        int lightColor = (beacon.getColor() & 0x00FFFFFF) | 0x05000000;
        renderCylinder(context, matrices, lightColor, 0.2F);
        
        matrices.pop();

        matrices.push();
        matrices.translate(beacon.getPos().getX(), (float)beacon.getPos().getY() + 1, beacon.getPos().getZ());
        BeaconBlockEntityRenderer.renderBeam(
            matrices,
            context.commandQueue(),
            BEAM_TEXTURE,
            1.0f,
            1.0F,
            0,
            256,
            beacon.getColor(),
            0.2F,
            0.25F
        );

        matrices.pop();
    }
    
    private static void renderCylinder(WorldRenderContext context, MatrixStack matrices, int color, float outerSize) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = Math.clamp(((color >> 24) & 0xFF) / 255f, 0.10f, 0.15f);

        var consumer = context.consumers().getBuffer(RenderLayer.getDebugQuads());
        var matrix = matrices.peek().getPositionMatrix();
        
        int segments = 8;
        double height = 256.0;
        
        for (int i = 0; i < segments; i++) {
            double angle1 = (2.0 * Math.PI * i) / segments;
            double angle2 = (2.0 * Math.PI * (i + 1)) / segments;
            
            double x1 = Math.cos(angle1) * outerSize;
            double z1 = Math.sin(angle1) * outerSize;
            double x2 = Math.cos(angle2) * outerSize;
            double z2 = Math.sin(angle2) * outerSize;
            
            consumer.vertex(matrix, (float)x1, 0.0f, (float)z1)
                    .color(r, g, b, a).normal(0.0f, 1.0f, 0.0f);
            consumer.vertex(matrix, (float)x1, (float)height, (float)z1)
                    .color(r, g, b, a).normal(0.0f, 1.0f, 0.0f);
            consumer.vertex(matrix, (float)x2, (float)height, (float)z2)
                    .color(r, g, b, a).normal(0.0f, 1.0f, 0.0f);
            consumer.vertex(matrix, (float)x2, 0.0f, (float)z2)
                    .color(r, g, b, a).normal(0.0f, 1.0f, 0.0f);
        }
    }
}
