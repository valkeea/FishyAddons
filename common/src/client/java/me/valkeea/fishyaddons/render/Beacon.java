package me.valkeea.fishyaddons.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class Beacon {
    private Beacon() {}
    private static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath("fishyaddons", "textures/block/beam.png");    

    public static void renderBeacon(LevelRenderContext ctx, IBeaconData beacon) {
        
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        var camPos = client.gameRenderer.getMainCamera().position();
        var matrices = ctx.poseStack();

        double x = beacon.getPos().getX();
        double y = beacon.getPos().getY();
        double z = beacon.getPos().getZ();

        float r = ((beacon.getColor() >> 16) & 0xFF) / 255f;
        float g = ((beacon.getColor() >> 8) & 0xFF) / 255f;
        float b = (beacon.getColor() & 0xFF) / 255f;
        float a = Math.max(0.2f, ((beacon.getColor() >> 24) & 0xFF) / 255f);

        matrices.pushPose();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        boolean wouldObstructView = client.player.position().distanceTo(beacon.getPos().getCenter()) < 5.0;
        if (!wouldObstructView) renderBeam(ctx, beacon, matrices);

        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        
        VertexConsumer fillConsumer = ctx.bufferSource().getBuffer(RenderTypes.debugQuads());
        WorldElements.boxFill(matrices, new AABB(beacon.getPos()), fillConsumer, r, g, b, 0.3f);

        VertexConsumer lineConsumer = ctx.bufferSource().getBuffer(RenderTypes.lines());
        WorldElements.boxOutline(matrices, new AABB(beacon.getPos()), lineConsumer, r, g, b, a);
        
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);

        if (beacon.getLabel() != null && !beacon.getLabel().isEmpty()) {
            WorldElements.text(ctx, matrices, beacon.getLabel(), x + 0.5, y + 1.5, z + 0.5, beacon.getColor());
        }
        
        matrices.popPose();
    }

    private static void renderBeam(LevelRenderContext ctx, IBeaconData beacon, PoseStack matrices) {
        matrices.pushPose();
        matrices.translate(beacon.getPos().getX() + 0.5, (double)beacon.getPos().getY() + 1, beacon.getPos().getZ() + 0.5);

        int lightColor = (beacon.getColor() & 0x00FFFFFF) | 0x05000000;
        renderCylinder(ctx, matrices, lightColor, 0.2F);
        
        matrices.popPose();

        matrices.pushPose();
        matrices.translate(beacon.getPos().getX(), (float)beacon.getPos().getY() + 1, beacon.getPos().getZ());
        BeaconRenderer.submitBeaconBeam(
            matrices,
            ctx.submitNodeCollector(),
            BEAM_TEXTURE,
            1.0f,
            1.0F,
            0,
            256,
            beacon.getColor(),
            0.2F,
            0.25F
        );

        matrices.popPose();
    }
    
    private static void renderCylinder(LevelRenderContext ctx, PoseStack matrices, int color, float outerSize) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = Math.clamp(((color >> 24) & 0xFF) / 255f, 0.10f, 0.15f);

        var consumer = ctx.bufferSource().getBuffer(RenderTypes.debugQuads());
        var matrix = matrices.last().pose();
        
        int segments = 8;
        double height = 256.0;
        
        for (int i = 0; i < segments; i++) {
            double angle1 = (2.0 * Math.PI * i) / segments;
            double angle2 = (2.0 * Math.PI * (i + 1)) / segments;
            
            double x1 = Math.cos(angle1) * outerSize;
            double z1 = Math.sin(angle1) * outerSize;
            double x2 = Math.cos(angle2) * outerSize;
            double z2 = Math.sin(angle2) * outerSize;
            
            consumer.addVertex(matrix, (float)x1, 0.0f, (float)z1)
                    .setColor(r, g, b, a).setNormal(0.0f, 1.0f, 0.0f);
            consumer.addVertex(matrix, (float)x1, (float)height, (float)z1)
                    .setColor(r, g, b, a).setNormal(0.0f, 1.0f, 0.0f);
            consumer.addVertex(matrix, (float)x2, (float)height, (float)z2)
                    .setColor(r, g, b, a).setNormal(0.0f, 1.0f, 0.0f);
            consumer.addVertex(matrix, (float)x2, 0.0f, (float)z2)
                    .setColor(r, g, b, a).setNormal(0.0f, 1.0f, 0.0f);
        }
    }
}
