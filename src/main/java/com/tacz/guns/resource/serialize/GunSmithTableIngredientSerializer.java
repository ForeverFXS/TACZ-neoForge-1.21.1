package com.tacz.guns.resource.serialize;

import com.google.gson.*;
import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GunSmithTableIngredientSerializer implements JsonDeserializer<GunSmithTableIngredient> {
    @Override
    public GunSmithTableIngredient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            if (!jsonObject.has("item")) {
                throw new JsonSyntaxException("Expected " + jsonObject + " must has a item member");
            }
            Ingredient ingredient = readIngredient(jsonObject.get("item"));
            int count = 1;
            if (jsonObject.has("count")) {
                count = Math.max(GsonHelper.getAsInt(jsonObject, "count"), 1);
            }
            return new GunSmithTableIngredient(ingredient, count);
        } else {
            throw new JsonSyntaxException("Expected " + json + " to be a Pair because it's not an object");
        }
    }
    /**
     * 1.21 删除了 {@code Ingredient.fromJson}。这里按原版 1.20 的 JSON 结构自行解析：
     * 字符串 = 物品 id；{@code {"item": ...}}、{@code {"tag": "ns:path"}}；数组 = 多个候选。
     * 只用 {@link BuiltInRegistries} 与 {@link TagKey}，不依赖 RegistryAccess，
     * 因此可以在枪包加载阶段（没有注册表上下文）安全执行。
     */
    private static Ingredient readIngredient(JsonElement element) {
        List<Ingredient.Value> values = new ArrayList<>();
        collectValues(element, values);
        if (values.isEmpty()) {
            throw new JsonSyntaxException("Empty ingredient: " + element);
        }
        return Ingredient.fromValues(values.stream());
    }

    private static void collectValues(JsonElement element, List<Ingredient.Value> values) {
        if (element == null || element.isJsonNull()) {
            throw new JsonSyntaxException("Ingredient member is null");
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectValues(child, values);
            }
            return;
        }
        if (element.isJsonPrimitive()) {
            values.add(new Ingredient.ItemValue(itemStack(element.getAsString())));
            return;
        }
        if (!element.isJsonObject()) {
            throw new JsonSyntaxException("Unsupported ingredient: " + element);
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("tag")) {
            ResourceLocation tagId = ResourceLocation.tryParse(GsonHelper.getAsString(object, "tag"));
            if (tagId == null) {
                throw new JsonSyntaxException("Invalid item tag id: " + object);
            }
            values.add(new Ingredient.TagValue(TagKey.create(Registries.ITEM, tagId)));
            return;
        }
        if (object.has("item")) {
            JsonElement item = object.get("item");
            if (item.isJsonPrimitive()) {
                values.add(new Ingredient.ItemValue(itemStack(item.getAsString())));
            } else {
                collectValues(item, values);
            }
            return;
        }
        throw new JsonSyntaxException("Expected tag or item member: " + object);
    }

    private static ItemStack itemStack(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            GunMod.LOGGER.warn("[TACZ] Unknown item in gun smith table ingredient: {}", itemId);
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }
}
