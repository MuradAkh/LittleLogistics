package ca.edtoaster.littlecontraptions.setup;

import ca.edtoaster.littlecontraptions.block.BargeAssemblerBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class LCBlockEntityTypes {
    // create — registered on Little Contraptions' own DeferredRegister so the block entity
    // shares the littlecontraptions namespace with its block.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BargeAssemblerBlockEntity>> BARGE_ASSEMBLER = register(
            "barge_assembler",
            BargeAssemblerBlockEntity::new,
            LCBlocks.BARGE_ASSEMBLER
    );

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            DeferredHolder<Block, ? extends Block> block) {
        return Registration.TILE_ENTITIES.register(name, () ->
                BlockEntityType.Builder.of(factory, block.get()).build(null));
    }

    public static void register () {

    }
}
