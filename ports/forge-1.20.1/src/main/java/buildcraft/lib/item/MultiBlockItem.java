package buildcraft.lib.item;

import java.util.EnumMap;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class MultiBlockItem<E extends Enum<E> & StringRepresentable> extends BlockItem implements ICreativeTabItemProvider {

    protected final E defaultType;
    protected final E[] values;
    private final boolean exposesAllVariants;

    /**
     * Compatibility constructor for a separately registered modern item.
     * New ports of legacy metadata items should use the array constructor.
     */
    @SuppressWarnings("unchecked")
    public MultiBlockItem(Block block, Properties properties, E type, @Nullable EnumMap<E, MultiBlockItem<E>> map) {
        super(block, properties);
        this.defaultType = Objects.requireNonNull(type, "type");
        this.values = (E[]) type.getDeclaringClass().getEnumConstants();
        this.exposesAllVariants = false;
        if (map != null) {
            map.put(type, this);
        }
    }

    /**
     * Creates one registry item whose stacks retain the legacy enum metadata.
     * Every enum key is deliberately mapped to this same item object so older
     * call sites can discover the live registry item without creating aliases.
     */
    public MultiBlockItem(
        Block block,
        Properties properties,
        E defaultType,
        E[] values,
        @Nullable EnumMap<E, MultiBlockItem<E>> map
    ) {
        super(block, properties);
        this.defaultType = Objects.requireNonNull(defaultType, "defaultType");
        this.values = Objects.requireNonNull(values, "values").clone();
        this.exposesAllVariants = true;
        if (this.values.length == 0) {
            throw new IllegalArgumentException("values cannot be empty");
        }
        if (map != null) {
            for (E value : this.values) {
                map.put(value, this);
            }
        }
    }

	@Override
	public void addCreativeTabItems(Consumer<ItemStack> output) {
        if (exposesAllVariants) {
            for (E value : values) {
                output.accept(getStack(value));
            }
        } else {
            output.accept(getStack(defaultType));
        }
	}


	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(getDescriptionId() + "_" + getType(stack).getSerializedName());
	}

    /**
     * Retained for callers that only know about split modern items.
     * Stack-sensitive code must use {@link #getType(ItemStack)}.
     */
	public E getType() {
		return defaultType;
	}

    public E getType(ItemStack stack) {
        if (!exposesAllVariants) {
            return defaultType;
        }
        int ordinal = LegacyItemMetadata.boundedOrdinal(
            LegacyItemMetadata.get(stack),
            values.length,
            defaultType.ordinal()
        );
        return values[ordinal];
    }

    public ItemStack getStack(E type) {
        return getStack(type, 1);
    }

    public ItemStack getStack(E type, int count) {
        Objects.requireNonNull(type, "type");
        return LegacyItemMetadata.create(this, count, type.ordinal());
    }
}
