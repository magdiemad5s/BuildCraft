package buildcraft.energy;

import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import buildcraft.api.core.BCLog;
import buildcraft.api.mj.MjAPI;
import buildcraft.neo.energy.EnergyConversionPolicy;
import buildcraft.energy.generation.features.OilGenerator;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class BCEnergyConfig {
    public static final double DEFAULT_SMALL_OIL_GEN_PROB = 2.0;
    public static final double DEFAULT_MEDIUM_OIL_GEN_PROB = 0.1;
    public static final double DEFAULT_LARGE_OIL_GEN_PROB = 0.04;
    public static final int DEFAULT_SMALL_SPOUT_MIN_HEIGHT = 6;
    public static final int DEFAULT_SMALL_SPOUT_MAX_HEIGHT = 12;
    public static final int DEFAULT_LARGE_SPOUT_MIN_HEIGHT = 10;
    public static final int DEFAULT_LARGE_SPOUT_MAX_HEIGHT = 20;

    public static ForgeConfigSpec config;

    public static boolean enableOilOceanBiome;
    public static boolean enableOilDesertBiome;
    public static boolean enableOilGeneration;
    public static double oilWellGenerationRate;
    public static boolean enableOilSpouts;
    public static boolean enableOilBurn;
    public static boolean oilIsSticky;
    public static int smallSpoutMinHeight;
    public static int smallSpoutMaxHeight;
    public static int largeSpoutMinHeight;
    public static int largeSpoutMaxHeight;
    public static double smallOilGenProb;
    public static double mediumOilGenProb;
    public static double largeOilGenProb;
    /** Micro-MJ produced for each accepted FE; retains the legacy general.mjPerRf contract. */
    public static volatile long microMjPerForgeEnergy = EnergyConversionPolicy.DEFAULT_MICRO_MJ_PER_FE;
    public static boolean excludedDimensionsIsBlackList;
    public static final Set<ResourceLocation> excessiveBiomes = new HashSet<>();
    public static final Set<ResourceLocation> excessiveVanillaBiomes = new HashSet<>();
    public static final Set<ResourceLocation> surfaceDepositBiomes = new HashSet<>();
    public static final Set<ResourceLocation> excludedBiomes = new HashSet<>();
    public static final Set<ResourceLocation> excludedDimensions = new HashSet<>();
    public static boolean excludedBiomesIsBlackList;
    public static SpecialEventType christmasEventStatus = SpecialEventType.DAY_ONLY;

    private static BooleanValue propEnableOilOceanBiome;
    private static BooleanValue propEnableOilDesertBiome;
    private static BooleanValue propEnableOilGeneration;
    private static DoubleValue propOilWellGenerationRate;
    private static BooleanValue propEnableOilSpouts;
    private static BooleanValue propEnableOilBurn;
    private static BooleanValue propOilIsSticky;
    private static IntValue propSmallSpoutMinHeight;
    private static IntValue propSmallSpoutMaxHeight;
    private static IntValue propLargeSpoutMinHeight;
    private static IntValue propLargeSpoutMaxHeight;
    private static DoubleValue propSmallOilGenProb;
    private static DoubleValue propMediumOilGenProb;
    private static DoubleValue propLargeOilGenProb;
    private static DoubleValue propMjPerRf;
    private static ConfigValue<String> propExcessiveBiomes;
    private static ConfigValue<String> propSurfaceDepositBiomes;
    private static ConfigValue<String> propExcludedBiomes;
    private static ConfigValue<String> propExcludedDimensions;
    private static BooleanValue propExcludedBiomesIsBlacklist;
    private static BooleanValue propExcludedDimensionsIsBlacklist;

    private BCEnergyConfig() {
    }

    public static void registry(IEventBus modEventBus) {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("worldgen.oil");
        propEnableOilOceanBiome = builder.comment("Should oil be especially common in deep oceans?")
            .define("oil_ocean_biome", true);
        propEnableOilDesertBiome = builder.comment("Should oil be especially common in deserts?")
            .define("oil_desert_biome", true);
        propEnableOilGeneration = builder.comment("Should oil deposits be generated?")
            .worldRestart().define("enable", true);
        propEnableOilBurn = builder.comment("Can oil and fuel blocks burn?")
            .define("can_burn", true);
        propOilIsSticky = builder.comment("Should heavy oil slow entities?")
            .define("oilIsDense", false);
        propOilWellGenerationRate = builder.comment("Global multiplier for oil-well occurrence.")
            .defineInRange("generationRate", 1.0, 0.0, 100.0);
        propExcessiveBiomes = builder.comment("Comma-separated biome IDs with greatly increased oil generation.")
            .define("excessiveBiomes", "minecraft:desert,minecraft:deep_ocean,minecraft:deep_cold_ocean");
        propSurfaceDepositBiomes = builder.comment(
            "Comma-separated biome IDs with three times the normal oil generation rate."
        ).define("surfaceDepositBiomes", "");
        propExcludedBiomes = builder.comment(
            "Comma-separated biome IDs used by excludedBiomesIsBlacklist."
        ).define(
            "excludedBiomes",
            "minecraft:the_end,minecraft:end_barrens,minecraft:end_highlands,minecraft:end_midlands,"
                + "minecraft:small_end_islands,minecraft:crimson_forest,minecraft:basalt_deltas,"
                + "minecraft:nether_wastes,minecraft:soul_sand_valley,minecraft:warped_forest,minecraft:the_void"
        );
        propExcludedBiomesIsBlacklist = builder.comment("Treat excluded biomes as a blacklist rather than a whitelist.")
            .define("excludedBiomesIsBlacklist", true);
        propExcludedDimensions = builder.comment(
            "Comma-separated dimension IDs used by excludedDimensionsIsBlacklist. Legacy -1, 0 and 1 IDs are accepted."
        ).define("excludedDimensions", "minecraft:the_nether,minecraft:the_end");
        propExcludedDimensionsIsBlacklist = builder.comment("Treat excluded dimensions as a blacklist rather than a whitelist.")
            .define("excludedDimensionsIsBlacklist", true);
        builder.pop();

        builder.push("worldgen.oil.spawn_probability");
        propSmallOilGenProb = builder.comment("Percentage probability of a small oil spawn.")
            .defineInRange("small", DEFAULT_SMALL_OIL_GEN_PROB, 0.0, 100.0);
        propMediumOilGenProb = builder.comment("Percentage probability of a medium oil spawn.")
            .defineInRange("medium", DEFAULT_MEDIUM_OIL_GEN_PROB, 0.0, 100.0);
        propLargeOilGenProb = builder.comment("Percentage probability of a large oil spawn.")
            .defineInRange("large", DEFAULT_LARGE_OIL_GEN_PROB, 0.0, 100.0);
        builder.pop();

        builder.push("worldgen.oil.spouts");
        propEnableOilSpouts = builder.comment("Whether oil spouts are generated.")
            .define("enable", true);
        propSmallSpoutMinHeight = builder.defineInRange(
            "small_min_height", DEFAULT_SMALL_SPOUT_MIN_HEIGHT, 0, 256
        );
        propSmallSpoutMaxHeight = builder.defineInRange(
            "small_max_height", DEFAULT_SMALL_SPOUT_MAX_HEIGHT, 0, 256
        );
        propLargeSpoutMinHeight = builder.defineInRange(
            "large_min_height", DEFAULT_LARGE_SPOUT_MIN_HEIGHT, 0, 256
        );
        propLargeSpoutMaxHeight = builder.defineInRange(
            "large_max_height", DEFAULT_LARGE_SPOUT_MAX_HEIGHT, 0, 256
        );
        builder.pop();

        builder.push("general");
        propMjPerRf = builder.comment(
            "MJ generated per accepted Forge Energy unit. The key name is retained from the legacy RF setting."
        ).defineInRange(
            "mjPerRf",
            EnergyConversionPolicy.DEFAULT_MICRO_MJ_PER_FE / (double) MjAPI.MJ,
            EnergyConversionPolicy.MIN_MICRO_MJ_PER_FE / (double) MjAPI.MJ,
            EnergyConversionPolicy.MAX_MICRO_MJ_PER_FE / (double) MjAPI.MJ
        );
        builder.pop();

        config = builder.build();
        modEventBus.register(BCEnergyConfig.class);
    }

    @SubscribeEvent
    public static void onReloadConfig(ModConfigEvent.Reloading event) {
        reloadConfig(event.getConfig().getModId());
    }

    @SubscribeEvent
    public static void onLoadConfig(ModConfigEvent.Loading event) {
        reloadConfig(event.getConfig().getModId());
    }

    static void reloadConfig(String modId) {
        if (!BCEnergy.MODID.equals(modId)) {
            return;
        }

        excessiveBiomes.clear();
        excessiveVanillaBiomes.clear();
        surfaceDepositBiomes.clear();
        excludedBiomes.clear();
        excludedDimensions.clear();
        addBiomeNames(propExcessiveBiomes.get(), excessiveBiomes);
        addBiomeNames(propSurfaceDepositBiomes.get(), surfaceDepositBiomes);
        addBiomeNames(propExcludedBiomes.get(), excludedBiomes);
        addDimensionNames(propExcludedDimensions.get(), excludedDimensions);
        excessiveVanillaBiomes.addAll(excessiveBiomes);

        excludedBiomesIsBlackList = propExcludedBiomesIsBlacklist.get();
        excludedDimensionsIsBlackList = propExcludedDimensionsIsBlacklist.get();
        enableOilOceanBiome = propEnableOilOceanBiome.get();
        enableOilDesertBiome = propEnableOilDesertBiome.get();
        enableOilGeneration = propEnableOilGeneration.get();
        oilWellGenerationRate = propOilWellGenerationRate.get();
        enableOilSpouts = propEnableOilSpouts.get();
        enableOilBurn = propEnableOilBurn.get();
        oilIsSticky = propOilIsSticky.get();
        smallSpoutMinHeight = propSmallSpoutMinHeight.get();
        smallSpoutMaxHeight = propSmallSpoutMaxHeight.get();
        largeSpoutMinHeight = propLargeSpoutMinHeight.get();
        largeSpoutMaxHeight = propLargeSpoutMaxHeight.get();
        smallOilGenProb = propSmallOilGenProb.get();
        mediumOilGenProb = propMediumOilGenProb.get();
        largeOilGenProb = propLargeOilGenProb.get();
        microMjPerForgeEnergy = Math.max(
            EnergyConversionPolicy.MIN_MICRO_MJ_PER_FE,
            Math.min(
                EnergyConversionPolicy.MAX_MICRO_MJ_PER_FE,
                Math.round(propMjPerRf.get() * MjAPI.MJ)
            )
        );
        OilGenerator.invalidateCache();
    }

    private static void addBiomeNames(String value, Set<ResourceLocation> destination) {
        for (String raw : value.split(",")) {
            String id = raw.trim();
            if (id.isEmpty()) {
                continue;
            }
            try {
                destination.add(new ResourceLocation(id));
            } catch (ResourceLocationException exception) {
                BCLog.logger.warn("Ignoring invalid biome id in BuildCraft Energy config: " + id);
            }
        }
    }

    private static void addDimensionNames(String value, Set<ResourceLocation> destination) {
        for (String raw : value.split(",")) {
            String id = raw.trim();
            if (id.isEmpty()) {
                continue;
            }
            if ("-1".equals(id)) {
                destination.add(new ResourceLocation("minecraft", "the_nether"));
                continue;
            }
            if ("0".equals(id)) {
                destination.add(new ResourceLocation("minecraft", "overworld"));
                continue;
            }
            if ("1".equals(id)) {
                destination.add(new ResourceLocation("minecraft", "the_end"));
                continue;
            }
            try {
                destination.add(new ResourceLocation(id));
            } catch (ResourceLocationException exception) {
                BCLog.logger.warn("Ignoring invalid dimension id in BuildCraft Energy config: " + id);
            }
        }
    }

    /** Dynamic biome registries are server-owned in 1.20.1, so validation is performed while parsing IDs. */
    public static void validateBiomeNames() {
    }

    public enum SpecialEventType {
        DISABLED,
        DAY_ONLY,
        MONTH,
        ENABLED;

        public final String lowerCaseName = name().toLowerCase(Locale.ROOT);

        public boolean isEnabled(MonthDay date) {
            if (this == DISABLED) return false;
            if (this == ENABLED) return true;
            LocalDateTime now = LocalDateTime.now();
            if (now.getMonth() != date.getMonth()) return false;
            if (this == MONTH) return true;
            int day = now.getDayOfMonth();
            int wanted = date.getDayOfMonth();
            return day >= wanted - 1 && day <= wanted + 1;
        }
    }
}
