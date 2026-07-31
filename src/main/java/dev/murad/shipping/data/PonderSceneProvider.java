package dev.murad.shipping.data;

import com.google.common.hash.Hashing;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Generates the compact structure templates used by Little Logistics Ponder scenes. */
public final class PonderSceneProvider implements DataProvider {

    private final PackOutput output;

    public PonderSceneProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        return CompletableFuture.runAsync(() -> {
            writeTrainLinkingScene(cachedOutput, scenePath("train_linking"));
            writeTrainRoutingScene(cachedOutput, scenePath("train_routing"));
            writeTrainDockingScene(cachedOutput, scenePath("train_docking"));
            writeTugLinkingScene(cachedOutput, scenePath("tug_linking"));
            writeTugRoutingScene(cachedOutput, scenePath("tug_routing"));
            writeTugDockingScene(cachedOutput, scenePath("tug_docking"));
        });
    }

    @Override
    public String getName() {
        return "Little Logistics Ponder scenes";
    }

    private Path scenePath(String name) {
        return output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve("littlelogistics/ponder/%s.nbt".formatted(name));
    }

    private static void writeTrainLinkingScene(CachedOutput cachedOutput, Path path) {
        CompoundTag template = new CompoundTag();
        template.put("size", integerList(5, 2, 7));
        template.put("palette", palette());
        template.put("blocks", blocks());
        template.put("entities", new ListTag());
        template.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(template, output);
            byte[] bytes = output.toByteArray();
            cachedOutput.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write Ponder scene " + path, exception);
        }
    }

    private static void writeTrainRoutingScene(CachedOutput cachedOutput, Path path) {
        CompoundTag template = new CompoundTag();
        template.put("size", integerList(15, 2, 15));
        template.put("palette", routingPalette());
        template.put("blocks", routingBlocks());
        template.put("entities", new ListTag());
        template.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(template, output);
            byte[] bytes = output.toByteArray();
            cachedOutput.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write Ponder scene " + path, exception);
        }
    }

    private static void writeTrainDockingScene(CachedOutput cachedOutput, Path path) {
        CompoundTag template = new CompoundTag();
        template.put("size", integerList(12, 3, 7));
        template.put("palette", dockingPalette());
        template.put("blocks", dockingBlocks());
        template.put("entities", new ListTag());
        template.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(template, output);
            byte[] bytes = output.toByteArray();
            cachedOutput.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write Ponder scene " + path, exception);
        }
    }

    private static void writeTugLinkingScene(CachedOutput cachedOutput, Path path) {
        writeScene(cachedOutput, path, integerList(5, 2, 8), tugLinkingPalette(), tugLinkingBlocks());
    }

    private static void writeTugRoutingScene(CachedOutput cachedOutput, Path path) {
        writeScene(cachedOutput, path, integerList(13, 2, 9), tugRoutingPalette(), tugRoutingBlocks());
    }

    private static void writeTugDockingScene(CachedOutput cachedOutput, Path path) {
        writeScene(cachedOutput, path, integerList(12, 4, 6), tugDockingPalette(), tugDockingBlocks());
    }

    private static void writeScene(CachedOutput cachedOutput, Path path, ListTag size,
                                   ListTag scenePalette, ListTag sceneBlocks) {
        CompoundTag template = new CompoundTag();
        template.put("size", size);
        template.put("palette", scenePalette);
        template.put("blocks", sceneBlocks);
        template.put("entities", new ListTag());
        template.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(template, output);
            byte[] bytes = output.toByteArray();
            cachedOutput.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write Ponder scene " + path, exception);
        }
    }

    private static ListTag palette() {
        ListTag palette = new ListTag();
        palette.add(blockState("minecraft:polished_andesite"));

        CompoundTag rail = blockState("minecraft:rail");
        CompoundTag properties = new CompoundTag();
        properties.putString("shape", "north_south");
        rail.put("Properties", properties);
        palette.add(rail);
        return palette;
    }

    private static ListTag routingPalette() {
        ListTag palette = new ListTag();
        palette.add(blockState("minecraft:polished_andesite"));
        palette.add(railBlockState("north_south"));
        palette.add(railBlockState("east_west"));
        palette.add(railBlockState("south_east"));
        palette.add(railBlockState("south_west"));
        palette.add(railBlockState("north_east"));
        palette.add(railBlockState("north_west"));
        palette.add(automaticTeeJunctionBlockState());
        return palette;
    }

    private static ListTag dockingPalette() {
        ListTag palette = new ListTag();
        palette.add(blockState("minecraft:polished_andesite"));
        palette.add(railBlockState("east_west"));
        palette.add(dockingStationBlockState("controller", "14"));
        palette.add(dockingStationBlockState("left_top", "14"));
        palette.add(dockingStationBlockState("controller", "11"));
        palette.add(dockingStationBlockState("left_top", "11"));

        CompoundTag hopper = blockState("minecraft:hopper");
        CompoundTag hopperProperties = new CompoundTag();
        hopperProperties.putString("enabled", "true");
        hopperProperties.putString("facing", "south");
        hopper.put("Properties", hopperProperties);
        palette.add(hopper);

        CompoundTag chest = blockState("minecraft:chest");
        CompoundTag chestProperties = new CompoundTag();
        chestProperties.putString("facing", "north");
        chestProperties.putString("type", "single");
        chestProperties.putString("waterlogged", "false");
        chest.put("Properties", chestProperties);
        palette.add(chest);
        return palette;
    }

    private static ListTag tugLinkingPalette() {
        ListTag palette = new ListTag();
        palette.add(blockState("minecraft:polished_andesite"));
        palette.add(waterBlockState());
        return palette;
    }

    private static ListTag tugRoutingPalette() {
        ListTag palette = new ListTag();
        palette.add(blockState("minecraft:polished_andesite"));
        palette.add(waterBlockState());
        return palette;
    }

    private static ListTag tugDockingPalette() {
        ListTag palette = new ListTag();
        palette.add(blockState("minecraft:polished_andesite"));
        palette.add(waterBlockState());
        palette.add(dockingStationBlockState("controller", "14"));
        palette.add(dockingStationBlockState("left_top", "14"));
        palette.add(dockingStationBlockState("controller", "11"));
        palette.add(dockingStationBlockState("left_top", "11"));

        CompoundTag hopper = blockState("minecraft:hopper");
        CompoundTag hopperProperties = new CompoundTag();
        hopperProperties.putString("enabled", "true");
        hopperProperties.putString("facing", "south");
        hopper.put("Properties", hopperProperties);
        palette.add(hopper);

        CompoundTag chest = blockState("minecraft:chest");
        CompoundTag chestProperties = new CompoundTag();
        chestProperties.putString("facing", "north");
        chestProperties.putString("type", "single");
        chestProperties.putString("waterlogged", "false");
        chest.put("Properties", chestProperties);
        palette.add(chest);
        return palette;
    }

    private static ListTag blocks() {
        ListTag blocks = new ListTag();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 7; z++) {
                blocks.add(block(x, 0, z, 0));
            }
        }
        for (int z = 0; z < 7; z++) {
            blocks.add(block(2, 1, z, 1));
        }
        return blocks;
    }

    private static ListTag routingBlocks() {
        ListTag blocks = new ListTag();
        for (int x = 2; x <= 12; x++) {
            for (int z = 2; z <= 8; z++) {
                blocks.add(block(x, 0, z, 0));
            }
        }

        blocks.add(block(2, 1, 2, 3));
        blocks.add(block(8, 1, 2, 4));
        blocks.add(block(2, 1, 8, 5));
        blocks.add(block(8, 1, 8, 7));

        for (int x = 3; x < 8; x++) {
            blocks.add(block(x, 1, 2, 2));
        }
        for (int x : new int[] {3, 4, 5, 6, 7, 9, 10, 11, 12}) {
            blocks.add(block(x, 1, 8, 2));
        }
        for (int z = 3; z < 8; z++) {
            blocks.add(block(2, 1, z, 1));
            blocks.add(block(8, 1, z, 1));
        }
        return blocks;
    }

    private static ListTag dockingBlocks() {
        ListTag blocks = new ListTag();
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 7; z++) {
                blocks.add(block(x, 0, z, 0));
            }
            blocks.add(block(x, 1, 3, 1));
        }

        for (int x : new int[] {6, 7}) {
            int paletteOffset = x == 6 ? 2 : 0;
            blocks.add(block(x, 1, 2, 2 + paletteOffset));
            blocks.add(block(x, 2, 2, 3 + paletteOffset));
        }

        blocks.add(block(6, 1, 1, 6));
        blocks.add(block(6, 2, 1, 7));
        return blocks;
    }

    private static ListTag tugLinkingBlocks() {
        ListTag blocks = new ListTag();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 8; z++) {
                blocks.add(block(x, 0, z, 0));
                blocks.add(block(x, 1, z, 1));
            }
        }
        return blocks;
    }

    private static ListTag tugRoutingBlocks() {
        ListTag blocks = new ListTag();
        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 9; z++) {
                blocks.add(block(x, 0, z, 0));
                boolean island = x >= 4 && x <= 8 && z >= 3 && z <= 5;
                blocks.add(block(x, 1, z, island ? 0 : 1));
            }
        }
        return blocks;
    }

    private static ListTag tugDockingBlocks() {
        ListTag blocks = new ListTag();
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 6; z++) {
                blocks.add(block(x, 0, z, 0));
            }

            for (int z = 0; z <= 2; z++) {
                blocks.add(block(x, 1, z, 0));
            }

            for (int z = 3; z < 6; z++) {
                blocks.add(block(x, 1, z, 1));
            }
        }

        for (int x : new int[] {6, 7}) {
            int paletteOffset = x == 6 ? 2 : 0;
            blocks.add(block(x, 2, 2, 2 + paletteOffset));
            blocks.add(block(x, 3, 2, 3 + paletteOffset));
        }

        blocks.add(block(6, 2, 1, 6));
        blocks.add(block(6, 3, 1, 7));
        return blocks;
    }

    private static CompoundTag block(int x, int y, int z, int state) {
        CompoundTag block = new CompoundTag();
        block.put("pos", integerList(x, y, z));
        block.putInt("state", state);
        return block;
    }

    private static CompoundTag blockState(String name) {
        CompoundTag blockState = new CompoundTag();
        blockState.put("Name", StringTag.valueOf(name));
        return blockState;
    }

    private static CompoundTag railBlockState(String shape) {
        CompoundTag rail = blockState("minecraft:rail");
        CompoundTag properties = new CompoundTag();
        properties.putString("shape", shape);
        rail.put("Properties", properties);
        return rail;
    }

    private static CompoundTag waterBlockState() {
        CompoundTag water = blockState("minecraft:water");
        CompoundTag properties = new CompoundTag();
        properties.putString("level", "0");
        water.put("Properties", properties);
        return water;
    }

    private static CompoundTag automaticTeeJunctionBlockState() {
        CompoundTag junction = blockState("littlelogistics:automatic_tee_junction_rail");
        CompoundTag properties = new CompoundTag();
        properties.putString("facing", "south");
        properties.putString("powered", "false");
        properties.putString("shape", "north_south");
        properties.putString("waterlogged", "false");
        junction.put("Properties", properties);
        return junction;
    }

    private static CompoundTag dockingStationBlockState(String part, String color) {
        CompoundTag dockingStation = blockState("littlelogistics:docking_station");
        CompoundTag properties = new CompoundTag();
        properties.putString("color", color);
        properties.putString("facing", "north");
        properties.putString("part", part);
        dockingStation.put("Properties", properties);
        return dockingStation;
    }

    private static ListTag integerList(int... values) {
        ListTag list = new ListTag();
        for (int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }
}
