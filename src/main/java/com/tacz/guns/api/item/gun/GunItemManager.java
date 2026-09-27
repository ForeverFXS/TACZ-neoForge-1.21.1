package com.tacz.guns.api.item.gun;

import com.google.common.collect.Maps;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collection;
import java.util.Map;

public class GunItemManager {
    private static final Map<String, DeferredHolder<Item, ? extends AbstractGunItem>> GUN_ITEM_MAP = Maps.newHashMap();

    /**
     * 建议在 RegistryEvent.Register<Item> 事件时注册此枪械变种
     */
    public static void registerGunItem(String name, DeferredHolder<Item, ? extends AbstractGunItem> itemHolder) {
        GUN_ITEM_MAP.put(name, itemHolder);
    }

    public static DeferredHolder<Item, ? extends AbstractGunItem> getGunItemRegistryObject(String key) {
        return GUN_ITEM_MAP.get(key);
    }

    public static Collection<DeferredHolder<Item, ? extends AbstractGunItem>> getAllGunItems() {
        return GUN_ITEM_MAP.values();
    }
}
