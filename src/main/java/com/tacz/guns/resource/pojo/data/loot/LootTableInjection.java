package com.tacz.guns.resource.pojo.data.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.GunMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * TACZ 战利品注入（把 TACZ 的掉落池塞进原版战利品表）。
 * <p>
 * <b>1.21 迁移（javap 实证）</b>：1.20.1 走的是 Forge 的
 * {@code Deserializers.createLootTableSerializer().create()} + {@code ForgeHooks.loadLootTable(gson, id, json, true)}，
 * 这两个入口在 1.21.1 都已不存在：
 * <ul>
 *   <li>{@code net.minecraft.world.level.storage.loot.Deserializers} 整个类被删除；</li>
 *   <li>{@code net.neoforged.neoforge.common.CommonHooks} 只剩
 *       {@code lootPoolsCodec}/{@code modifyLoot(List, LootContext)}/{@code modifyLoot(ResourceLocation, ObjectArrayList, LootContext)}，
 *       <b>没有</b> {@code loadLootTable}。</li>
 * </ul>
 * 1.21 的战利品表本身就是一个 codec：{@code LootTable.DIRECT_CODEC}（{@code Codec<LootTable>}，
 * 即 {@code LootDataType.TABLE#codec()} 的同一实例）。因此这里直接用
 * {@code LootTable.DIRECT_CODEC.parse(JsonOps.INSTANCE, json)} 解码，
 * 再 {@link LootTable#setLootTableId(ResourceLocation)} 补回表 id（原 ForgeHooks 也会做这一步）。
 * <p>
 * <b>行为差异</b>：旧实现经 ForgeHooks 加载时，其它 mod 可以通过
 * {@code LootTableLoadEvent} 修改这张注入表；新实现是本地立即解码，<b>不再触发该事件链</b>
 * （注入表 id 是 TACZ 内部文件 id，正常情况下不会有第三方针对性修改）。
 * {@code createStacks} 的取值逻辑未变。
 */
public record LootTableInjection(List<ResourceLocation> lootTables, LootTable lootTable) {
    public static LootTableInjection fromJson(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "loot injection");
        List<ResourceLocation> lootTables = readLootTables(fileId, object);
        if (!object.has("pools")) {
            throw new JsonParseException("Loot injection " + fileId + " must define pools");
        }
        return new LootTableInjection(lootTables, loadLootTable(fileId, object));
    }

    private static LootTable loadLootTable(ResourceLocation fileId, JsonObject object) {
        // 1.21: LootTable.DIRECT_CODEC 替代被删除的 Deserializers/ForgeHooks.loadLootTable
        LootTable table = LootTable.DIRECT_CODEC.parse(JsonOps.INSTANCE, object)
                .resultOrPartial(error -> GunMod.LOGGER.error("Failed to parse loot injection {}: {}", fileId, error))
                .orElseThrow(() -> new JsonParseException("Failed to parse loot injection " + fileId
                        + " as a loot table, see log for details"));
        table.setLootTableId(fileId);
        return table;
    }

    private static List<ResourceLocation> readLootTables(ResourceLocation fileId, JsonObject object) {
        List<ResourceLocation> lootTables = new ArrayList<>();
        if (object.has("loot_tables")) {
            for (JsonElement table : GsonHelper.getAsJsonArray(object, "loot_tables")) {
                lootTables.add(ResourceLocation.parse(GsonHelper.convertToString(table, "loot table")));
            }
        } else if (object.has("loot_table")) {
            lootTables.add(ResourceLocation.parse(GsonHelper.getAsString(object, "loot_table")));
        } else {
            throw new JsonParseException("Loot injection " + fileId + " must define loot_table or loot_tables");
        }
        return List.copyOf(lootTables);
    }

    public List<ItemStack> createStacks(LootContext context) {
        List<ItemStack> stacks = new ArrayList<>();
        lootTable.getRandomItemsRaw(context, stacks::add);
        return stacks;
    }
}
