package com.tacz.guns.init;

import com.tacz.guns.client.gui.compat.ClothConfigScreen;
import com.tacz.guns.compat.carryon.BlackList;
import com.tacz.guns.compat.cloth.MenuIntegration;
import com.tacz.guns.compat.oculus.OculusCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CompatRegistry {
    public static final String CLOTH_CONFIG = "cloth_config";
    /** 1.20.1 的光影模组 modid。 */
    public static final String OCULUS = "oculus";
    /** 1.21.1 的光影模组 modid。 */
    public static final String IRIS = "iris";
    public static final String CARRY_ON_ID = "carryon";

    @SubscribeEvent
    public static void onEnqueue(final InterModEnqueueEvent event) {
        // 1.21.1 的 FML 移除了 DistExecutor（javap 确认 net.neoforged.fml.DistExecutor 不存在），
        // 改用 FMLEnvironment.dist 判断；MenuIntegration 只在客户端分支的 lambda 里才会被类加载，等价 safeRunWhenOn。
        event.enqueueWork(() -> checkModLoad(CLOTH_CONFIG, () -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                MenuIntegration.registerModsPage();
            }
        }));
        event.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClothConfigScreen.registerNoClothConfigPage();
            }
        });
        // 1.21.1 的光影模组是 Iris(modid=iris)，1.20.1 是 Oculus(modid=oculus)；任一存在都初始化兼容层。
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded(OCULUS) || ModList.get().isLoaded(IRIS)) {
                OculusCompat.initCompat();
            }
        });
        event.enqueueWork(() -> checkModLoad(CARRY_ON_ID, BlackList::addBlackList));
    }

    public static void checkModLoad(String modId, Runnable runnable) {
        if (ModList.get().isLoaded(modId)) {
            runnable.run();
        }
    }
}
