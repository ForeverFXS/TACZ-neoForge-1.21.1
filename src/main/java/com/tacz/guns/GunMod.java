package com.tacz.guns;

import com.tacz.guns.api.resource.ResourceManager;
import com.tacz.guns.compat.kubejs.TimelessKubeJSPlugin;
import com.tacz.guns.config.ClientConfig;
import com.tacz.guns.config.CommonConfig;
import com.tacz.guns.config.ServerConfig;
import com.tacz.guns.init.*;
import com.tacz.guns.resource.GunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.server.packs.PackType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GunMod.MOD_ID)
public class GunMod {
    public static final String MOD_ID = "tacz";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    /**
     * 默认模型包文件夹
     */
    public static final String DEFAULT_GUN_PACK_NAME = "tacz_default_gun";

    // 注意：此处必须使用完全限定名 net.neoforged.fml.ModContainer。
    // 上方 import com.tacz.guns.init.* 已引入 TACZ 自有的 com.tacz.guns.init.ModContainer，
    // 若显式 import NeoForge 的 ModContainer（单类型 import 优先于按需 import），
    // 会导致本类中 ModContainer.CONTAINER_TYPE 解析到错误的类。
    public GunMod(net.neoforged.fml.ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.init());
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.init());
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.init());

        Dist side = FMLLoader.getDist();
        GunPackLoader.INSTANCE.packType = side.isClient() ? PackType.CLIENT_RESOURCES : PackType.SERVER_DATA;

        IEventBus bus = container.getEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.TILE_ENTITIES.register(bus);
        ModCreativeTabs.TABS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITY_TYPES.register(bus);
        ModRecipe.RECIPE_SERIALIZERS.register(bus);
        ModRecipe.RECIPE_TYPES.register(bus);
        ModLootModifiers.LOOT_MODIFIER_SERIALIZERS.register(bus);
        ModContainer.CONTAINER_TYPE.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModParticles.PARTICLE_TYPES.register(bus);
        ModAttributes.ATTRIBUTES.register(bus);
        ModPainting.PAINTINGS.register(bus);
        if (ModList.get().isLoaded("kubejs")) {
            bus.register(new TimelessKubeJSPlugin());
        }

        registerDefaultExtraGunPack();
        AttachmentPropertyManager.registerModifier();
    }

    private static void registerDefaultExtraGunPack() {
        String jarDefaultPackPath = String.format("/assets/%s/custom/%s", GunMod.MOD_ID, DEFAULT_GUN_PACK_NAME);
        ResourceManager.registerExportResource(GunMod.class, jarDefaultPackPath);
    }
}
