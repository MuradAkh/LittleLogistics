package dev.murad.shipping;

import net.minecraftforge.common.ForgeConfigSpec;

public class ShippingConfig {
    public static class Client {
        public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec SPEC;

        public static final ForgeConfigSpec.ConfigValue<Double> TUG_SMOKE_MODIFIER;
        public static final ForgeConfigSpec.ConfigValue<Boolean> DISABLE_TUG_ROUTE_BEACONS;

        static {
            BUILDER.push("general");
            TUG_SMOKE_MODIFIER =
                    BUILDER.comment("Modify the rate of smoke produced by a tug. Min 0, Max 1, Default 0.4")
                            .defineInRange("tugSmoke", 0.4, 0, 1);
            DISABLE_TUG_ROUTE_BEACONS =
                    BUILDER.comment("Disable indicator beacons for tug route item, for use with shaders. Default false.")
                            .define("disableTugRouteBeacons", false);
            BUILDER.pop();
            SPEC = BUILDER.build();
        }
    }

    public static class Server {
        public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec SPEC;
        public static final ForgeConfigSpec.ConfigValue<Double> FISHING_TREASURE_CHANCE_MODIFIER;

        public static final ForgeConfigSpec.ConfigValue<Double> TUG_BASE_SPEED;

        public static final ForgeConfigSpec.ConfigValue<Integer> STEAM_TUG_FUEL_MULTIPLIER;

        public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_TUG_BASE_CAPACITY;
        public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_TUG_BASE_ENERGY_USAGE;
        public static final ForgeConfigSpec.ConfigValue<Integer> ENERGY_TUG_BASE_MAX_CHARGE_RATE;

        public static final ForgeConfigSpec.ConfigValue<Integer> VESSEL_CHARGER_BASE_CAPACITY;
        public static final ForgeConfigSpec.ConfigValue<Integer> VESSEL_CHARGER_BASE_MAX_TRANSFER;



        static {
            BUILDER.push("vessel");
            {
                BUILDER.push("barge");
                FISHING_TREASURE_CHANCE_MODIFIER =
                        BUILDER.comment("Modify the chance of using the treasure loot table with the auto fishing barge, other factors such as depth and overfishing still play a role.")
                                .define("fishingTreasureChance", 0.02);
                BUILDER.pop();
            }
            {
                BUILDER.push("tug");
                TUG_BASE_SPEED =
                        BUILDER.comment("Base speed of the tugs. Default 2.4.")
                                .defineInRange("tugBaseSpeed", 2.4, 0.1, 10);

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
            SPEC = BUILDER.build();
        }
    }



}
