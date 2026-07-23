package buildcraft.neo.forge1201;

import java.util.List;

/**
 * Immutable legacy module identifiers. These are compatibility-facing names,
 * not branding; changing one is a data and dependency migration.
 */
public final class LegacyModuleIds {
    public static final String LIB = "buildcraftlib";
    public static final String CORE = "buildcraftcore";
    public static final String BUILDERS = "buildcraftbuilders";
    public static final String ENERGY = "buildcraftenergy";
    public static final String FACTORY = "buildcraftfactory";
    public static final String SILICON = "buildcraftsilicon";
    public static final String TRANSPORT = "buildcrafttransport";
    public static final String ROBOTICS = "buildcraftrobotics";

    private static final List<String> ORDERED = List.of(
        LIB, CORE, BUILDERS, ENERGY, FACTORY, SILICON, TRANSPORT, ROBOTICS
    );

    private LegacyModuleIds() {
    }

    public static List<String> ordered() {
        return ORDERED;
    }

    public static boolean isKnown(String moduleId) {
        return ORDERED.contains(moduleId);
    }
}
