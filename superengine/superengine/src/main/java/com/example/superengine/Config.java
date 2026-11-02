package com.example.superengine;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue POWER_LINEAR = B
            .comment("Forward power of one Super Engine at full heat. Eureka's normal engine is 500000.")
            .defineInRange("powerLinear", 3000000.0, 0.0, 1.0E9);

    public static final ForgeConfigSpec.DoubleValue POWER_LINEAR_MIN = B
            .comment("Forward power of one Super Engine at minimal heat. Eureka's normal engine is 10000.")
            .defineInRange("powerLinearMin", 60000.0, 0.0, 1.0E9);

    public static final ForgeConfigSpec.DoubleValue POWER_ANGULAR = B
            .comment("Turning power of one Super Engine at full heat. Eureka's normal engine is 1.")
            .defineInRange("powerAngular", 1.0, 0.0, 1000.0);

    public static final ForgeConfigSpec SPEC = B.build();
}
