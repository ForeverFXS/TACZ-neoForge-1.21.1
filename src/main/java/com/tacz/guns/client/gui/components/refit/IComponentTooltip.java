package com.tacz.guns.client.gui.components.refit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public interface IComponentTooltip {
    /**
     * 获取物品的文本提示
     */
    static List<Component> getTooltipFromItem(ItemStack stack) {
        // 1.21 起 ItemStack#getTooltipLines 需要 TooltipContext，且 TooltipFlag 需经
        // NeoForge ClientTooltipFlag 包装。原版 Screen 已提供等价静态方法，直接复用以保证行为一致。
        return Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
    }

    /**
     * 添加此接口，会调用此渲染文本提示
     *
     * @param consumer 需要渲染的文本提示
     */
    void renderTooltip(Consumer<List<Component>> consumer);
}
