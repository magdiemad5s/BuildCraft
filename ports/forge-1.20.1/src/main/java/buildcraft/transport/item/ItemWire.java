package buildcraft.transport.item;

import java.util.function.Consumer;

import buildcraft.lib.item.ICreativeTabItemProvider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The sixteen pipe-wire colours remain variants of the single legacy
 * {@code buildcrafttransport:wire} item.
 *
 * <p>Minecraft no longer exposes arbitrary item metadata, but ItemStack's
 * standard {@code Damage} tag still provides a stable integer field. Keeping
 * the original black-to-white metadata values there preserves old-stack
 * migration semantics while avoiding sixteen incompatible registry IDs.</p>
 */
public class ItemWire extends Item implements ICreativeTabItemProvider {
    public ItemWire(Properties properties) {
        super(properties);
    }

    public ItemStack createStack(DyeColor colour) {
        return createStack(colour, 1);
    }

    public ItemStack createStack(DyeColor colour, int count) {
        ItemStack stack = new ItemStack(this, count);
        stack.setDamageValue(
            LegacyWireVariantCodec.legacyMetadataFromModernDyeId(colour.getId())
        );
        return stack;
    }

    public DyeColor getColour(ItemStack stack) {
        int modernId = LegacyWireVariantCodec.modernDyeIdFromLegacyMetadata(
            getLegacyMetadata(stack)
        );
        return DyeColor.byId(modernId);
    }

    public int getLegacyMetadata(ItemStack stack) {
        return LegacyWireVariantCodec.clampLegacyMetadata(stack.getDamageValue());
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return "item.pipewire." + getColour(stack).getName();
    }

    @Override
    public void addCreativeTabItems(Consumer<ItemStack> output) {
        // Original 1.12.2 order: metadata 0 (black) through 15 (white).
        for (int metadata = 0; metadata < LegacyWireVariantCodec.VARIANT_COUNT; metadata++) {
            DyeColor colour = DyeColor.byId(
                LegacyWireVariantCodec.modernDyeIdFromLegacyMetadata(metadata)
            );
            output.accept(createStack(colour));
        }
    }
}