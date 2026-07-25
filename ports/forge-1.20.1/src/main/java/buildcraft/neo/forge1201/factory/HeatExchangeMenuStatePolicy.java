package buildcraft.neo.forge1201.factory;

/**
 * Stable data-slot layout for the BuildCraft Heat Exchanger menu.
 *
 * <p>Each tank occupies five consecutive fields. Button IDs intentionally match
 * the first field of their tank record so tank clicks and synchronized gauges
 * use the same offsets.</p>
 */
public final class HeatExchangeMenuStatePolicy {
    public static final int TANK_COUNT = 4;
    public static final int FIELDS_PER_TANK = 5;
    public static final int CAPACITY_FIELD_OFFSET = 4;
    public static final int CLIENT_DATA_COUNT = TANK_COUNT * FIELDS_PER_TANK;

    private HeatExchangeMenuStatePolicy() {
    }

    public static int dataOffsetForTank(int tankIndex) {
        if (tankIndex < 0 || tankIndex >= TANK_COUNT) {
            throw new IllegalArgumentException("Unknown Heat Exchanger tank index " + tankIndex);
        }
        return tankIndex * FIELDS_PER_TANK;
    }

    public static int tankIndexForButton(int buttonId) {
        for (int tankIndex = 0; tankIndex < TANK_COUNT; tankIndex++) {
            if (buttonId == dataOffsetForTank(tankIndex)) {
                return tankIndex;
            }
        }
        return -1;
    }
}
