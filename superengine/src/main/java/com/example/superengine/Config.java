package com.example.superengine;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue SUPER_MULTIPLIER = B
            .comment("Super Engine power compared to a normal Eureka engine (6 = six times stronger).")
            .defineInRange("superEngineMultiplier", 6.0, 1.0, 1000.0);

    public static final ForgeConfigSpec.DoubleValue MEGA_MULTIPLIER = B
            .comment("Mega Engine power compared to a normal Eureka engine.")
            .defineInRange("megaEngineMultiplier", 15.0, 1.0, 1000.0);

    public static final ForgeConfigSpec.DoubleValue GIGA_MULTIPLIER = B
            .comment("Giga Engine power compared to a normal Eureka engine.")
            .defineInRange("gigaEngineMultiplier", 40.0, 1.0, 1000.0);

    public static final ForgeConfigSpec SPEC = B.build();
}
