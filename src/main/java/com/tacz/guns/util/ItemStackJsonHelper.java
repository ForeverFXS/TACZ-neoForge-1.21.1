package com.tacz.guns.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.api.item.nbt.ItemNbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21 移除了 Forge 的 CraftingHelper.getItemStack，这里提供等价的最小实现。
 * 支持的字段：item（必填，命名空间:id）、count（可选，默认 1）、nbt（可选，SNBT 字符串）。
 */
public final class ItemStackJsonHelper {
    private ItemStackJsonHelper() {
    }

    public static ItemStack getItemStack(JsonObject json, boolean readNbt) {
        return getItemStack(json, readNbt, true);
    }

    public static ItemStack getItemStack(JsonObject json, boolean readNbt, boolean allowEmpty) {
        String itemName = GsonHelper.getAsString(json, "item");
        ResourceLocation id = ResourceLocation.tryParse(itemName);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            throw new JsonSyntaxException("Unknown item '" + itemName + "'");
        }
        int count = GsonHelper.getAsInt(json, "count", 1);
        if (count <= 0) {
            if (allowEmpty) {
                return ItemStack.EMPTY;
            }
            throw new JsonSyntaxException("Invalid count " + count + " for item '" + itemName + "'");
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id), count);
        if (readNbt && json.has("nbt")) {
            String raw = GsonHelper.getAsString(json, "nbt");
            try {
                CompoundTag tag = TagParser.parseTag(raw);
                ItemNbtHelper.setTag(stack, tag);
            } catch (CommandSyntaxException e) {
                throw new JsonSyntaxException("Invalid nbt for item '" + itemName + "'", e);
            }
        }
        return stack;
    }
}
