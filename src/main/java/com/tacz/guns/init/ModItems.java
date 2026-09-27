package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, GunMod.MOD_ID);

    public static DeferredHolder<Item, ModernKineticGunItem> MODERN_KINETIC_GUN = ITEMS.register("modern_kinetic_gun", ModernKineticGunItem::new);

//    public static DeferredHolder<Item, ThrowableItem> M67 = ITEMS.register("m67", ThrowableItem::new);

    public static DeferredHolder<Item, Item> AMMO = ITEMS.register("ammo", AmmoItem::new);
    public static DeferredHolder<Item, AttachmentItem> ATTACHMENT = ITEMS.register("attachment", AttachmentItem::new);

    public static DeferredHolder<Item, GunSmithTableItem> GUN_SMITH_TABLE = ITEMS.register("gun_smith_table", () -> new DefaultTableItem(ModBlocks.GUN_SMITH_TABLE.get()));
    public static DeferredHolder<Item, GunSmithTableItem> WORKBENCH_111 = ITEMS.register("workbench_a", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_111.get()));
    public static DeferredHolder<Item, GunSmithTableItem> WORKBENCH_211 = ITEMS.register("workbench_b", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_211.get()));
    public static DeferredHolder<Item, GunSmithTableItem> WORKBENCH_121 = ITEMS.register("workbench_c", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_121.get()));


    public static DeferredHolder<Item, Item> TARGET = ITEMS.register("target", () -> new BlockItem(ModBlocks.TARGET.get(), new Item.Properties()));
    public static DeferredHolder<Item, Item> STATUE = ITEMS.register("statue", () -> new BlockItem(ModBlocks.STATUE.get(), new Item.Properties()));
    public static DeferredHolder<Item, Item> AMMO_BOX = ITEMS.register("ammo_box", AmmoBoxItem::new);
    public static DeferredHolder<Item, Item> TARGET_MINECART = ITEMS.register("target_minecart", TargetMinecartItem::new);

    @SubscribeEvent
    public static void onItemRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            GunItemManager.registerGunItem(ModernKineticGunItem.TYPE_NAME, MODERN_KINETIC_GUN);
        }
    }
}