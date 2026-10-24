package com.tacz.guns.client.init;

import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModContainerScreen {
    @SubscribeEvent
    public static void clientSetup(RegisterMenuScreensEvent evt) {
        evt.register(GunSmithTableMenu.TYPE, GunSmithTableScreen::new);
    }
}
