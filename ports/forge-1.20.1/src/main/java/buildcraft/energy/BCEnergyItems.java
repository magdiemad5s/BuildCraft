package buildcraft.energy;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/** Legacy Energy items whose registry identities are independent of blocks or fluids. */
public final class BCEnergyItems {
    public static final RegistryObject<Item> GLOB_OF_OIL = BCEnergy.ITEMS.register(
        "glob_of_oil",
        () -> new Item(new Item.Properties())
    );

    private BCEnergyItems() {
    }

    /** Forces static registration before the Energy deferred register is attached to the event bus. */
    public static void init() {
    }

    public static List<ItemStack> getCreativeTabItems() {
        return GLOB_OF_OIL.isPresent()
            ? List.of(GLOB_OF_OIL.get().getDefaultInstance())
            : List.of();
    }
}
