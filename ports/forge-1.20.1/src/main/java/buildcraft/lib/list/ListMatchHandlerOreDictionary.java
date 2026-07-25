/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.list;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import buildcraft.api.lists.ListMatchHandler;
import buildcraft.lib.CreativeTabManager;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

public class ListMatchHandlerOreDictionary extends ListMatchHandler {
    private static final Set<String> CLASSIFICATION_ONLY_CATEGORIES = Set.of(
        "ore_bearing_ground", "ore_rates", "ores_in_ground"
    );

    @Override
    public boolean matches(Type type, @Nonnull ItemStack stack, @Nonnull ItemStack target, boolean precise) {
        Set<TagKey<Item>> sourceTags = getTags(stack);
        Set<TagKey<Item>> targetTags = getTags(target);
        if (sourceTags.isEmpty() || targetTags.isEmpty()) {
            return false;
        }
        if (type == Type.CLASS) {
            return sourceTags.stream().anyMatch(targetTags::contains);
        }
        Set<TagKey<Item>> sourceEquivalenceTags = getEquivalenceTags(sourceTags);
        Set<TagKey<Item>> targetEquivalenceTags = getEquivalenceTags(targetTags);
        if (sourceEquivalenceTags.isEmpty() || targetEquivalenceTags.isEmpty()) {
            return false;
        }
        TagKey<Item> bestSource = getBestTag(sourceEquivalenceTags);
        String sourcePart = type == Type.MATERIAL ? getMaterial(bestSource) : getType(bestSource);
        return targetEquivalenceTags.stream().anyMatch(tag -> sourcePart.equals(
            type == Type.MATERIAL ? getMaterial(tag) : getType(tag)
        ));
    }

    @Override
    public boolean isValidSource(Type type, @Nonnull ItemStack stack) {
        Set<TagKey<Item>> tags = getTags(stack);
        return type == Type.CLASS ? !tags.isEmpty() : !getEquivalenceTags(tags).isEmpty();
    }

    private static Set<TagKey<Item>> getTags(ItemStack stack) {
        Set<TagKey<Item>> tags = new LinkedHashSet<>();
        stack.getTags().forEach(tags::add);
        return tags;
    }

    private static Set<TagKey<Item>> getEquivalenceTags(Set<TagKey<Item>> tags) {
        Set<TagKey<Item>> equivalenceTags = new LinkedHashSet<>();
        for (TagKey<Item> tag : tags) {
            String namespace = tag.location().getNamespace();
            String path = tag.location().getPath();
            int split = path.indexOf('/');
            if (!("forge".equals(namespace) || "c".equals(namespace)) || split <= 0) {
                continue;
            }
            String category = path.substring(0, split);
            if (!CLASSIFICATION_ONLY_CATEGORIES.contains(category)) {
                equivalenceTags.add(tag);
            }
        }
        return equivalenceTags;
    }

    private static TagKey<Item> getBestTag(Set<TagKey<Item>> tags) {
        return tags.stream().max(
            Comparator.<TagKey<Item>>comparingInt(tag -> pathDepth(tag.location().getPath()))
                .thenComparingInt(tag -> tag.location().getPath().length())
        ).orElseThrow();
    }

    private static int pathDepth(String path) {
        int depth = 1;
        for (int index = 0; index < path.length(); index++) {
            if (path.charAt(index) == '/') {
                depth++;
            }
        }
        return depth;
    }

    private static String getType(TagKey<Item> tag) {
        String path = tag.location().getPath();
        int split = path.lastIndexOf('/');
        String type = split < 0 ? path : path.substring(0, split);
        return qualifyNonCommonNamespace(tag, type);
    }

    private static String getMaterial(TagKey<Item> tag) {
        String path = tag.location().getPath();
        int split = path.lastIndexOf('/');
        String material = split < 0 ? path : path.substring(split + 1);
        return qualifyNonCommonNamespace(tag, material);
    }

    private static String qualifyNonCommonNamespace(TagKey<Item> tag, String value) {
        String namespace = tag.location().getNamespace();
        return "forge".equals(namespace) || "c".equals(namespace) ? value : namespace + ':' + value;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public NonNullList<ItemStack> getClientExamples(Type type, @Nonnull ItemStack stack) {
        NonNullList<ItemStack> examples = NonNullList.create();
        if (!isValidSource(type, stack)) {
            return examples;
        }
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            NonNullList<ItemStack> variants = NonNullList.create();
            CreativeTabManager.addItemVariants(item, variants::add);
            for (ItemStack candidate : variants) {
                if (matches(type, stack, candidate, false)
                    && examples.stream().noneMatch(existing -> ItemStack.isSameItemSameTags(existing, candidate))) {
                    examples.add(candidate.copy());
                }
            }
        }
        return examples;
    }
}
