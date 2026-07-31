package ca.edtoaster.littlecontraptions;

import ca.edtoaster.littlecontraptions.setup.Registration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(LCMod.MOD_ID)
public class LCMod
{
    public static final String MOD_ID = "littlecontraptions";
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    // NeoForge 1.21.1: the mod ctor receives the mod event bus + ModContainer directly,
    // replacing the no-arg ctor + FMLJavaModLoadingContext.get().getModEventBus().
    public LCMod(IEventBus modEventBus, ModContainer modContainer) {
        Registration.register(modEventBus);
    }
}
