package buildcraft.neo.energy;

/**
 * Loader-neutral pipe directions.
 *
 * <p>Forge and NeoForge adapters must translate their {@code Direction} value
 * at the capability boundary rather than leaking a loader class into the
 * shared power-accounting code.</p>
 */
public enum PipeSide {
    DOWN,
    UP,
    NORTH,
    SOUTH,
    WEST,
    EAST
}
