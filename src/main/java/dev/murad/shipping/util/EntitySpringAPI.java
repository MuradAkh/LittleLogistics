package dev.murad.shipping.util;

import dev.murad.shipping.entity.custom.ModBargeEntity;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.TugEntity;
import dev.murad.shipping.item.SpringItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class EntitySpringAPI {
    private EntitySpringAPI() {
    }

    private static final BiFunction<Entity, SpringEntity.SpringSide, Vector3d> DEFAULT_ANCHOR_LOCATION = (e, sideArg) -> e.position();
    private static final List<Predicate<Entity>> predicates = new ArrayList<>();
    private static final Map<Class<? extends Entity>, BiFunction<Entity, SpringEntity.SpringSide, Vector3d>> mapping = new HashMap<>();
    public static final BiFunction<Entity, SpringEntity.SpringSide, Vector3d> DEFAULT_BOAT_ANCHOR = (entity, side) -> {
        float distanceFromCenter = 0.0625f * 17f * (side == SpringEntity.SpringSide.DOMINANT ? 1f : -1f);
        double anchorX = entity.getX() + MathHelper.cos((float) ((entity.yRot + 90f) * Math.PI / 180f)) * distanceFromCenter;
        double anchorY = entity.getY();
        double anchorZ = entity.getZ() + MathHelper.sin((float)((entity.yRot + 90f) * Math.PI / 180f)) * distanceFromCenter;
        return new Vector3d(anchorX, anchorY, anchorZ);
    };

    static {
        addGenericAnchorMapping(BoatEntity.class, DEFAULT_BOAT_ANCHOR);
    }

    public static boolean isValidTarget(Entity target, SpringItem.State state) {
        return (target instanceof TugEntity && state == SpringItem.State.READY) || target instanceof ModBargeEntity;
    }

    public static void addGenericAnchorMapping(Class<? extends Entity> entity, BiFunction<Entity, SpringEntity.SpringSide, Vector3d> function) {
        mapping.put(entity, function);
    }

    public static <T extends Entity> void addAnchorMapping(Class<? extends T> entity, BiFunction<T, SpringEntity.SpringSide, Vector3d> function) {
        mapping.put(entity, (e, side) -> function.apply((T) e, side));
    }

    public static Vector3d calculateAnchorPosition(Entity entity, SpringEntity.SpringSide side) {
        BiFunction<Entity, SpringEntity.SpringSide, Vector3d> function = mapping.getOrDefault(entity.getClass(), DEFAULT_ANCHOR_LOCATION);
        return function.apply(entity, side);
    }
}
