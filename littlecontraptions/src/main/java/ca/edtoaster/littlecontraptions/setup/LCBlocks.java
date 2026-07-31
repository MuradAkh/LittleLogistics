package ca.edtoaster.littlecontraptions.setup;

import ca.edtoaster.littlecontraptions.block.BargeAssemblerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

public class LCBlocks {

    public static final DeferredHolder<Block, BargeAssemblerBlock> BARGE_ASSEMBLER = Registration.BLOCKS
                    .register("barge_assembler",
                            () -> new BargeAssemblerBlock(BlockBehaviour.Properties
                                    .ofFullCopy(Blocks.IRON_BLOCK)
                                    .noCollission()
                                    .destroyTime(0.5f)));

    public static void register () {}
}
