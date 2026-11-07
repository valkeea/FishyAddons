package me.valkeea.fishyaddons.feature.skyblock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.valkeea.fishyaddons.api.skyblock.IslandRegion;
import me.valkeea.fishyaddons.api.skyblock.SafariData.SafariZone;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@VCModule
public class EntityHighlight {
    private EntityHighlight() {}

    private interface Highlighter {
        /** Island check and any enabled-by-config check. Evaluated once per frame. */
        boolean active();

        /** Called once per frame before visiting entities. */
        default void begin(Frustum frustum, float partialTicks) {}

        /** False if the highlighter works from its own tracked state and needs no level scan. */
        default boolean scansEntities() {
            return true;
        }

        default void visit(Entity e, Frustum frustum, float partialTicks) {}
    }

    private static final Highlighter SAFARI = new Highlighter() {
        @Override
        public boolean active() {
            return (critterColor != 0 || dropColor != 0) && SkyblockAreas.isSafari();
        }

        @Override
        public void begin(Frustum frustum, float partialTicks) {
            drawnDrops.clear();
        }

        @Override
        public void visit(Entity e, Frustum frustum, float partialTicks) {
            if (dropColor != 0 && e instanceof Display.ItemDisplay d && isFloorDrop(d)) {
                drawFloorDrop(d, frustum);
            } else if (critterColor != 0 && isCritter(e)) {
                drawEntity(e, frustum, partialTicks, critterColor);
            }
        }
    };

    private static final List<Highlighter> HIGHLIGHTERS = List.of(SAFARI);
    private static final List<Highlighter> activeHighlighters = new ArrayList<>(HIGHLIGHTERS.size());

    public static void emit(Frustum frustum, float partialTicks) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;

        var active = activeHighlighters;
        active.clear();
        boolean scan = false;
        for (var h : HIGHLIGHTERS) {
            if (h.active()) {
                active.add(h);
                scan |= h.scansEntities();
            }
        }
        if (active.isEmpty()) return;

        if (mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)) return;

        for (int i = 0; i < active.size(); i++) {
            active.get(i).begin(frustum, partialTicks);
        }
        if (!scan) return;

        var camera = mc.getCameraEntity();
        for (Entity e : level.entitiesForRendering()) {
            if (e == camera) continue;
            for (int i = 0; i < active.size(); i++) {
                active.get(i).visit(e, frustum, partialTicks);
            }
        }
    }

    private static void drawEntity(Entity e, Frustum frustum, float partialTicks, int color) {
        var box = e instanceof Display display ? displayBox(display) : e.getBoundingBox();
        if (box == null) return;

        var offset = e.getPosition(partialTicks).subtract(e.position());
        draw(box, offset, frustum, color);
    }

    private static void draw(AABB box, Vec3 offset, Frustum frustum, int color) {
        box = box.move(offset);
        if (frustum.isVisible(box)) {
            Gizmos.cuboid(box, GizmoStyle.stroke(color));
        }
    }

    // Calc the box for a display entity based on its rendered transformation if 0 size
    private static AABB displayBox(Display d) {
        var box = d.getBoundingBox();
        if (box.getXsize() > 0 && box.getYsize() > 0) return box;

        var state = d.renderState();
        if (state == null) return null;

        var transform = state.transformation().get(1.0F);
        var scale = transform.scale();
        var t = transform.translation();

        double hx = Math.abs(scale.x()) / 2.0;
        double hy = Math.abs(scale.y()) / 2.0;
        double hz = Math.abs(scale.z()) / 2.0;
        double cx = d.getX() + t.x();
        double cy = d.getY() + t.y();
        double cz = d.getZ() + t.z();

        return new AABB(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);
    }

    // --- Safari ---

    private static int critterColor;
    private static int dropColor;
    private static final double DROP_BOX_LIFT = 0.2;

    private static final Set<BlockPos> drawnDrops = new HashSet<>();

    @VCListener(ints = IntKey.CRITTER_COLOR)
    public static void onCritterColor(int color) {
        critterColor = color;
    }

    @VCListener(ints = IntKey.FLOOR_DROP_COLOR)
    public static void onDropColor(int color) {
        dropColor = color;
    }

    // 3 displays in a triangle on one clickable block, box drawn once
    private static void drawFloorDrop(Display.ItemDisplay d, Frustum frustum) {
        var box = displayBox(d);
        if (box == null) return;

        var pos = BlockPos.containing(box.getCenter());
        if (drawnDrops.add(pos)) {
            var lifted = new AABB(pos).inflate(0.0, DROP_BOX_LIFT, 0.0);
            draw(lifted, Vec3.ZERO, frustum, dropColor);
        }
    }

    private static boolean isFloorDrop(Display.ItemDisplay display) {
        return display.getItemStack().is(Items.STRING);
    }

    private static boolean isCritter(Entity e) {
        return switch (e) {
            case ArmorStand s -> s.getItemBySlot(EquipmentSlot.HEAD).is(Items.PLAYER_HEAD);
            case Player p -> p.getMainHandItem().isEmpty() && isInHauntedZone();
            case Interaction _ -> isInHauntedZone();
            case Display d -> d instanceof Display.ItemDisplay id && isValidType(id);
            default -> (!e.isInvisible() && e instanceof LivingEntity) || e instanceof Zombie;
        };
    }

    private static boolean isValidType(Display.ItemDisplay id) {
        var stack = id.getItemStack();
        return stack.is(Items.PLAYER_HEAD) || stack.is(McApi.getPurpleShulker()) || stack.is(McApi.getGreenShulker());
    }

    private static boolean isInHauntedZone() {
        return IslandRegion.getPlayerSafariZone() == SafariZone.HAUNTED;
    }
}
