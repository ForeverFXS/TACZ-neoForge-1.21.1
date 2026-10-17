package com.tacz.guns.event;

import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.handshake.ServerMessageSyncedEntityDataMapping;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 网络层运行时事件。
 * <p>
 * 原实现把 SyncedEntityData 键映射挂在 Forge 的登录阶段（loginIndex + markAsLoginPacket）。
 * NeoForge 1.21 只允许 configuration / play 两类自定义 payload，没有自定义 login payload，
 * 因此迁移到 play 阶段：玩家登录后由服务端立即下发映射，客户端处理完回 {@code Acknowledge}。
 * <p>
 * {@code PlayerLoggedInEvent} 在客户端同样会触发（实体为 LocalPlayer），
 * 这里用 {@code instanceof ServerPlayer} 保证只在服务端发送。
 */
@EventBusSubscriber
public final class NetworkEvent {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.sendToClientPlayer(new ServerMessageSyncedEntityDataMapping(), player);
        }
    }
}
