package com.tacz.guns.compat.kubejs.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.api.item.nbt.ItemNbtHelper;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

public class GunSmithTableResultInfo {
    private static final String TYPE_KEY = "type";
    private static final String ID_KEY = "id";
    private static final String COUNT_KEY = "count";
    private static final String NBT_KEY = "nbt";
    private static final String CUSTOM_ITEM_KEY = "item";
    private static final String OUTPUT_GROUP_KEY = "group";
    private final JsonObject json;

    private GunSmithTableResultInfo() {
        this(new JsonObject());
    }

    private GunSmithTableResultInfo(JsonObject jsonObject) {
        this.json = (jsonObject != null) ? jsonObject : new JsonObject();
    }

    public static GunSmithTableResultInfo create() {
        return new GunSmithTableResultInfo();
    }

    public static GunSmithTableResultInfo createFromJson(JsonObject jsonObject) {
        return new GunSmithTableResultInfo(jsonObject);
    }

    /**
     * 1.21 迁移：{@code RegistryInfo.ITEM.getId(...)}（kubejs 2001）→ {@link BuiltInRegistries#ITEM} 的注册名；
     * {@code stack.getTag()/getOrCreateTag()}（1.20 的 ItemStack）→ {@link ItemNbtHelper}（CustomData 承载）。
     */
    public static GunSmithTableResultInfo createFromItemStack(ItemStack stack) {
        GunSmithTableResultInfo info = create().setType(GunSmithTableResult.CUSTOM);
        JsonObject itemJson = new JsonObject();
        itemJson.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        itemJson.addProperty("count", stack.getCount());
        CompoundTag tag = ItemNbtHelper.getTag(stack);
        if (tag != null) {
            itemJson.addProperty("nbt", tag.toString());
        }
        info.setCustomItem(itemJson);
        return info;
    }

    /**
     * {@link GunSmithTableResultInfo}的TypeWrapper, 将其他类型转为{@link GunSmithTableResultInfo}
     * object为其他类型时优先解析{@link JsonObject}，其次{@link ItemStack}，
     * 之后尝试转化为{@link String}解析为{@link ResourceLocation}。
     * <p>
     * 1.21 迁移（kubejs 2101 实测缺失项，javap 实证）：
     * <ul>
     *   <li>{@code OutputItem} 类型已不存在 —— 分支删除，脚本传自定义物品请直接给 {@link ItemStack} 或 JSON；</li>
     *   <li>{@code ItemStackJS.of(Object)} 已不存在（2101 只剩 {@code wrap/getRawItem/...} 静态工具）——
     *       该兜底分支删除；</li>
     *   <li>{@code UtilsJS.getMCID(null, String)} 已不存在 —— 换成 {@link ResourceLocation#tryParse(String)}；</li>
     *   <li>{@code JsonIO.of(Object)} 已不存在 —— 换成“已经就是 {@link JsonElement} 才按 JSON 解析”，
     *       其余无法识别的对象抛 {@link IllegalArgumentException}（<b>不再静默</b>吞成空 JSON）。</li>
     * </ul>
     *
     * @param object 输入待转化对象
     * @return {@link GunSmithTableResultInfo}
     */
    public static GunSmithTableResultInfo of(Object object) {
        if (object instanceof GunSmithTableResultInfo info) {
            return info;
        } else if (object instanceof JsonObject jsonObject) {
            return createFromJson(jsonObject);
        } else if (object instanceof ItemStack stack) {
            return createFromItemStack(stack);
        } else if (object instanceof JsonElement element && element.isJsonObject()) {
            return createFromJson(element.getAsJsonObject());
        }
        if (object instanceof CharSequence || object instanceof ResourceLocation) {
            String idString = object.toString();
            ResourceLocation rl = ResourceLocation.tryParse(idString);
            if (rl != null) {
                TimelessItemWrapper.ItemIndexInfo indexInfo = TimelessItemWrapper.ItemIndexInfo.createFromResourceLocation(rl);
                if (indexInfo.isValidForRecipe()) {
                    return create().setType(indexInfo.getParent()).setId(indexInfo.getIndexId());
                }
            }
        }
        throw new IllegalArgumentException("Cannot convert " + (object == null ? "null" : object.getClass().getName())
                + " to GunSmithTableResultInfo, expected a JsonObject, an ItemStack, or an item id string");
    }

    public String getType() {
        return GsonHelper.getAsString(this.json, TYPE_KEY);
    }

    public GunSmithTableResultInfo setType(String typeName) {
        this.json.addProperty(TYPE_KEY, typeName);
        return this;
    }

    public ResourceLocation getId() {
        return ResourceLocation.tryParse(GsonHelper.getAsString(this.json, ID_KEY));
    }

    public GunSmithTableResultInfo setId(ResourceLocation id) {
        this.json.addProperty(ID_KEY, id.toString());
        return this;
    }

    /**
     * 1.21 迁移：{@code CraftingHelper.getNBT(JsonElement)} 已被 NeoForge 移除（javap 实证：
     * {@code CraftingHelper} 只剩 {@code makeIngredientCodec/makeIngredientMapCodec}），
     * 这里就地实现等价解析：SNBT 字符串走 {@link TagParser#parseTag(String)}，
     * JSON 对象走 {@code JsonOps → NbtOps} 转换。
     */
    public CompoundTag getNbt() {
        JsonElement element = this.json.get(NBT_KEY);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String snbt = element.getAsString();
            if (snbt.isEmpty()) {
                return null;
            }
            try {
                return TagParser.parseTag(snbt);
            } catch (CommandSyntaxException e) {
                throw new JsonParseException("Invalid nbt of gun smith table result: " + e.getMessage(), e);
            }
        }
        if (element.isJsonObject()) {
            Tag tag = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, element);
            if (tag instanceof CompoundTag compoundTag) {
                return compoundTag;
            }
        }
        throw new JsonParseException("Invalid nbt of gun smith table result, expected a SNBT string or a json object, got " + element);
    }

    public GunSmithTableResultInfo setNbt(CompoundTag nbt) {
        this.json.addProperty(NBT_KEY, nbt.toString());
        return this;
    }

    public int getCount() {
        return Math.max(GsonHelper.getAsInt(this.json, "count"), 1);
    }

    public GunSmithTableResultInfo setCount(int count) {
        this.json.addProperty(COUNT_KEY, count);
        return this;
    }

    public JsonObject getCustomItem() {
        return GsonHelper.getAsJsonObject(this.json, CUSTOM_ITEM_KEY);
    }

    public GunSmithTableResultInfo setCustomItem(JsonObject itemJson) {
        this.json.add(CUSTOM_ITEM_KEY, itemJson);
        return this;
    }

    public GunSmithTableResultInfo setGroupName(String groupName) {
        this.json.addProperty(OUTPUT_GROUP_KEY, groupName);
        return this;
    }

    public GunSmithTableResultInfo setGroup(OutputGroupName group) {
        this.setGroupName(group.getName());
        return this;
    }

    public JsonObject toJson() {
        return json;
    }

    public enum OutputGroupName {
        AMMO("ammo"),
        EXTENDED_MAG("extended_mag"),
        GRIP("grip"),
        MG("mg"),
        MUZZLE("muzzle"),
        PISTOL("pistol"),
        RIFLE("rifle"),
        RPG("rpg"),
        SCOPE("scope"),
        SHOTGUN("shotgun"),
        SMG("smg"),
        SNIPER("sniper"),
        STOCK("stock");

        private final String name;

        OutputGroupName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
