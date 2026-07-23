package buildcraft.neo.neoforge1211;

import java.util.List;

/**
 * Stable public module identities inherited from the 1.12.2 release line.
 *
 * <p>These are not branding aliases. Future registries, packet meanings and
 * persistence migrations must retain their existing legacy namespace.</p>
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

    public static final List<String> ALL = List.of(
        LIB,
        CORE,
        BUILDERS,
        ENERGY,
        FACTORY,
        SILICON,
        TRANSPORT,
        ROBOTICS
    );

    private LegacyModuleIds() {
    }
}
