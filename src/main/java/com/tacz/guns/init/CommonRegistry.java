package com.tacz.guns.init;

import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.GunPackLoader;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class CommonRegistry {
    private static boolean LOAD_COMPLETE = false;

    @SubscribeEvent
    public static void onSetupEvent(FMLCommonSetupEvent event) {
        event.enqueueWork(ModSyncedEntityData::init);
    }

    /**
     * 1.21 网络包注册入口：{@link RegisterPayloadHandlersEvent} 属于 mod 总线事件。
     */
    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        NetworkHandler.register(event);
    }

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        LOAD_COMPLETE = true;
    }

    public static boolean isLoadComplete() {
        return LOAD_COMPLETE;
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> {
            event.add(type, ModAttributes.BULLET_RESISTANCE);
        });
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        event.addRepositorySource(GunPackLoader.INSTANCE);
    }
}
