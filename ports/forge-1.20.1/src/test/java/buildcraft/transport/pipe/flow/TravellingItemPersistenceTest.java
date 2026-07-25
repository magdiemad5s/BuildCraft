package buildcraft.transport.pipe.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import buildcraft.test.MinecraftTestBootstrap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TravellingItemPersistenceTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootStrap();
    }

    @Test
    void nbtRoundTripPreservesStackAndRelativeMovementState() {
        TravellingItem original = new TravellingItem(new ItemStack(Items.DIAMOND, 12));
        original.colour = DyeColor.PURPLE;
        original.toCenter = false;
        original.speed = 0.08;
        original.tickStarted = 100;
        original.tickFinished = 115;
        original.timeToDest = 15;
        original.side = Direction.EAST;
        original.tried.add(Direction.WEST);
        original.tried.add(Direction.DOWN);
        original.isPhantom = true;

        CompoundTag saved = original.writeToNbt(108);
        TravellingItem restored = new TravellingItem(saved, 1_000);

        assertTrue(restored.stack.is(Items.DIAMOND));
        assertEquals(12, restored.stack.getCount());
        assertEquals(DyeColor.PURPLE, restored.colour);
        assertFalse(restored.toCenter);
        assertEquals(0.08, restored.speed);
        assertEquals(992, restored.tickStarted);
        assertEquals(1_007, restored.tickFinished);
        assertEquals(15, restored.timeToDest);
        assertEquals(Direction.EAST, restored.side);
        assertEquals(original.tried, restored.tried);
        assertTrue(restored.isPhantom);

        CompoundTag savedAgain = restored.writeToNbt(1_000);
        assertEquals(saved.getCompound("stack"), savedAgain.getCompound("stack"));
        assertEquals(saved.getInt("tickStarted"), savedAgain.getInt("tickStarted"));
        assertEquals(saved.getInt("tickFinished"), savedAgain.getInt("tickFinished"));
    }

    @Test
    void zeroDistanceStillGetsOneFiniteMovementTick() {
        TravellingItem item = new TravellingItem(new ItemStack(Items.COBBLESTONE));
        item.genTimings(50, 0);

        assertEquals(1, item.timeToDest);
        assertEquals(0, item.getWayThrough(50));
        assertEquals(1, item.getWayThrough(51));
        assertTrue(Double.isFinite(item.getWayThrough(50)));
        assertEquals(Vec3.ZERO, item.interpolatePosition(Vec3.ZERO, new Vec3(1, 0, 0), 50, 0));
        assertEquals(new Vec3(1, 0, 0), item.interpolatePosition(Vec3.ZERO, new Vec3(1, 0, 0), 51, 0));
    }

    @Test
    void mergeConservesTheExactItemCount() {
        TravellingItem first = new TravellingItem(new ItemStack(Items.IRON_INGOT, 10));
        TravellingItem second = new TravellingItem(new ItemStack(Items.IRON_INGOT, 20));
        first.toCenter = second.toCenter = true;
        first.side = second.side = Direction.NORTH;
        first.tickFinished = second.tickFinished = 10;

        int before = first.stack.getCount() + second.stack.getCount();
        assertTrue(first.mergeWith(second));
        assertEquals(before, first.stack.getCount());
    }
}
