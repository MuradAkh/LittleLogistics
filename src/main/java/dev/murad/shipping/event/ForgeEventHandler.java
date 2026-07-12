package dev.murad.shipping.event;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.rail.PortalRail;
import dev.murad.shipping.entity.custom.vessel.tug.VehicleFrontPart;
import dev.murad.shipping.global.PlayerTrainChunkManager;
import dev.murad.shipping.global.TrainChunkManagerManager;
import dev.murad.shipping.item.SpringItem;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.BlockUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.portal.PortalForcer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * Forge-wide event bus
 */
@EventBusSubscriber(modid = ShippingMod.MOD_ID)
public class ForgeEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        handleEvent(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entitySpecificInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        handleEvent(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onWorldTick(LevelTickEvent.Post event) {
        // Don't do anything client side
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TrainChunkManagerManager.get(serverLevel.getServer()).getManagers(event.getLevel().dimension()).forEach(PlayerTrainChunkManager::tick);
        }
    }

    @SubscribeEvent
    public static void onPlayerSignInEvent(PlayerEvent.PlayerLoggedInEvent event){
        if (event.getEntity().level().isClientSide() || ShippingConfig.Server.OFFLINE_LOADING.get()) {
            return;
        }

        TrainChunkManagerManager.get(event.getEntity().level().getServer())
                .getManagers(event.getEntity().getUUID())
                .forEach(PlayerTrainChunkManager::activate);
    }

    @SubscribeEvent
    public static void onPlayerSignInEvent(PlayerEvent.PlayerLoggedOutEvent event){
        if (event.getEntity().level().isClientSide || ShippingConfig.Server.OFFLINE_LOADING.get()) {
            return;
        }

        TrainChunkManagerManager.get(event.getEntity().level().getServer())
                .getManagers(event.getEntity().getUUID())
                .forEach(PlayerTrainChunkManager::deactivate);
    }

    /* Replaces a rail placed adjacent to a nether portal with a PortalRail and places a
       matching one on the destination side, so trains can cross without derailing. */
    @SubscribeEvent
    public static void onRailPlacedNearPortal(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState placed = event.getPlacedBlock();
        if (!(placed.getBlock() instanceof BaseRailBlock) || placed.getBlock() instanceof PortalRail) return;

        BlockPos railPos = event.getPos();

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = railPos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!neighborState.is(Blocks.NETHER_PORTAL)) continue;

            // Skip if a PortalRail already exists adjacent to this portal; avoids duplicate pairs.
            boolean alreadyLinked = false;
            for (Direction checkDir : Direction.Plane.HORIZONTAL) {
                BlockPos adj = neighborPos.relative(checkDir);
                if (!adj.equals(railPos) && level.getBlockState(adj).getBlock() instanceof PortalRail) {
                    alreadyLinked = true;
                    break;
                }
            }
            if (alreadyLinked) break;

            RailShape railShape = (dir.getAxis() == Direction.Axis.Z) ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;
            // Portal axis is perpendicular to travel direction: traveling N/S means the portal faces along X.
            Direction.Axis portalAxis = neighborState.getValue(BlockStateProperties.HORIZONTAL_AXIS);

            // Don't place the source rail if the destination can't accept one; a one-sided pair would strand any train that crosses.
            if (!placeDestinationPortalRail(level, railPos, dir, portalAxis)) break;

            BlockState portalRailState = ModBlocks.PORTAL_RAIL.get().defaultBlockState()
                    .setValue(PortalRail.RAIL_SHAPE, railShape)
                    .setValue(PortalRail.PORTAL_FACING, dir);
            level.setBlock(railPos, portalRailState, Block.UPDATE_ALL);

            break;
        }
    }

    /* Places a PortalRail on the exit side of the destination portal so trains continue in the same
       direction after crossing (north in → north out). Returns false if the exit position is blocked. */
    private static boolean placeDestinationPortalRail(ServerLevel sourceLevel, BlockPos sourceRailPos,
                                                       Direction entryDir, Direction.Axis sourcePortalAxis) {
        ResourceKey<Level> srcDim = sourceLevel.dimension();
        ResourceKey<Level> destDim = srcDim == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
        ServerLevel destLevel = sourceLevel.getServer().getLevel(destDim);
        if (destLevel == null) return false;

        double scale = srcDim == Level.NETHER ? 8.0 : 0.125;
        BlockPos scaledPos = BlockPos.containing(
                sourceRailPos.getX() * scale,
                sourceRailPos.getY(),
                sourceRailPos.getZ() * scale);

        PortalForcer forcer = destLevel.getPortalForcer();
        Optional<BlockPos> exitPortal = forcer.findClosestPortalPosition(
                scaledPos, destDim == Level.NETHER, destLevel.getWorldBorder());

        // Create the portal if it doesn't exist yet; rail placement must happen before the first entity crossing.
        if (exitPortal.isEmpty()) {
            exitPortal = forcer.createPortal(scaledPos, sourcePortalAxis)
                    .map(rect -> rect.minCorner);
        }

        if (exitPortal.isEmpty()) return false;

        // Scan down to the floor-level portal block; findClosestPortalPosition may return any block in the column.
        BlockPos floor = exitPortal.get();
        while (destLevel.getBlockState(floor.below()).is(Blocks.NETHER_PORTAL)) {
            floor = floor.below();
        }
        if (!destLevel.getBlockState(floor).is(Blocks.NETHER_PORTAL)) return false;

        // Skip if this destination portal already has a PortalRail; it may have been placed from the other side.
        for (Direction checkDir : Direction.Plane.HORIZONTAL) {
            if (destLevel.getBlockState(floor.relative(checkDir)).getBlock() instanceof PortalRail) return false;
        }

        // Exit rail is on the entryDir side so the train exits pointing the same direction it entered: north in → north out.
        BlockPos railPos = floor.relative(entryDir);
        RailShape exitShape = entryDir.getAxis() == Direction.Axis.Z
                ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;

        if (!destLevel.getBlockState(railPos.below()).isSolid()
                || !destLevel.getBlockState(railPos).canBeReplaced()) {
            return false;
        }

        // PORTAL_FACING is entryDir.getOpposite(): from the exit rail's perspective, the portal is behind it.
        BlockState exitRailState = ModBlocks.PORTAL_RAIL.get().defaultBlockState()
                .setValue(PortalRail.RAIL_SHAPE, exitShape)
                .setValue(PortalRail.PORTAL_FACING, entryDir.getOpposite());
        destLevel.setBlock(railPos, exitRailState, Block.UPDATE_ALL);
        return true;
    }

    private static void handleEvent(PlayerInteractEvent event, Entity target) {
        if(!event.getItemStack().isEmpty()) {
            Item item = event.getItemStack().getItem();
            if(item instanceof SpringItem springItem) {
                if(target instanceof LinkableEntity || target instanceof VehicleFrontPart) {
                    springItem.onUsedOnEntity(event.getItemStack(), event.getEntity(), event.getLevel(), target);
                    cancelEvent(event);
                }
            }

            if(item instanceof ShearsItem) {
                if(target instanceof LinkableEntity v) {
                    v.handleShearsCut();
                    cancelEvent(event);
                }
            }
        }
    }

    private static void cancelEvent(PlayerInteractEvent event) {
        if (event instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }
        if (event instanceof PlayerInteractEvent.EntityInteract e) {
            e.setCancellationResult(InteractionResult.SUCCESS);
        } else if (event instanceof PlayerInteractEvent.EntityInteractSpecific e) {
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}