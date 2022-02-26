package dev.murad.shipping.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RailShape;

public class RailShapeUtil {

    public static final EnumProperty<RailShape> RAIL_SHAPE_STRAIGHT_FLAT = EnumProperty.create("shape", RailShape.class, (s) -> s == RailShape.NORTH_SOUTH || s == RailShape.EAST_WEST);
    public static final RailShape DEFAULT = RailShape.NORTH_SOUTH;

    public static RailShape getRailShape(Direction node1, Direction node2) {
       return switch (node1) {
           case NORTH ->
                   switch (node2) {
                       case SOUTH -> RailShape.NORTH_SOUTH;
                       case EAST -> RailShape.NORTH_EAST;
                       case WEST -> RailShape.NORTH_WEST;
                       default -> DEFAULT;
                   };
           case EAST ->
                   switch (node2) {
                       case WEST -> RailShape.EAST_WEST;
                       case NORTH -> RailShape.NORTH_EAST;
                       case SOUTH -> RailShape.SOUTH_EAST;
                       default -> DEFAULT;
                   };
           case SOUTH ->
                   switch (node2) {
                       case NORTH -> RailShape.NORTH_SOUTH;
                       case EAST -> RailShape.SOUTH_EAST;
                       case WEST -> RailShape.SOUTH_WEST;
                       default -> DEFAULT;
                   };
           case WEST ->
                   switch (node2) {
                       case EAST -> RailShape.EAST_WEST;
                       case NORTH -> RailShape.NORTH_WEST;
                       case SOUTH -> RailShape.SOUTH_WEST;
                       default -> DEFAULT;
                   };
           default -> DEFAULT;
       };
    }
}
