package me.valkeea.fishyaddons.render;

import org.joml.Matrix4f;

import me.valkeea.fishyaddons.util.text.Enhancer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class WorldElements {
    private WorldElements() {}

    /**
     * Renders a floating text at the specified world coordinates.
     */
    protected static void text(WorldRenderContext context, 
     MatrixStack matrices, String text, double x, double y, double z, int color) {

        var client = MinecraftClient.getInstance();
        if (client.textRenderer == null || text == null || text.isEmpty()) return;
        
        var textRenderer = client.textRenderer;
        Vec3d playerPos = client.player.getEntityPos();
        
        double distance = Math.sqrt(
            Math.pow(x - playerPos.x, 2) + 
            Math.pow(y - playerPos.y, 2) + 
            Math.pow(z - playerPos.z, 2)
        );

        float baseScale = 0.08f;
        float distanceScale = Math.max(1.0f, (float)(distance / 20.0f));
        float finalScale = baseScale * distanceScale;
        var rotation = client.gameRenderer.getCamera().getRotation();
        
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(rotation);
        matrices.scale(finalScale, -finalScale, finalScale);

        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);

        Text parsed = Enhancer.parseFormattedText(text);
        
        if ((color & 0xFF000000) == 0) color = color | 0xFF000000;
        
        var commandQueue = context.commandQueue();
        if (commandQueue != null) {
            try {

                float width = textRenderer.getWidth(parsed.getString());
                
                commandQueue.submitText(
                    matrices,
                    -width / 2f,
                    0.0f,
                    parsed.asOrderedText(),
                    false,
                    TextRenderer.TextLayerType.SEE_THROUGH,
                    0xF000F000,
                    color,
                    0x80000000,
                    0
                );
            } catch (Exception e) {
                System.err.println("[FishyAddons] Failed to submit text to command queue: " + e.getMessage());
                var consumers = context.consumers();
                float textWidth = textRenderer.getWidth(parsed.getString());
                
                textRenderer.draw(
                    parsed,
                    -textWidth / 2f,
                    0,
                    color,
                    false,
                    matrices.peek().getPositionMatrix(),
                    consumers,
                    TextRenderer.TextLayerType.SEE_THROUGH,
                    0x80000000,
                    0xF000F0
                );
            }
        }
        
        matrices.pop();
    }

    protected static void boxOutline(MatrixStack matrices, Box box, VertexConsumer consumer, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float[] color = {r, g, b, a};

        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        line(consumer, matrix, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), color);
        line(consumer, matrix, new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), color);
        line(consumer, matrix, new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), color);
        line(consumer, matrix, new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), color);

        line(consumer, matrix, new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), color);
        line(consumer, matrix, new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), color);
        line(consumer, matrix, new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), color);
        line(consumer, matrix, new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), color);

        line(consumer, matrix, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), color);
        line(consumer, matrix, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), color);
        line(consumer, matrix, new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), color);
        line(consumer, matrix, new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), color);
    }

    private static void line(VertexConsumer consumer, Matrix4f matrix,
                             Vec3d start, Vec3d end, float[] color) {
        consumer.vertex(matrix, (float)start.x, (float)start.y, (float)start.z)
                .color(color[0], color[1], color[2], color[3]).normal(0, 1, 0);
        consumer.vertex(matrix, (float)end.x, (float)end.y, (float)end.z)
                .color(color[0], color[1], color[2], color[3]).normal(0, 1, 0);
    }

    protected static void boxFill(MatrixStack matrices, Box box, VertexConsumer consumer, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float[] color = {r, g, b, a};

        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        quad(consumer, matrix, 
             new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), 
             new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), color);

        quad(consumer, matrix,
             new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2),
             new Vec3d(x2, y2, z1), new Vec3d(x1, y2, z1), color);

        quad(consumer, matrix,
             new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1),
             new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), color);

        quad(consumer, matrix,
             new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2),
             new Vec3d(x1, y2, z2), new Vec3d(x1, y1, z2), color);

        quad(consumer, matrix,
             new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2),
             new Vec3d(x1, y2, z1), new Vec3d(x1, y1, z1), color);

        quad(consumer, matrix,
             new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1),
             new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), color);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                            Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, float[] color) {

        Vec3d edge1 = v2.subtract(v1);
        Vec3d edge2 = v3.subtract(v1);
        Vec3d normal = edge1.crossProduct(edge2).normalize();
        
        float nx = (float) normal.x;
        float ny = (float) normal.y;
        float nz = (float) normal.z;

        consumer.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z)
                .color(color[0], color[1], color[2], color[3]).normal(nx, ny, nz);
        consumer.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z)
                .color(color[0], color[1], color[2], color[3]).normal(nx, ny, nz);
        consumer.vertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z)
                .color(color[0], color[1], color[2], color[3]).normal(nx, ny, nz);
        consumer.vertex(matrix, (float)v4.x, (float)v4.y, (float)v4.z)
                .color(color[0], color[1], color[2], color[3]).normal(nx, ny, nz);
    }
}
