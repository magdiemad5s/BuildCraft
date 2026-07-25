/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.item;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import buildcraft.api.core.BCLog;
import buildcraft.api.enums.EnumSnapshotType;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.Snapshot.Header;
import buildcraft.lib.item.ICreativeTabItemProvider;
import buildcraft.lib.item.LegacyItemMetadata;
import buildcraft.lib.misc.HashUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ItemSnapshot extends Item implements ICreativeTabItemProvider {
	
    private static ItemSnapshot instance;

    public ItemSnapshot(Item.Properties properties) {
        super(properties);
        instance = this;
    }

    public static ItemStack getClean(EnumSnapshotType snapshotType) {
        if (instance == null) {
            BCLog.logger.warn("ItemSnapshot#getClean called before item registration");
            return ItemStack.EMPTY;
        }
        return createStack(EnumItemSnapshotType.get(snapshotType, false));
    }

    public static ItemStack getUsed(EnumSnapshotType snapshotType, Header header) {
        if (instance == null) {
            BCLog.logger.warn("ItemSnapshot#getUsed called before item registration");
            return ItemStack.EMPTY;
        }
        ItemStack stack = createStack(EnumItemSnapshotType.get(snapshotType, true));
        stack.getOrCreateTag().put("header", header.serializeNBT());
        return stack;
    }

    private static ItemStack createStack(EnumItemSnapshotType type) {
        return LegacyItemMetadata.create(instance, 1, type.ordinal());
    }

    public static Header getHeader(ItemStack stack) {
        if (stack.getItem() instanceof ItemSnapshot) {
            if (EnumItemSnapshotType.getFromStack(stack).used) {
                CompoundTag nbt = stack.getTag();
                if (nbt != null) {
                    if (nbt.contains("header", Tag.TAG_COMPOUND)) {
                        return new Header(nbt.getCompound("header"));
                    }
                }
            }
        }
        return null;
    }
    
    @Override
	public int getMaxStackSize(ItemStack stack) {
    	return EnumItemSnapshotType.getFromStack(stack).used ? 1 : 16;
	}

    
    @Override
    public void addCreativeTabItems(Consumer<ItemStack> output) {
        output.accept(getClean(EnumSnapshotType.BLUEPRINT));
        output.accept(getClean(EnumSnapshotType.TEMPLATE));
    }

 /*   @Override
    @OnlyIn(Dist.CLIENT)
    public void addModelVariants(Int2ObjectMap<ModelResourceLocation> variants) {
        for (EnumItemSnapshotType type : EnumItemSnapshotType.values()) {
            addVariant(variants, type.ordinal(), type.getName());
        }
    }*/
    
    

    @Override
	public String getDescriptionId(ItemStack stack) {
        EnumItemSnapshotType type = EnumItemSnapshotType.getFromStack(stack);
        if (type.snapshotType == EnumSnapshotType.BLUEPRINT) {
            return "item.buildcraftbuilders.blueprint";
        }
        return "item.buildcraftbuilders.template";
	}
    
    
    @OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        EnumItemSnapshotType type = EnumItemSnapshotType.getFromStack(stack);
        Snapshot.Header header = getHeader(stack);
        if (header == null) {
            tooltip.add(Component.translatable("item.blueprint.blank").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.buildcraftbuilders.snapshot.clean_hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(getDisplayName(header).withStyle(ChatFormatting.GRAY));

        String ownerName = header.ownerName;
        if (ownerName == null || ownerName.isBlank()) {
            Player owner = world == null ? null : header.getOwnerPlayer(world);
            if (owner != null) {
                ownerName = owner.getGameProfile().getName();
            }
        }
        if (ownerName != null && !ownerName.isBlank()) {
            tooltip.add(Component.translatable("item.blueprint.author", ownerName).withStyle(ChatFormatting.DARK_GRAY));
        }

        tooltip.add(Component.translatable(
            type.snapshotType == EnumSnapshotType.BLUEPRINT
                ? "item.buildcraftbuilders.blueprint.used_hint"
                : "item.buildcraftbuilders.template.used_hint"
        ).withStyle(ChatFormatting.DARK_GRAY));

        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable(
                "item.buildcraftbuilders.snapshot.hash",
                HashUtil.convertHashToString(header.key.hash)
            ).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable(
                "item.buildcraftbuilders.snapshot.created",
                header.created.toInstant().toString()
            ).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable(
                "item.buildcraftbuilders.snapshot.owner_uuid",
                header.owner.toString()
            ).withStyle(ChatFormatting.DARK_GRAY));
        }
	}

    private static MutableComponent getDisplayName(Snapshot.Header header) {
        if (header.name == null || header.name.isBlank() || "<unnamed>".equals(header.name)) {
            return Component.translatable("item.blueprint.unnamed");
        }
        return Component.literal(header.name);
    }

    public enum EnumItemSnapshotType implements StringRepresentable {
        TEMPLATE_CLEAN(EnumSnapshotType.TEMPLATE, false),
        TEMPLATE_USED(EnumSnapshotType.TEMPLATE, true),
        BLUEPRINT_CLEAN(EnumSnapshotType.BLUEPRINT, false),
        BLUEPRINT_USED(EnumSnapshotType.BLUEPRINT, true);

        public final EnumSnapshotType snapshotType;
        public final boolean used;

        EnumItemSnapshotType(EnumSnapshotType snapshotType, boolean used) {
            this.snapshotType = snapshotType;
            this.used = used;
        }

        
        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static EnumItemSnapshotType get(EnumSnapshotType snapshotType, boolean used) {
            if (snapshotType == EnumSnapshotType.TEMPLATE) {
                return !used ? TEMPLATE_CLEAN : TEMPLATE_USED;
            } else if (snapshotType == EnumSnapshotType.BLUEPRINT) {
                return !used ? BLUEPRINT_CLEAN : BLUEPRINT_USED;
            } else {
                throw new IllegalArgumentException();
            }
        }

        public static EnumItemSnapshotType getFromStack(ItemStack stack) {
            if (!(stack.getItem() instanceof ItemSnapshot)) {
                BCLog.logger.warn("ItemSnapshot.EnumItemSnapshotType: not a snapshot ItemStack");
                return BLUEPRINT_CLEAN;
            }
            int ordinal = LegacyItemMetadata.boundedOrdinal(
                LegacyItemMetadata.get(stack),
                values().length,
                BLUEPRINT_CLEAN.ordinal()
            );
            return values()[ordinal];
        }
    }
}
