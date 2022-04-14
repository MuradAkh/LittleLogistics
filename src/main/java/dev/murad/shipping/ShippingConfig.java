package dev.murad.shipping;

import net.minecraftforge.common.ForgeConfigSpec;

public class ShippingConfig {
    public static class Client {
        public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec SPEC;

        public static final ForgeConfigSpec.ConfigValue<Double> TUG_SMOKE_MODIFIER;
        public static final ForgeConfigSpec.ConfigValue<Double> LOCO_SMOKE_MODIFIER;
        public static final ForgeConfigSpec.ConfigValue<Boolean> DISABLE_TUG_ROUTE_BEACONS;

        static {
            BUILDER.push("general");
            TUG_SMOKE_MODIFIER =
                    BUILDER.comment("Modify the rate of smoke produced by a tug. Min 0, Max 1, Default 0.4")
                            .defineInRange("tugSmoke", 0.4, 0, 1);

            LOCO_SMOKE_MODIFIER =
                    BUILDER.comment("Modify the rate of smoke produced by a locomotive. Min 0, Max 1, Default 0.2")
                            .defineInRange("locomotiveSmoke", 0.2, 0, 1);

            DISABLE_TUG_ROUTE_BEACONS =
                    BUILDER.comment("Disable indicator beacons for tug route item. Default false.")
                            .define("disableTugRouteBeacons", false);
            BUILDER.pop();
            SPEC = BUILDER.build();
        }
    }

    public static class Server {
        public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec SPEC;
        public static final ForgeConfigSpec.ConfigValue<Double> FISHING_TREASURE_CHANCE_MODIFIER;
        public static final ForgeConfigSpec.ConfigValue<String> FISHING_LOOT_TABLE;

        public static final ForgeConfigSpec.ConfigValue<Double> TUG_BASE_SPEED;

        public static final ForgeConfigSpec.ConfigValue<Integer> STEAM_TUG_FUEL_MULTIPLIER;

        public static final ForgeConfigSpec.ConfigValue<Integer> TUG_PATHFINDING_MULTIPLIER;
        public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_TUG_BASE_CAPACITY;
        public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_TUG_BASE_ENERGY_USAGE;
        public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_TUG_BASE_MAX_CHARGE_RATE;

        public static final ForgeConfigSpec.ConfigValue<Double> TRAIN_MAX_SPEED;
        public static final ForgeConfigSpec.ConfigValue<Double> LOCO_BASE_SPEED;

        public static final ForgeConfigSpec.ConfigValue<Integer> STEAM_LOCO_FUEL_MULTIPLIER;
        public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_LOCO_BASE_CAPACITY;
        public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_LOCO_BASE_ENERGY_USAGE;
        public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_LOCO_BASE_MAX_CHARGE_RATE;

        public static final ForgeConfigSpec.ConfigValue<Integer> VESSEL_CHARGER_BASE_CAPACITY;
        public static final ForgeConfigSpec.ConfigValue<Integer> VESSEL_CHARGER_BASE_MAX_TRANSFER;



        static {
            BUILDER.push("vessel");
            {
                BUILDER.push("barge");
                FISHING_TREASURE_CHANCE_MODIFIER =
                        BUILDER.comment("Modify the chance of using the treasure loot table with the auto fishing barge, other factors such as depth and overfishing still play a role. " +
                                        "Default 0.02.")
                                .define("fishingTreasureChance", 0.02);
                FISHING_LOOT_TABLE =
                        BUILDER.comment("Loot table to use when fishing barge catches a fish. Change to 'minecraft:gameplay/fishing' if some modded fish aren't being caught. Defaults to 'minecraft:gameplay/fishing/fish'.")
                                .define("fishingLootTable", "minecraft:gameplay/fishing/fish");

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

                STEAM_TUG_FUEL_MULTIPLIER =
                        BUILDER.comment("Increases the burn duration of Steam tug fuel by N times when compared to furnace, must be an integer >= 1. Default 4.")
                                .defineInRange("steamTugFuelMultiplier", 4, 1, Integer.MAX_VALUE);

                ENERGY_TUG_BASE_CAPACITY =
                        BUILDER.comment("Base maximum capacity of the Energy tug in FE, must be an integer >= 1. Default 10000.")
                                .defineInRange("energyTugBaseCapacity", 10000, 1, Integer.MAX_VALUE);
                ENERGY_TUG_BASE_ENERGY_USAGE =
                        BUILDER.comment("Base energy usage of the Energy tug in FE/tick, must be an integer >= 1. Default 1.")
                                .defineInRange("energyTugBaseEnergyUsage", 1, 1, Integer.MAX_VALUE);
                ENERGY_TUG_BASE_MAX_CHARGE_RATE =
                        BUILDER.comment("Base max charge rate of the Energy tug in FE/tick, must be an integer >= 1. Default 100.")
                                .defineInRange("energyTugBaseMaxChargeRate", 100, 1, Integer.MAX_VALUE);
                BUILDER.pop();
            }
            BUILDER.pop();
            BUILDER.push("dock");
            {
                BUILDER.push("charger");
                VESSEL_CHARGER_BASE_CAPACITY =
                        BUILDER.comment("Base max capacity of the Vessel Charger in FE, must be an integer >= 1. Default 10000.")
                                .defineInRange("vesselChargerBaseCapacity", 10000, 1, Integer.MAX_VALUE);
                VESSEL_CHARGER_BASE_MAX_TRANSFER =
                        BUILDER.comment("Base max transfer rate of the Vessel Charger in FE/tick, must be an integer >= 1. Default 100.")
                                .defineInRange("vesselChargerBaseMaxTransfer", 100, 1, Integer.MAX_VALUE);
                BUILDER.pop();
            }
            BUILDER.pop();
            BUILDER.push("train");
            {
                BUILDER.push("general");
                TRAIN_MAX_SPEED =
                        BUILDER.comment("Max speed that trains can be accelerated to. High speed may cause chunk loading lag or issues, not advised for servers or packs. Default 0.25, max is 1")
                                .defineInRange("trainMaxSpeed", 0.25, 0.01, 1);
                BUILDER.pop();
            }
            {
                BUILDER.push("locomotive");
                LOCO_BASE_SPEED =
                        BUILDER.comment("Locomotive base speed. High speed may cause chunk loading lag or issues, not advised for servers or packs. Default 0.2, max is 0.9")
                                .defineInRange("locoBaseSpeed", 0.2, 0.01, 0.9);

                STEAM_LOCO_FUEL_MULTIPLIER =
                        BUILDER.comment("Increases the burn duration of Steam locomotive fuel by N times when compared to furnace, must be an integer >= 1. Default 4.")
                                .defineInRange("steamLocoFuelMultiplier", 4, 1, Integer.MAX_VALUE);

                ENERGY_LOCO_BASE_CAPACITY =
                        BUILDER.comment("Base maximum capacity of the Energy locomotive in FE, must be an integer >= 1. Default 10000.")
                                .defineInRange("energyLocoBaseCapacity", 10000, 1, Integer.MAX_VALUE);
                ENERGY_LOCO_BASE_ENERGY_USAGE =
                        BUILDER.comment("Base energy usage of the Energy locomotive in FE/tick, must be an integer >= 1. Default 1.")
                                .defineInRange("energyLocoBaseEnergyUsage", 1, 1, Integer.MAX_VALUE);
                ENERGY_LOCO_BASE_MAX_CHARGE_RATE =
                        BUILDER.comment("Base max charge rate of the Energy locomotive in FE/tick, must be an integer >= 1. Default 100.")
                                .defineInRange("energyLocoBaseMaxChargeRate", 100, 1, Integer.MAX_VALUE);BUILDER.pop();
            }
            BUILDER.pop();
            SPEC = BUILDER.build();
        }
    }



}
