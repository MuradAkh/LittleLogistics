package dev.murad.shipping;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ShippingConfig {
    public static class Common {
        public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        public static final ModConfigSpec SPEC;

        public static final ModConfigSpec.ConfigValue<Boolean> CREATE_COMPAT;

        static {
            BUILDER.push("compat").comment("Additional compatibility features for third-party mods, disable if broken by a third-party mod update.");
            CREATE_COMPAT = BUILDER.define("create", true);
            BUILDER.pop();
            SPEC = BUILDER.build();
        }


    }

    public static class Client {
        public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        public static final ModConfigSpec SPEC;

        public static final ModConfigSpec.ConfigValue<Double> TUG_SMOKE_MODIFIER;
        public static final ModConfigSpec.ConfigValue<Double> LOCO_SMOKE_MODIFIER;
        public static final ModConfigSpec.ConfigValue<Boolean> DISABLE_ROUTE_MARKERS;
        public static final ModConfigSpec.ConfigValue<Boolean> SHOW_WRENCH_TUG_ROUTES;
        public static final ModConfigSpec.ConfigValue<Boolean> SHOW_WRENCH_LOCO_ROUTES;

        static {
            BUILDER.push("general");
            TUG_SMOKE_MODIFIER =
                    BUILDER.comment("Modify the rate of smoke produced by a tug. Min 0, Max 1, Default 0.4")
                            .defineInRange("tugSmoke", 0.4, 0, 1);

            LOCO_SMOKE_MODIFIER =
                    BUILDER.comment("Modify the rate of smoke produced by a locomotive. Min 0, Max 1, Default 0.2")
                            .defineInRange("locomotiveSmoke", 0.2, 0, 1);

            DISABLE_ROUTE_MARKERS =
                    BUILDER.comment("Disable in-world route waypoint markers when holding a route item. Default false.")
                            .define("disableRouteMarkers", false);

            SHOW_WRENCH_TUG_ROUTES =
                    BUILDER.comment("Show nearby registered tug routes while holding the Conductor's Wrench. Default true.")
                            .define("showWrenchTugRoutes", true);

            SHOW_WRENCH_LOCO_ROUTES =
                    BUILDER.comment("Show nearby registered locomotive routes while holding the Conductor's Wrench. Default true.")
                            .define("showWrenchLocoRoutes", true);
            BUILDER.pop();

            SPEC = BUILDER.build();
        }
    }

    public static class Server {
        public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        public static final ModConfigSpec SPEC;
        public static final ModConfigSpec.ConfigValue<Double> FISHING_TREASURE_CHANCE_MODIFIER;
        public static final ModConfigSpec.ConfigValue<String> FISHING_LOOT_TABLE;
        public static final ModConfigSpec.ConfigValue<Integer> FISHING_COOLDOWN;

        public static final ModConfigSpec.ConfigValue<Double> TUG_BASE_SPEED;

        public static final ModConfigSpec.ConfigValue<Double> STEAM_TUG_FUEL_MULTIPLIER;

        public static final ModConfigSpec.ConfigValue<Integer> TUG_PATHFINDING_MULTIPLIER;
        public static final ModConfigSpec.ConfigValue<Integer> TUG_ROUTE_MAX_SEGMENT_LENGTH;
        public static final ModConfigSpec.ConfigValue<Integer> ENERGY_TUG_BASE_CAPACITY;
        public static final ModConfigSpec.ConfigValue<Integer> ENERGY_TUG_BASE_ENERGY_USAGE;
        public static final ModConfigSpec.ConfigValue<Integer> ENERGY_TUG_BASE_ENERGY_USAGE_INTERVAL;
        public static final ModConfigSpec.ConfigValue<Integer> ENERGY_TUG_BASE_MAX_CHARGE_RATE;

        public static final ModConfigSpec.ConfigValue<Double> TRAIN_MAX_SPEED;
        public static final ModConfigSpec.ConfigValue<Double> LOCO_BASE_SPEED;

        public static final ModConfigSpec.ConfigValue<Double> STEAM_LOCO_FUEL_MULTIPLIER;
        public static final ModConfigSpec.ConfigValue<Integer> ENERGY_LOCO_BASE_CAPACITY;
        public static final ModConfigSpec.ConfigValue<Integer> ENERGY_LOCO_BASE_ENERGY_USAGE;
        public static final ModConfigSpec.ConfigValue<Integer> ENERGY_LOCO_BASE_ENERGY_USAGE_INTERVAL;
        public static final ModConfigSpec.ConfigValue<Integer> ENERGY_LOCO_BASE_MAX_CHARGE_RATE;

        public static final ModConfigSpec.ConfigValue<List<? extends String>> TRAIN_EXEMPT_DAMAGE_SOURCES;
        public static final ModConfigSpec.ConfigValue<List<? extends String>> VESSEL_EXEMPT_DAMAGE_SOURCES;


        public static final ModConfigSpec.ConfigValue<Integer> CHUNK_LOADING_LEVEL;
        public static final ModConfigSpec.ConfigValue<Boolean> MANAGED_VEHICLE_LOADING;
        public static final ModConfigSpec.ConfigValue<Boolean> OFFLINE_LOADING;
        public static final ModConfigSpec.ConfigValue<Integer> ROUTE_LOOKAHEAD_SECONDS;
        public static final ModConfigSpec.ConfigValue<Integer> CHUNK_RELEASE_GRACE_TICKS;


        static {
            BUILDER.push("managed vehicle loading - requires restart");
            BUILDER.comment("Tugs and locomotives placed by players are owned automatically. Active vehicles can load their consist footprint and a forward window of their compiled route. At level 0 only Little Logistics entities are ticked manually.");

            CHUNK_LOADING_LEVEL = BUILDER.comment("Chunkloading level, from low perf impact to high. 0: no ticking (except LL, recommended), 1: tile entity ticking, 2: entity ticking (regular).")
                            .defineInRange("chunkLoadingLevel", 0, 0, 2);

            MANAGED_VEHICLE_LOADING = BUILDER.comment("Allow automatically owned tugs and locomotives to load the chunks required by their active compiled route.")
                    .define("managedVehicleLoading", true);

            OFFLINE_LOADING = BUILDER.comment("Load vehicles even when the player is offline")
                    .define("offlineLoading", false);

            ROUTE_LOOKAHEAD_SECONDS = BUILDER.comment("Approximate maximum-speed travel time to preload along an active compiled route.")
                    .defineInRange("routeLookAheadSeconds", 8, 1, 60);

            CHUNK_RELEASE_GRACE_TICKS = BUILDER.comment("Ticks to retain route chunks after they leave a vehicle's required window.")
                    .defineInRange("chunkReleaseGraceTicks", 40, 0, 1200);

            BUILDER.pop();
            BUILDER.push("vessel");
            {
                BUILDER.push("general");
                VESSEL_EXEMPT_DAMAGE_SOURCES = BUILDER.comment("Damage sources that vessels are invulnerable to")
                        .defineList("vesselInvuln", List.of("create.mechanical_saw", "create.mechanical_drill"), s -> true);

                BUILDER.pop();
            }
            {
                BUILDER.push("barge");
                FISHING_TREASURE_CHANCE_MODIFIER =
                        BUILDER.comment("Modify the chance of using the treasure loot table with the auto fishing barge, other factors such as depth and overfishing still play a role. " +
                                        "Default 0.04.")
                                .define("fishingTreasureChance", 0.04);
                FISHING_LOOT_TABLE =
                        BUILDER.comment("Loot table to use when fishing barge catches a fish. Change to 'minecraft:gameplay/fishing' if some modded fish aren't being caught. Defaults to 'minecraft:gameplay/fishing/fish'.")
                                .define("fishingLootTable", "minecraft:gameplay/fishing/fish");

                FISHING_COOLDOWN =
                        BUILDER.comment("Cooldown before each fishing attempt")
                                .defineInRange("fishingCooldown", 40, 0, 200000);

                BUILDER.pop();
            }
            {
                BUILDER.push("tug");
                TUG_BASE_SPEED =
                        BUILDER.comment("Base speed of the tugs. Default 2.4.")
                                .defineInRange("tugBaseSpeed", 2.4, 0.1, 10);

                TUG_PATHFINDING_MULTIPLIER =
                        BUILDER.comment("Multiplier for tug pathfinding search space, high values may impact performance. Default 1.")
                                .defineInRange("tugPathfindMult", 1, 1, 10);

                TUG_ROUTE_MAX_SEGMENT_LENGTH =
                        BUILDER.comment("Maximum Euclidean distance, in blocks, between two tug route waypoints. Default 96.")
                                .defineInRange("tugRouteMaxSegmentLength", 96, 4, 1024);

                STEAM_TUG_FUEL_MULTIPLIER =
                        BUILDER.comment("Increases the burn duration of Steam tug fuel by N times when compared to furnace, must be >= 0.01. Default 4.0.")
                                .defineInRange("steamTugFuelMultiplier", 4.0, 0.01, Double.MAX_VALUE);

                ENERGY_TUG_BASE_CAPACITY =
                        BUILDER.comment("Base maximum capacity of the Energy tug in FE, must be an integer >= 1. Default 10000.")
                                .defineInRange("energyTugBaseCapacity", 10000, 1, Integer.MAX_VALUE);
                ENERGY_TUG_BASE_ENERGY_USAGE =
                        BUILDER.comment("Base energy usage of the Energy tug in FE per drain, must be an integer >= 1. Default 1.")
                                .defineInRange("energyTugBaseEnergyUsage", 1, 1, Integer.MAX_VALUE);
                ENERGY_TUG_BASE_ENERGY_USAGE_INTERVAL =
                        BUILDER.comment("How many active ticks between each energy drain of the Energy tug, must be an integer >= 1. E.g. 4 means drain energyTugBaseEnergyUsage FE once every 4 ticks. Default 4.")
                                .defineInRange("energyTugBaseEnergyUsageInterval", 4, 1, Integer.MAX_VALUE);
                ENERGY_TUG_BASE_MAX_CHARGE_RATE =
                        BUILDER.comment("Base max charge rate of the Energy tug in FE/tick, must be an integer >= 1. Default 100.")
                                .defineInRange("energyTugBaseMaxChargeRate", 100, 1, Integer.MAX_VALUE);
                BUILDER.pop();
            }
            BUILDER.pop();
            BUILDER.push("train");
            {
                BUILDER.push("general");
                TRAIN_MAX_SPEED =
                        BUILDER.comment("Max speed that trains can be accelerated to. High speed may cause chunk loading lag or issues, not advised for servers or packs. Default 0.6, max is 1")
                                .defineInRange("trainMaxSpeed", 0.6, 0.01, 1);

                TRAIN_EXEMPT_DAMAGE_SOURCES = BUILDER.comment("Damage sources that trains are invulnerable to")
                                .defineList("trainInvuln", List.of("create.mechanical_saw", "create.mechanical_drill"), s -> true);



                BUILDER.pop();
            }
            {
                BUILDER.push("locomotive");
                LOCO_BASE_SPEED =
                        BUILDER.comment("Locomotive base speed. High speed may cause chunk loading lag or issues, not advised for servers or packs. Default 0.5, max is 0.9")
                                .defineInRange("locoBaseSpeed", 0.5, 0.01, 0.9);

                STEAM_LOCO_FUEL_MULTIPLIER =
                        BUILDER.comment("Increases the burn duration of Steam locomotive fuel by N times when compared to furnace, must be >= 0.01. Default 4.0.")
                                .defineInRange("steamLocoFuelMultiplier", 4.0, 0.01, Double.MAX_VALUE);

                ENERGY_LOCO_BASE_CAPACITY =
                        BUILDER.comment("Base maximum capacity of the Energy locomotive in FE, must be an integer >= 1. Default 10000.")
                                .defineInRange("energyLocoBaseCapacity", 10000, 1, Integer.MAX_VALUE);
                ENERGY_LOCO_BASE_ENERGY_USAGE =
                        BUILDER.comment("Base energy usage of the Energy locomotive in FE per drain, must be an integer >= 1. Default 1.")
                                .defineInRange("energyLocoBaseEnergyUsage", 1, 1, Integer.MAX_VALUE);
                ENERGY_LOCO_BASE_ENERGY_USAGE_INTERVAL =
                        BUILDER.comment("How many active ticks between each energy drain of the Energy locomotive, must be an integer >= 1. E.g. 4 means drain energyLocoBaseEnergyUsage FE once every 4 ticks. Default 4.")
                                .defineInRange("energyLocoBaseEnergyUsageInterval", 4, 1, Integer.MAX_VALUE);
                ENERGY_LOCO_BASE_MAX_CHARGE_RATE =
                        BUILDER.comment("Base max charge rate of the Energy locomotive in FE/tick, must be an integer >= 1. Default 100.")
                                .defineInRange("energyLocoBaseMaxChargeRate", 100, 1, Integer.MAX_VALUE);BUILDER.pop();
            }
            BUILDER.pop();
            SPEC = BUILDER.build();
        }
    }



}
