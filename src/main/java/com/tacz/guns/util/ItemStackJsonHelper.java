package com.tacz.guns.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.api.item.nbt.ItemNbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

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
        // 1.21 原生格式：result 里直接写 components（{"minecraft:entity_data":{...}} 等）。
        // 这类数据没法用下面的 nbt(SNBT) 路径表达，必须交给官方 ItemStack.CODEC 解析，
        // 否则组件会被静默丢弃（例：画作变体 entity_data、物品名 item_name）。
        if (readNbt && json.has("components")) {
            JsonObject normalized = new JsonObject();
            String idText = json.has("id") ? GsonHelper.getAsString(json, "id") : GsonHelper.getAsString(json, "item");
            normalized.addProperty("id", idText);
            normalized.addProperty("count", GsonHelper.getAsInt(json, "count", 1));
            normalized.add("components", json.get("components"));
            Optional<ItemStack> parsed = ItemStack.CODEC.parse(JsonOps.INSTANCE, normalized).result();
            if (parsed.isEmpty() || parsed.get().isEmpty()) {
                throw new JsonSyntaxException("Invalid item components for '" + idText + "': " + normalized);
            }
            return parsed.get();
        }
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
            ItemNbtHelper.setTag(stack, readNbtElement(json.get("nbt"), itemName));
        }
        return stack;
    }

    /**
     * 1.20.1 的 Forge CraftingHelper 同时接受 {"nbt": "{...SNBT...}"} 与 {"nbt": {"Key": "Value"}} 两种写法；
     * TACZ 自有的枪包数据（data/blocks/*.json 的 tabs[].icon.nbt、index 等）大量使用对象形式，
     * 因此这里两种都支持：字符串走 SNBT 解析，对象走 JsonOps -> NbtOps 结构转换。
     */
    private static CompoundTag readNbtElement(JsonElement element, String itemName) {
        if (element == null || element.isJsonNull()) {
            return new CompoundTag();
        }
        if (element.isJsonObject()) {
            if (!(JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, element) instanceof CompoundTag compoundTag)) {
                throw new JsonSyntaxException("Invalid nbt object for item '" + itemName + "'");
            }
            return compoundTag;
        }
        try {
            return TagParser.parseTag(GsonHelper.convertToString(element, "nbt"));
        } catch (CommandSyntaxException e) {
            throw new JsonSyntaxException("Invalid nbt for item '" + itemName + "'", e);
        }
    }
}
