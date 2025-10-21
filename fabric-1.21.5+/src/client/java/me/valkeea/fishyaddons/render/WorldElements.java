package me.valkeea.fishyaddons.render;

import org.joml.Matrix4f;

import me.valkeea.fishyaddons.util.text.Enhancer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class WorldElements {
    private WorldElements() {}

    protected static void text(WorldRenderContext context, 
     MatrixStack matrices, String text, double x, double y, double z, int color) {

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        
        Vec3d playerPos = context.camera().getPos();
        double distance = Math.sqrt(
            Math.pow(x - playerPos.x, 2) + 
            Math.pow(y - playerPos.y, 2) + 
            Math.pow(z - playerPos.z, 2)
        );

        float baseScale = 0.07f;
        float distanceScale = Math.max(1.0f, (float)(distance / 20.0f));
        float finalScale = baseScale * distanceScale;
        
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(context.camera().getRotation());
        matrices.scale(finalScale, -finalScale, finalScale);

        Text parsed = Enhancer.parseFormattedText(text);
        float width = textRenderer.getWidth(parsed.getString());
        
        textRenderer.draw(
            parsed,
            -width / 2f,
            0,
            color,
            false,
            matrices.peek().getPositionMatrix(),
            context.consumers(),
            TextRenderer.TextLayerType.NORMAL,
            0x80000000,
            0xF000F0
        );
        
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

        // Bottom square
        line(consumer, matrix, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), color);
        line(consumer, matrix, new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), color);
        line(consumer, matrix, new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), color);
        line(consumer, matrix, new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), color);

        // Top square
        line(consumer, matrix, new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), color);
        line(consumer, matrix, new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), color);
        line(consumer, matrix, new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), color);
        line(consumer, matrix, new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), color);

        // Vertical lines
        line(consumer, matrix, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), color);
        line(consumer, matrix, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), color);
        line(consumer, matrix, new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), color);
        line(consumer, matrix, new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), color);
    }

    protected static void line(VertexConsumer consumer, Matrix4f matrix,
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

        // Bottom face (y = y1)
        quad(consumer, matrix, 
             new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), 
             new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), color);

        // Top face (y = y2)
        quad(consumer, matrix,
             new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2),
             new Vec3d(x2, y2, z1), new Vec3d(x1, y2, z1), color);

        // North face (z = z1)
        quad(consumer, matrix,
             new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1),
             new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), color);

        // South face (z = z2)
        quad(consumer, matrix,
             new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2),
             new Vec3d(x1, y2, z2), new Vec3d(x1, y1, z2), color);

        // West face (x = x1)
        quad(consumer, matrix,
             new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2),
             new Vec3d(x1, y2, z1), new Vec3d(x1, y1, z1), color);

        // East face (x = x2)
        quad(consumer, matrix,
             new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1),
             new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), color);
    }

    protected static void quad(VertexConsumer consumer, Matrix4f matrix,
                            Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, float[] color) {

        // Calculate normal vector for proper lighting
        Vec3d edge1 = v2.subtract(v1);
        Vec3d edge2 = v3.subtract(v1);
        Vec3d normal = edge1.crossProduct(edge2).normalize();
        
        float nx = (float) normal.x;
        float ny = (float) normal.y;
        float nz = (float) normal.z;

        // Draw the quad (two triangles)
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
