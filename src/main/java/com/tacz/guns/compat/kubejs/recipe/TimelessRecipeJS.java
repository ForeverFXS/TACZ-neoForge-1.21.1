package com.tacz.guns.compat.kubejs.recipe;

import com.google.gson.JsonObject;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.nbt.ItemNbtHelper;
import com.tacz.guns.compat.kubejs.util.GunSmithTableResultInfo;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.recipe.GunResult;
import com.tacz.guns.util.ItemStackJsonHelper;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Locale;

/**
 * TACZ 砧台配方的 KubeJS 脚本侧配方对象。
 * <p>
 * <b>2101 迁移说明（javap 实证）</b>：kubejs 2101 把配方改成<b>数据驱动</b>的
 * {@link KubeRecipe}（内部持有 {@code JsonObject json} + {@code RecipeComponentValueMap valueMap}），
 * 2001 的四个回调钩子 {@code readInputItem/writeInputItem/readOutputItem/writeOutputItem}
 * 在 2101 里<b>完全不存在</b>（{@code KubeRecipe} 没有任何可覆写的方法参与 JSON 读写），
 * 读写改由 {@code RecipeComponent} 的 {@code Codec} 完成（见
 * {@link GunSmithTableResultComponents}）。因此：
 * <ul>
 *   <li>四个 override 全部删除（代码不再属于“能被调用的路径”，否则会成为死代码 + 与 Codec 双写导致分叉）；</li>
 *   <li>{@code readOutputItem} 里“按 type 构造结果 ItemStack”的逻辑保留为
 *       {@link #createResultItemStack(JsonObject, CompoundTag)} 工具方法，并暴露
 *       {@link #getResultItemStack()} 给脚本侧使用；</li>
 *   <li>{@code InputItem/OutputItem} 类型在 2101 已不存在，对应脚本写法降级为
 *       {@code {item: <原版 Ingredient>, count: n}}（与原 datapack JSON 形状一致）。</li>
 * </ul>
 */
public class TimelessRecipeJS extends KubeRecipe {
    public TimelessRecipeJS() {
        super();
    }

    public GunSmithTableResultInfo getResultInfo() {
        return getValue(TimelessGunSmithTableRecipeSchema.RESULT);
    }

    public void setResultInfo(GunSmithTableResultInfo info) {
        setValue(TimelessGunSmithTableRecipeSchema.RESULT, info);
    }

    /**
     * 设置后对配方结果的影响改至{@link com.tacz.guns.compat.kubejs.util.GunSmithTableResultInfo}
     */
    public TimelessRecipeJS outputGroupName(String group) {
        GunSmithTableResultInfo info = getResultInfo();
        info.setGroupName(group);
        // 组件值是同一个对象，但重新 setValue 才会标记 changed，保证 createRecipe 时重新序列化
        setResultInfo(info);
        return this;
    }

    /**
     * 设置后对配方结果的影响改至{@link com.tacz.guns.compat.kubejs.util.GunSmithTableResultInfo}
     */
    public TimelessRecipeJS outputGroup(GunSmithTableResultInfo.OutputGroupName group) {
        return outputGroupName(group.getName());
    }

    /**
     * 按本配方的 {@code result} 组件内容构造实际的结果 {@link ItemStack}。
     * 等价于 2001 版 {@code readOutputItem(Object)} 的返回值（{@code OutputItem.of(stack).withCount(count)}）。
     */
    public ItemStack getResultItemStack() {
        GunSmithTableResultInfo info = getResultInfo();
        ItemStack stack = createResultItemStack(info.toJson(), info.getNbt());
        if (!stack.isEmpty()) {
            stack.setCount(info.getCount());
        }
        return stack;
    }

    /**
     * 旧 {@code readOutputItem} 的“JSON → ItemStack”逻辑，原样保留为工具方法（2101 没有等价钩子）。
     *
     * @param jsonObject {@code result} 组件的 JSON
     * @param extraTag   外层 {@code nbt} 成员解析出的 NBT（可为 null）
     */
    public static ItemStack createResultItemStack(JsonObject jsonObject, CompoundTag extraTag) {
        String typeName = GsonHelper.getAsString(jsonObject, "type");
        ItemStack resultItemStack = ItemStack.EMPTY;
        switch (typeName) {
            case GunSmithTableResult.GUN -> resultItemStack = getGunItemFromJson(jsonObject);
            case GunSmithTableResult.AMMO -> resultItemStack = getAmmoItemFromJson(jsonObject);
            case GunSmithTableResult.ATTACHMENT -> resultItemStack = getAttachmentItemFromJson(jsonObject);
            case GunSmithTableResult.CUSTOM -> {
                JsonObject resultObject = GsonHelper.getAsJsonObject(jsonObject, "item");
                resultItemStack = ItemStackJsonHelper.getItemStack(resultObject, true);
            }
        }
        if (extraTag != null && !resultItemStack.isEmpty()) {
            ItemNbtHelper.setTag(resultItemStack, extraTag);
        }
        return resultItemStack;
    }

    /**
     * 旧 {@code readOutputItem} 里推导输出分组（军械库分类）的逻辑。
     *
     * @return 分组名，无法推导时返回空串
     */
    public static String getResultGroupName(JsonObject jsonObject) {
        if (jsonObject.has("group")) {
            return GsonHelper.getAsString(jsonObject, "group");
        }
        String typeName = GsonHelper.getAsString(jsonObject, "type");
        return switch (typeName) {
            case GunSmithTableResult.GUN -> getGunTypeFromJson(jsonObject);
            case GunSmithTableResult.AMMO -> GunSmithTableResultInfo.OutputGroupName.AMMO.getName();
            case GunSmithTableResult.ATTACHMENT -> getAttachmentTypeFromJson(jsonObject);
            default -> "";
        };
    }

    private static ResourceLocation getIdFromJson(JsonObject jsonObject) {
        return ResourceLocation.parse(GsonHelper.getAsString(jsonObject, "id"));
    }

    private static String getGunTypeFromJson(JsonObject jsonObject) {
        ResourceLocation gunId = getIdFromJson(jsonObject);
        return TimelessAPI.getCommonGunIndex(gunId).map(CommonGunIndex::getType).orElse("");
    }

    private static ItemStack getGunItemFromJson(JsonObject jsonObject) {
        ResourceLocation gunId = getIdFromJson(jsonObject);
        int ammoCount;
        EnumMap<AttachmentType, ResourceLocation> attachments;
        GunResult gunResult = CommonAssetsManager.GSON.fromJson(jsonObject, GunResult.class);
        if (gunResult != null) {
            ammoCount = Math.max(0, gunResult.getAmmoCount());
            attachments = gunResult.getAttachments();
        } else {
            ammoCount = 0;
            attachments = new EnumMap<>(AttachmentType.class);
        }
        return TimelessAPI.getCommonGunIndex(gunId).map(gunIndex ->
                GunItemBuilder.create()
                        .setId(gunId)
                        .setAmmoCount(ammoCount)
                        .setAmmoInBarrel(false)
                        .putAllAttachment(attachments)
                        .setFireMode(gunIndex.getGunData().getFireModeSet().get(0))
                        .build()
        ).orElse(ItemStack.EMPTY);
    }

    private static String getAttachmentTypeFromJson(JsonObject jsonObject) {
        ResourceLocation attachmentId = getIdFromJson(jsonObject);
        return TimelessAPI.getCommonAttachmentIndex(attachmentId).map(attachmentIndex ->
                attachmentIndex.getType()
                        .name()
                        .toLowerCase(Locale.US)
        ).orElse("");
    }

    private static ItemStack getAttachmentItemFromJson(JsonObject jsonObject) {
        ResourceLocation attachmentId = getIdFromJson(jsonObject);
        return AttachmentItemBuilder.create().setId(attachmentId).build();
    }

    private static ItemStack getAmmoItemFromJson(JsonObject jsonObject) {
        ResourceLocation ammoId = getIdFromJson(jsonObject);
        return AmmoItemBuilder.create().setId(ammoId).build();
    }
}
