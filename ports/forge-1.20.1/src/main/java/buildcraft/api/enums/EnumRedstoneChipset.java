package buildcraft.api.enums;

import java.util.Locale;

import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.item.ItemRedstoneChipsetLegacy;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

public enum EnumRedstoneChipset implements StringRepresentable {
    RED,
    IRON,
    GOLD,
    QUARTZ,
    DIAMOND;

    private final String name = name().toLowerCase(Locale.ROOT);

    public ItemStack getStack(int stackSize) {
        if (!BCSiliconItems.REDSTONE_CHIPSET.isPresent()) {
            return ItemStack.EMPTY;
        }
        return BCSiliconItems.REDSTONE_CHIPSET.get().getStack(this, stackSize);
    }

    public ItemStack getStack() {
        return getStack(1);
    }

    public static EnumRedstoneChipset fromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return RED;
        }
        if (stack.getItem() instanceof ItemRedstoneChipsetLegacy chipset) {
            return chipset.getType(stack);
        }
        return RED;
    }

    public static EnumRedstoneChipset fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length) {
            return RED;
        }
        return values()[ordinal];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}