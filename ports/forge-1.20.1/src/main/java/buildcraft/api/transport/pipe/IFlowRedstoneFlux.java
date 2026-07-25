package buildcraft.api.transport.pipe;

import net.minecraft.core.Direction;

/**
 * Forge Energy transport flow retained under the legacy Redstone Flux API
 * name used by BuildCraft 8.
 */
public interface IFlowRedstoneFlux {
    void reconfigure();

    /**
     * Pulls Forge Energy from the adjacent storage into this receiver pipe.
     *
     * @return FE extracted and accepted by the pipe
     */
    int tryExtractPower(int maximumForgeEnergy, Direction from);
}
