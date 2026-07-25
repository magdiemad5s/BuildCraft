package buildcraft.lib.misc;

import static org.junit.jupiter.api.Assertions.assertSame;

import buildcraft.test.MinecraftTestBootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlockUtilFluidDetectionTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootStrap();
    }

    @Test
    void vanillaFluidBlocksResolveToTheirStillFluid() {
        assertSame(Fluids.WATER, BlockUtil.getFluidWithFlowing(Blocks.WATER));
    }

    @Test
    void blocksWithoutFluidReturnTheEmptySentinel() {
        assertSame(Fluids.EMPTY, BlockUtil.getFluidWithoutFlowing(Blocks.STONE.defaultBlockState()));
        assertSame(Fluids.EMPTY, BlockUtil.getFluidWithFlowing(Blocks.STONE));
    }
}
