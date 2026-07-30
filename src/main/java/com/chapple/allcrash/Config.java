package com.chapple.allcrash;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> crash_mod;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        crash_mod = builder
                .comment("include these mod will crash")
                .defineList("crash_mod", List.of("examplemod"), String.class::isInstance);
        SPEC = builder.build();
    }
}