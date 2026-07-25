package buildcraft.energy.generation.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;
import buildcraft.core.BCCoreBlocks;
import buildcraft.energy.BCEnergyConfig;
import buildcraft.energy.generation.features.OilFeatureConfiguration.ExcessiveBiome;
import buildcraft.energy.generation.features.OilFeatureConfiguration.GenSetting;
import buildcraft.energy.generation.features.OilStructure.GenByPredicate;
import buildcraft.energy.generation.features.OilStructure.ReplaceType;
import buildcraft.lib.delta.SimplexNoise;
import buildcraft.lib.misc.RandUtil;
import buildcraft.lib.misc.VecUtil;
import buildcraft.lib.misc.data.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

public class OilGenerator {
    /** Random number, used to differentiate generators */
    private static final long MAGIC_GEN_NUMBER = 0xD0_46_B4_E4_0C_7D_07_CFL;

    public static final boolean DEBUG_OILGEN_BASIC = BCDebugging.shouldDebugLog("energy.oilgen");
    public static final boolean DEBUG_OILGEN_ALL = BCDebugging.shouldDebugComplex("energy.oilgen");

    private static final LoadingCache<Long, List<OilStructure>> structureCache
        = CacheBuilder.newBuilder().expireAfterAccess(20, TimeUnit.SECONDS).build(CacheLoader.from(OilGenerator::genCache));

    private static OilFeatureConfiguration config;

    static double xOffset = -1;
    static double zOffset = -1;

    private static WorldGenLevel level;

    public static int worldHeight = -1;//384
    public static int seaLevel = -1;//63
    public static int bottomY = -1;//-64

    public static synchronized void setConfiguration(OilFeatureConfiguration newConfig) {
        if (config != newConfig) {
            config = newConfig;
            structureCache.invalidateAll();
        }
    }

    public static synchronized void invalidateCache() {
        structureCache.invalidateAll();
    }

    private static List<OilStructure> genCache(long key){
        return getStructures(level, (int)(key&0xFFFFFFFFL), (int)((key>>32)&0xFFFFFFFFL), false);
    }

    public static synchronized List<OilStructure> getStructures(WorldGenLevel world, int cx, int cz) {
        if(level != world) {
            structureCache.invalidateAll();
            level = world;
            Random rand = new Random(world.getSeed());
            int OFFSET_RANGE = 500000;
            OilGenerator.xOffset = rand.nextInt(OFFSET_RANGE) - (OFFSET_RANGE / 2);
            OilGenerator.zOffset = rand.nextInt(OFFSET_RANGE) - (OFFSET_RANGE / 2);
            ServerLevel serverLevel = world.getLevel();
            DimensionType dimensionType = serverLevel.dimensionType();
            worldHeight = dimensionType.height();
            seaLevel = serverLevel.getSeaLevel();
            bottomY = dimensionType.minY();
        }
        return structureCache.getUnchecked((((long)(cz))<<32)|(cx&0xFFFFFFFFL));
    }

    /*this will not use the cache, only use for testing*/
    protected static List<OilStructure> getStructures(WorldGenLevel world, int cx, int cz, boolean log) {
        if (!BCEnergyConfig.enableOilGeneration || config == null) {
            return ImmutableList.of();
        }
        Random rand = RandUtil.createRandomForChunk(world, cx, cz, MAGIC_GEN_NUMBER);

        // shift to world coordinates
        int x = cx * 16 + 8 + rand.nextInt(16);
        int z = cz * 16 + 8 + rand.nextInt(16);



        Holder<Biome> biome = world.getBiome(new BlockPos(x, seaLevel, z));
        Optional<net.minecraft.resources.ResourceKey<Biome>> biomeKey = biome.unwrapKey();
        if (biomeKey.isEmpty()) {
            return ImmutableList.of();
        }
        ResourceLocation key = biomeKey.get().location();
//        if(!"buildcraftenergy:oil_desert".equals(key.location().toString())) {
//          BCLog.logger.debug("OilGenFeature:fail");
//          return ImmutableList.of();
//        }

        ResourceLocation dimension = world.getLevel().dimension().location();
        boolean dimensionAllowed = OilGenerationPolicy.isAllowedByList(
            BCEnergyConfig.excludedDimensions.contains(dimension),
            BCEnergyConfig.excludedDimensionsIsBlackList
        );
        boolean biomeAllowed = OilGenerationPolicy.isAllowedByList(
            BCEnergyConfig.excludedBiomes.contains(key),
            BCEnergyConfig.excludedBiomesIsBlackList
        );
        boolean excludedByData = config.excludedBiomes().contains(key);
        if (!dimensionAllowed || !biomeAllowed || excludedByData) {
            if (DEBUG_OILGEN_BASIC & log) {
                BCLog.logger.info(
                    "[energy.oilgen] Not generating oil in " + toStr(world) + " chunk " + cx + ", " + cz
                        + " because its dimension or biome (" + dimension + ", " + key + ") is disabled!"
                );
            }
            return ImmutableList.of();
        }

/*        if (isEndBiome(key) && (Math.abs(x) < 1200 || Math.abs(z) < 1200)) {
            if (DEBUG_OILGEN_BASIC & log) {
                BCLog.logger.info(
                    "[energy.oilgen] Not generating oil in " + toStr(world) + " chunk " + cx + ", " + cz
                        + " because it's the end biome and we're within 1200 blocks of the ender dragon fight"
                );
            }
            return ImmutableList.of();
        }*/


        boolean oilBiome = BCEnergyConfig.surfaceDepositBiomes.contains(key)
            || config.surfaceDepositBiomes().contains(key);
        boolean excessive = BCEnergyConfig.excessiveBiomes.contains(key)
            && OilGenerationPolicy.isLegacyExcessiveBiomeEnabled(
                key.getNamespace(),
                key.getPath(),
                BCEnergyConfig.enableOilDesertBiome,
                BCEnergyConfig.enableOilOceanBiome
            );
        boolean vanillaBiome = "minecraft".equals(key.getNamespace());
        if (!oilBiome && !excessive
            && ((vanillaBiome && !config.genOilInEveryVanillaBiomes())
                || (!vanillaBiome && !config.genOilInEveryModBiomes()))) {
            return ImmutableList.of();
        }

        double bonus = oilBiome ? 3.0 : 1.0;
        bonus *= OilGenerationPolicy.combineGenerationRates(
            config.oilWellGenerationRate(),
            BCEnergyConfig.oilWellGenerationRate
        );
/*        if (BCEnergyWorldGen.isTerraBlenderLoaded) {
            if (BCEnergyConfig.excessiveBiomes.contains(key))
                bonus *= 30.0;
            if (sampler == null) {
                ServerChunkCache serverchunkcache = serverlevel.getChunkSource();
                RandomState randomstate = serverchunkcache.randomState();
                sampler = randomstate.sampler();
            }
            DensityFunction.SinglePointContext densityfunction$singlepointcontext = new DensityFunction.SinglePointContext(x&0xFFFFFFFC, seaLevel&0xFFFFFFFC, z&0xFFFFFFFC);
            double compute = sampler.weirdness().compute(densityfunction$singlepointcontext);
            if(BCEnergyConfig.excessiveBiomes.contains(key) != compute > 0)
            BCLog.d("missmatch oil gen");

        }*/
        Optional<ExcessiveBiome> excessiveBiome = config.excessiveBiomes().stream()
            .filter((entry) -> entry.biome().equals(key))
            .findAny();
        if (excessive) {
            // Datapacks may provide a noise mask for configured excessive biomes.
            // A configured biome without a mask retains the legacy unconditional 30x bonus.
            if (excessiveBiome.isEmpty()) {
                bonus *= 30.0;
            } else {
                ExcessiveBiome excessiveBiome0 = excessiveBiome.get();
                double d0 = SimplexNoise.noise(
                    (x + xOffset) * excessiveBiome0.noiseScale(),
                    (z + zOffset) * excessiveBiome0.noiseScale()
                );
                if (d0 > excessiveBiome0.noiseThreshold()) {
                    bonus *= 30.0;
                    BCLog.d("gen many oil " + x + ", " + z);
                }
            }
        }
        GenSetting genSetting = config.genSetting();
        OilGenerationPolicy.DepositType type = OilGenerationPolicy.select(
            rand::nextDouble,
            bonus,
            OilGenerationPolicy.layerPercentage(
                genSetting.largeOilGenProb(),
                BCEnergyConfig.largeOilGenProb,
                BCEnergyConfig.DEFAULT_LARGE_OIL_GEN_PROB
            ),
            OilGenerationPolicy.layerPercentage(
                genSetting.mediumOilGenProb(),
                BCEnergyConfig.mediumOilGenProb,
                BCEnergyConfig.DEFAULT_MEDIUM_OIL_GEN_PROB
            ),
            OilGenerationPolicy.layerPercentage(
                genSetting.smallOilGenProb(),
                BCEnergyConfig.smallOilGenProb,
                BCEnergyConfig.DEFAULT_SMALL_OIL_GEN_PROB
            )
        );
        if (type == OilGenerationPolicy.DepositType.NONE) {
            if (DEBUG_OILGEN_ALL & log) {
                BCLog.logger.info(
                    "[energy.oilgen] Not generating oil in " + toStr(world) + " chunk " + cx + ", " + cz
                        + " because none of the random numbers were above the thresholds for generation"
                );
            }
            return ImmutableList.of();
        }
        if (DEBUG_OILGEN_BASIC && log) {
            BCLog.logger.info(
                "[energy.oilgen] Generating an oil well (" + type.name().toLowerCase(Locale.ROOT)
                    + ") in " + toStr(world) + " chunk " + cx + ", " + cz + " at " + x + ", " + z
            );
        }

        List<OilStructure> structures = new ArrayList<>();
        int lakeRadius;
        int tendrilRadius;
        if (type == OilGenerationPolicy.DepositType.LARGE) {
            lakeRadius = 4;
            tendrilRadius = 25 + rand.nextInt(20);
        } else if (type == OilGenerationPolicy.DepositType.LAKE) {
            lakeRadius = 6;
            tendrilRadius = 25 + rand.nextInt(20);
        } else {
            lakeRadius = 2;
            tendrilRadius = 5 + rand.nextInt(10);
        }
        structures.add(createTendril(new BlockPos(x, seaLevel -1, z), lakeRadius, tendrilRadius, rand));

        if (type != OilGenerationPolicy.DepositType.LAKE) {
            // Generate a spherical cave deposit
            int wellY = bottomY + 20 + rand.nextInt(10);

            int radius;
            if (type == OilGenerationPolicy.DepositType.LARGE) {
                radius = 8 + rand.nextInt(9);
            } else {
                radius = 4 + rand.nextInt(4);
            }

            structures.add(createSphere(new BlockPos(x, wellY, z), radius));

            // Generate a spout
            if (BCEnergyConfig.enableOilSpouts && genSetting.enableOilSpouts()) {
                int maxHeight, minHeight;

                if (type == OilGenerationPolicy.DepositType.LARGE) {
                    minHeight = OilGenerationPolicy.layerHeight(
                        genSetting.largeSpoutMinHeight(),
                        BCEnergyConfig.largeSpoutMinHeight,
                        BCEnergyConfig.DEFAULT_LARGE_SPOUT_MIN_HEIGHT
                    );
                    maxHeight = OilGenerationPolicy.layerHeight(
                        genSetting.largeSpoutMaxHeight(),
                        BCEnergyConfig.largeSpoutMaxHeight,
                        BCEnergyConfig.DEFAULT_LARGE_SPOUT_MAX_HEIGHT
                    );
                    radius = 1;
                } else {
                    minHeight = OilGenerationPolicy.layerHeight(
                        genSetting.smallSpoutMinHeight(),
                        BCEnergyConfig.smallSpoutMinHeight,
                        BCEnergyConfig.DEFAULT_SMALL_SPOUT_MIN_HEIGHT
                    );
                    maxHeight = OilGenerationPolicy.layerHeight(
                        genSetting.smallSpoutMaxHeight(),
                        BCEnergyConfig.smallSpoutMaxHeight,
                        BCEnergyConfig.DEFAULT_SMALL_SPOUT_MAX_HEIGHT
                    );
                    radius = 0;
                }
                final int height = OilGenerationPolicy.chooseInclusiveHeight(rand::nextInt, minHeight, maxHeight);
                structures.add(createSpout(new BlockPos(x, wellY, z), height, radius));
            }

            // Generate a spring at the bottom. The old 1.12 code assumed
            // minY == 0 and passed the absolute well Y as the tube length. With
            // negative world heights that mirrored the tube below the world.
            if (type == OilGenerationPolicy.DepositType.LARGE) {
                BlockPos springPos = new BlockPos(x, bottomY, z);
                BlockPos tubeStart = springPos.above();
                int tubeLength = Math.max(0, wellY - tubeStart.getY());
                // The spring connection is the released large-spout width even when visible
                // spouts are disabled. Reusing the reservoir radius here created an enormous
                // 17-33 block-wide shaft whenever worldgen.oil.spouts.enable was false.
                structures.add(createTube(
                    tubeStart,
                    tubeLength,
                    OilGenerationPolicy.springTubeRadius(type),
                    Axis.Y
                ));
                if (BCCoreBlocks.SPRING.isPresent()) {
                    structures.add(createSpring(springPos));
                }
            }
        }
        return structures;
    }

    private static String toStr(WorldGenLevel world) {
        return world.dimensionType().effectsLocation().toString();
    }

    private static OilStructure createSpout(BlockPos start, int height, int radius) {
        return new OilStructure.Spout(start, ReplaceType.ALWAYS, radius, height);
    }

    public static OilStructure createTubeY(BlockPos base, int height, int radius) {
        return createTube(base, height, radius, Axis.Y);
    }

    public static OilStructure createSpring(BlockPos at) {
        return new OilStructure.Spring(at);
    }

    public static OilStructure createTube(BlockPos center, int length, int radius, Axis axis) {
        int valForAxis = VecUtil.getValue(center, axis);
        BlockPos min = VecUtil.replaceValue(center.offset(-radius, -radius, -radius), axis, valForAxis);
        BlockPos max = VecUtil.replaceValue(center.offset(radius, radius, radius), axis, valForAxis + length);
        double radiusSq = radius * radius;
        int toReplace = valForAxis;
        Predicate<BlockPos> tester = p -> VecUtil.replaceValue(p, axis, toReplace).distSqr(center) <= radiusSq;
        return new GenByPredicate(new Box(min, max), ReplaceType.ALWAYS, tester);
    }

    public static OilStructure createSphere(BlockPos center, int radius) {
        Box box = new Box(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius));
        double radiusSq = radius * radius + 0.01;
        Predicate<BlockPos> tester = p -> p.distSqr(center) <= radiusSq;
        return new GenByPredicate(box, ReplaceType.ALWAYS, tester);
    }

    public static OilStructure createTendril(BlockPos center, int lakeRadius, int radius, Random rand) {
        BlockPos.MutableBlockPos start = center.mutable().move(-radius, 0, -radius);
        int diameter = radius * 2 + 1;
        boolean[][] pattern = new boolean[diameter][diameter];

        int x = radius;
        int z = radius;
        for (int dx = -lakeRadius; dx <= lakeRadius; dx++) {
            for (int dz = -lakeRadius; dz <= lakeRadius; dz++) {
                pattern[x + dx][z + dz] = dx * dx + dz * dz <= lakeRadius * lakeRadius;
            }
        }

        for (int w = 1; w < radius; w++) {
            float proba = (float) (radius - w + 4) / (float) (radius + 4);

            fillPatternIfProba(rand, proba, x, z + w, pattern);
            fillPatternIfProba(rand, proba, x, z - w, pattern);
            fillPatternIfProba(rand, proba, x + w, z, pattern);
            fillPatternIfProba(rand, proba, x - w, z, pattern);

            for (int i = 1; i <= w; i++) {
                fillPatternIfProba(rand, proba, x + i, z + w, pattern);
                fillPatternIfProba(rand, proba, x + i, z - w, pattern);
                fillPatternIfProba(rand, proba, x + w, z + i, pattern);
                fillPatternIfProba(rand, proba, x - w, z + i, pattern);

                fillPatternIfProba(rand, proba, x - i, z + w, pattern);
                fillPatternIfProba(rand, proba, x - i, z - w, pattern);
                fillPatternIfProba(rand, proba, x + w, z - i, pattern);
                fillPatternIfProba(rand, proba, x - w, z - i, pattern);
            }
        }

        int depth = rand.nextDouble() < 0.5 ? 1 : 2;
        return OilStructure.PatternTerrainHeight.create(start, ReplaceType.IS_FOR_LAKE, pattern, depth);
    }

    private static void fillPatternIfProba(Random rand, float proba, int x, int z, boolean[][] pattern) {
        if (rand.nextFloat() <= proba) {
            pattern[x][z] = isSet(pattern, x, z - 1) | isSet(pattern, x, z + 1) //
                | isSet(pattern, x - 1, z) | isSet(pattern, x + 1, z);
        }
    }

    private static boolean isSet(boolean[][] pattern, int x, int z) {
        if (x < 0 || x >= pattern.length) return false;
        if (z < 0 || z >= pattern[x].length) return false;
        return pattern[x][z];
    }

    private static boolean isEndBiome(ResourceLocation key) {
        return false;//TODO
    }
}
