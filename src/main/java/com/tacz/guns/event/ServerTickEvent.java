package com.tacz.guns.event;

import com.tacz.guns.util.CycleTaskHelper;
import net.minecraftforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber
public class ServerTickEvent {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // 更新 CycleTaskHelper 中的任务
        CycleTaskHelper.tick();
    }
}
