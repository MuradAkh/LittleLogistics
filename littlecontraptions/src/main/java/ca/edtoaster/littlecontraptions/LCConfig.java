package ca.edtoaster.littlecontraptions;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * TODO: remove configs
 */
public class LCConfig {
    public static class Client {
        public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        public static final ModConfigSpec SPEC;

        static {
            SPEC = BUILDER.build();
        }
    }

    public static class Server {
        public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        public static final ModConfigSpec SPEC;

        static {
            SPEC = BUILDER.build();
        }
    }



}
