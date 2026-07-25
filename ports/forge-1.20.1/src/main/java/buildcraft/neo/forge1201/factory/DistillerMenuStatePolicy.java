package buildcraft.neo.forge1201.factory;

/**
 * Stable, loader-local contract for the BuildCraft 8 Distiller menu.
 *
 * <p>The menu deliberately transports tank values as full-width integers. Vanilla
 * {@code ContainerData} synchronization writes signed shorts and would corrupt
 * capacities or addon-provided amounts above 32767 mB.</p>
 */
public final class DistillerMenuStatePolicy {
    public static final String MODULE_ID = "buildcraftfactory";
    public static final String REGISTRY_PATH = "distiller";
    public static final String MENU_REGISTRY_PATH = "menu.distiller";

    public static final String TANKS_NBT_KEY = "tanks";
    public static final String INPUT_TANK_NBT_KEY = "in";
    public static final String GAS_TANK_NBT_KEY = "gasOut";
    public static final String LIQUID_TANK_NBT_KEY = "liquidOut";
    public static final String LEGACY_GAS_TANK_NBT_KEY = "out_gas";
    public static final String LEGACY_LIQUID_TANK_NBT_KEY = "out_liquid";
    public static final String BATTERY_NBT_KEY = "battery";
    public static final String LEGACY_BATTERY_NBT_KEY = "mjBattery";
    public static final String DISTILL_POWER_NBT_KEY = "distillPower";
    public static final String POWER_AVERAGE_NBT_KEY = "powerAvg";

    public static final int TANK_CAPACITY_MILLIBUCKETS = 4_000;
    public static final int TANK_COUNT = 3;
    public static final int VALUES_PER_TANK = 3;
    public static final int DATA_VALUE_COUNT = TANK_COUNT * VALUES_PER_TANK;

    /** Button IDs intentionally match the first data offset of each tank gauge. */
    public static final int INPUT_TANK_BUTTON = 0;
    public static final int GAS_TANK_BUTTON = 3;
    public static final int LIQUID_TANK_BUTTON = 6;

    private DistillerMenuStatePolicy() {
    }

    public static boolean isTankButton(int buttonId) {
        return buttonId == INPUT_TANK_BUTTON
            || buttonId == GAS_TANK_BUTTON
            || buttonId == LIQUID_TANK_BUTTON;
    }

    public static int tankIndexForButton(int buttonId) {
        if (!isTankButton(buttonId)) {
            throw new IllegalArgumentException("Unknown Distiller tank button " + buttonId);
        }
        return buttonId / VALUES_PER_TANK;
    }

    public static int sanitizeFluidId(int fluidId) {
        return Math.max(0, fluidId);
    }

    public static int sanitizeCapacity(int capacity) {
        return Math.max(0, capacity);
    }

    public static int clampAmount(int amount, int capacity) {
        int safeCapacity = sanitizeCapacity(capacity);
        return Math.max(0, Math.min(amount, safeCapacity));
    }
}
