package me.valkeea.fishyaddons.render;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.valkeea.fishyaddons.util.text.Enhancer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WorldElements {
    private WorldElements() {}

    /**
     * Renders a floating text at the specified world coordinates.
     */
    protected static void text(LevelRenderContext ctx, 
     PoseStack matrices, String text, double x, double y, double z, int color) {

        var mc = Minecraft.getInstance();
        var textRenderer = mc.font;
        if (textRenderer == null || text == null || text.isEmpty()) return;
        
        Vec3 playerPos = mc.player.position();
        
        double distance = Math.sqrt(
            Math.pow(x - playerPos.x, 2) + 
            Math.pow(y - playerPos.y, 2) + 
            Math.pow(z - playerPos.z, 2)
        );

        float baseScale = 0.08f;
        float distanceScale = Math.max(1.0f, (float)(distance / 20.0f));
        float finalScale = baseScale * distanceScale;
        var rotation = mc.gameRenderer.getMainCamera().rotation();
        
        matrices.pushPose();
        matrices.translate(x, y, z);
        matrices.mulPose(rotation);
        matrices.scale(finalScale, -finalScale, finalScale);

        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);

        Component parsed = Enhancer.parseFormattedText(text);
        
        if ((color & 0xFF000000) == 0) color = color | 0xFF000000;
        
        var nodes = ctx.submitNodeCollector();
        if (nodes != null) {
            try {

                float width = textRenderer.width(parsed.getString());
                
                nodes.submitText(
                    matrices,
                    -width / 2f,
                    0.0f,
                    parsed.getVisualOrderText(),
                    false,
                    Font.DisplayMode.SEE_THROUGH,
                    0xF000F000,
                    color,
                    0x80000000,
                    0
                );

            } catch (Exception _) {
                var consumers = ctx.bufferSource();
                float textWidth = textRenderer.width(parsed.getString());
                
                textRenderer.drawInBatch(
                    parsed,
                    -textWidth / 2f,
                    0,
                    color,
                    false,
                    matrices.last().pose(),
                    consumers,
                    Font.DisplayMode.SEE_THROUGH,
                    0x80000000,
                    0xF000F0
                );
            }
        }
        
        matrices.popPose();
    }

    protected static void boxOutline(PoseStack matrices, AABB box, VertexConsumer consumer, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.last().pose();
        float[] color = {r, g, b, a};

        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        line(consumer, matrix, new Vec3(x1, y1, z1), new Vec3(x2, y1, z1), color);
        line(consumer, matrix, new Vec3(x2, y1, z1), new Vec3(x2, y1, z2), color);
        line(consumer, matrix, new Vec3(x2, y1, z2), new Vec3(x1, y1, z2), color);
        line(consumer, matrix, new Vec3(x1, y1, z2), new Vec3(x1, y1, z1), color);

        line(consumer, matrix, new Vec3(x1, y2, z1), new Vec3(x2, y2, z1), color);
        line(consumer, matrix, new Vec3(x2, y2, z1), new Vec3(x2, y2, z2), color);
        line(consumer, matrix, new Vec3(x2, y2, z2), new Vec3(x1, y2, z2), color);
        line(consumer, matrix, new Vec3(x1, y2, z2), new Vec3(x1, y2, z1), color);

        line(consumer, matrix, new Vec3(x1, y1, z1), new Vec3(x1, y2, z1), color);
        line(consumer, matrix, new Vec3(x2, y1, z1), new Vec3(x2, y2, z1), color);
        line(consumer, matrix, new Vec3(x2, y1, z2), new Vec3(x2, y2, z2), color);
        line(consumer, matrix, new Vec3(x1, y1, z2), new Vec3(x1, y2, z2), color);
    }

    private static void line(VertexConsumer consumer, Matrix4f matrix,
                             Vec3 start, Vec3 end, float[] color) {
        consumer.addVertex(matrix, (float)start.x, (float)start.y, (float)start.z)
                .setColor(color[0], color[1], color[2], color[3]).setNormal(0, 1, 0)
                .setLineWidth(1.0f);
        consumer.addVertex(matrix, (float)end.x, (float)end.y, (float)end.z)
                .setColor(color[0], color[1], color[2], color[3]).setNormal(0, 1, 0)
                .setLineWidth(1.0f);
    }

    protected static void boxFill(PoseStack matrices, AABB box, VertexConsumer consumer, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.last().pose();
        float[] color = {r, g, b, a};

        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        quad(consumer, matrix, 
             new Vec3(x1, y1, z1), new Vec3(x2, y1, z1), 
             new Vec3(x2, y1, z2), new Vec3(x1, y1, z2), color);

        quad(consumer, matrix,
             new Vec3(x1, y2, z2), new Vec3(x2, y2, z2),
             new Vec3(x2, y2, z1), new Vec3(x1, y2, z1), color);

        quad(consumer, matrix,
             new Vec3(x1, y1, z1), new Vec3(x1, y2, z1),
             new Vec3(x2, y2, z1), new Vec3(x2, y1, z1), color);

        quad(consumer, matrix,
             new Vec3(x2, y1, z2), new Vec3(x2, y2, z2),
             new Vec3(x1, y2, z2), new Vec3(x1, y1, z2), color);

        quad(consumer, matrix,
             new Vec3(x1, y1, z2), new Vec3(x1, y2, z2),
             new Vec3(x1, y2, z1), new Vec3(x1, y1, z1), color);

        quad(consumer, matrix,
             new Vec3(x2, y1, z1), new Vec3(x2, y2, z1),
             new Vec3(x2, y2, z2), new Vec3(x2, y1, z2), color);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                            Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, float[] color) {

        Vec3 edge1 = v2.subtract(v1);
        Vec3 edge2 = v3.subtract(v1);
        Vec3 normal = edge1.cross(edge2).normalize();
        
        float nx = (float) normal.x;
        float ny = (float) normal.y;
        float nz = (float) normal.z;

        consumer.addVertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z)
                .setColor(color[0], color[1], color[2], color[3]).setNormal(nx, ny, nz)
                .setLineWidth(1.0f);
        consumer.addVertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z)
                .setColor(color[0], color[1], color[2], color[3]).setNormal(nx, ny, nz)
                .setLineWidth(1.0f);
        consumer.addVertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z)
                .setColor(color[0], color[1], color[2], color[3]).setNormal(nx, ny, nz)
                .setLineWidth(1.0f);
        consumer.addVertex(matrix, (float)v4.x, (float)v4.y, (float)v4.z)
                .setColor(color[0], color[1], color[2], color[3]).setNormal(nx, ny, nz)
                .setLineWidth(1.0f);
    }
}
