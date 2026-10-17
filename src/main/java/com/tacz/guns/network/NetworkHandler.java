package com.tacz.guns.network;

import com.tacz.guns.network.message.*;
import com.tacz.guns.network.message.event.*;
import com.tacz.guns.network.message.handshake.Acknowledge;
import com.tacz.guns.network.message.handshake.ServerMessageSyncedEntityDataMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 1.21 网络层入口。
 * <p>
 * Forge 的 {@code SimpleChannel} / {@code NetworkRegistry.newSimpleChannel} 在 NeoForge 1.21 已整体移除，
 * 改为 {@link CustomPacketPayload} + {@code StreamCodec}，注册由 {@link RegisterPayloadHandlersEvent} 驱动。
 * 各消息类自行持有 {@code TYPE} 与 {@code STREAM_CODEC}，此处只做方向与 handler 绑定。
 * <p>
 * 注册入口见 {@code com.tacz.guns.init.CommonRegistry#onRegisterPayloads}。
 */
public class NetworkHandler {
    /**
     * 协议版本。{@code PayloadRegistrar} 会用它做双向版本协商，不一致的连接会被拒绝。
     */
    private static final String VERSION = "1.0.5";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        // ==================== 客户端 -> 服务端 ====================
        registrar.playToServer(ClientMessagePlayerShoot.TYPE, ClientMessagePlayerShoot.STREAM_CODEC, ClientMessagePlayerShoot::handle);
        registrar.playToServer(ClientMessagePlayerReloadGun.TYPE, ClientMessagePlayerReloadGun.STREAM_CODEC, ClientMessagePlayerReloadGun::handle);
        registrar.playToServer(ClientMessagePlayerCancelReload.TYPE, ClientMessagePlayerCancelReload.STREAM_CODEC, ClientMessagePlayerCancelReload::handle);
        registrar.playToServer(ClientMessagePlayerFireSelect.TYPE, ClientMessagePlayerFireSelect.STREAM_CODEC, ClientMessagePlayerFireSelect::handle);
        registrar.playToServer(ClientMessagePlayerAim.TYPE, ClientMessagePlayerAim.STREAM_CODEC, ClientMessagePlayerAim::handle);
        registrar.playToServer(ClientMessagePlayerCrawl.TYPE, ClientMessagePlayerCrawl.STREAM_CODEC, ClientMessagePlayerCrawl::handle);
        registrar.playToServer(ClientMessagePlayerDrawGun.TYPE, ClientMessagePlayerDrawGun.STREAM_CODEC, ClientMessagePlayerDrawGun::handle);
        registrar.playToServer(ClientMessageCraft.TYPE, ClientMessageCraft.STREAM_CODEC, ClientMessageCraft::handle);
        registrar.playToServer(ClientMessagePlayerZoom.TYPE, ClientMessagePlayerZoom.STREAM_CODEC, ClientMessagePlayerZoom::handle);
        registrar.playToServer(ClientMessageRefitGun.TYPE, ClientMessageRefitGun.STREAM_CODEC, ClientMessageRefitGun::handle);
        registrar.playToServer(ClientMessageUnloadAttachment.TYPE, ClientMessageUnloadAttachment.STREAM_CODEC, ClientMessageUnloadAttachment::handle);
        registrar.playToServer(ClientMessagePlayerBoltGun.TYPE, ClientMessagePlayerBoltGun.STREAM_CODEC, ClientMessagePlayerBoltGun::handle);
        registrar.playToServer(ClientMessagePlayerMelee.TYPE, ClientMessagePlayerMelee.STREAM_CODEC, ClientMessagePlayerMelee::handle);
        registrar.playToServer(ClientMessageSyncBaseTimestamp.TYPE, ClientMessageSyncBaseTimestamp.STREAM_CODEC, ClientMessageSyncBaseTimestamp::handle);
        registrar.playToServer(ClientMessageLaserColor.TYPE, ClientMessageLaserColor.STREAM_CODEC, ClientMessageLaserColor::handle);

        // ==================== 服务端 -> 客户端 ====================
        registrar.playToClient(ServerMessageSound.TYPE, ServerMessageSound.STREAM_CODEC, ServerMessageSound::handle);
        registrar.playToClient(ServerMessageCraft.TYPE, ServerMessageCraft.STREAM_CODEC, ServerMessageCraft::handle);
        registrar.playToClient(ServerMessageRefreshRefitScreen.TYPE, ServerMessageRefreshRefitScreen.STREAM_CODEC, ServerMessageRefreshRefitScreen::handle);
        registrar.playToClient(ServerMessageSwapItem.TYPE, ServerMessageSwapItem.STREAM_CODEC, ServerMessageSwapItem::handle);
        registrar.playToClient(ServerMessageLevelUp.TYPE, ServerMessageLevelUp.STREAM_CODEC, ServerMessageLevelUp::handle);
        registrar.playToClient(ServerMessageGunHurt.TYPE, ServerMessageGunHurt.STREAM_CODEC, ServerMessageGunHurt::handle);
        registrar.playToClient(ServerMessageGunKill.TYPE, ServerMessageGunKill.STREAM_CODEC, ServerMessageGunKill::handle);
        registrar.playToClient(ServerMessageUpdateEntityData.TYPE, ServerMessageUpdateEntityData.STREAM_CODEC, ServerMessageUpdateEntityData::handle);
        registrar.playToClient(ServerMessageSyncGunPack.TYPE, ServerMessageSyncGunPack.STREAM_CODEC, ServerMessageSyncGunPack::handle);
        registrar.playToClient(ServerMessageGunDraw.TYPE, ServerMessageGunDraw.STREAM_CODEC, ServerMessageGunDraw::handle);
        registrar.playToClient(ServerMessageGunFire.TYPE, ServerMessageGunFire.STREAM_CODEC, ServerMessageGunFire::handle);
        registrar.playToClient(ServerMessageGunFireSelect.TYPE, ServerMessageGunFireSelect.STREAM_CODEC, ServerMessageGunFireSelect::handle);
        registrar.playToClient(ServerMessageGunMelee.TYPE, ServerMessageGunMelee.STREAM_CODEC, ServerMessageGunMelee::handle);
        registrar.playToClient(ServerMessageGunReload.TYPE, ServerMessageGunReload.STREAM_CODEC, ServerMessageGunReload::handle);
        registrar.playToClient(ServerMessageGunShoot.TYPE, ServerMessageGunShoot.STREAM_CODEC, ServerMessageGunShoot::handle);
        registrar.playToClient(ServerMessageSyncBaseTimestamp.TYPE, ServerMessageSyncBaseTimestamp.STREAM_CODEC, ServerMessageSyncBaseTimestamp::handle);

        // ==================== 实体数据映射握手 ====================
        // 原实现挂在 Forge 的登录阶段（loginIndex + markAsLoginPacket）。1.21 只允许 configuration / play
        // 两类自定义 payload，故迁移到 play 阶段：玩家登录后由 NetworkEvent 下发映射，客户端回 Acknowledge。
        registrar.playToClient(ServerMessageSyncedEntityDataMapping.TYPE, ServerMessageSyncedEntityDataMapping.STREAM_CODEC, ServerMessageSyncedEntityDataMapping::handle);
        registrar.playToServer(Acknowledge.TYPE, Acknowledge.STREAM_CODEC, Acknowledge::handle);
    }

    // ==================== 发送工具 ====================

    public static void sendToServer(CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }

    public static void sendToClientPlayer(CustomPacketPayload message, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, message);
        }
    }

    /**
     * 发送给所有监听此实体的玩家
     */
    public static void sendToTrackingEntityAndSelf(Entity centerEntity, CustomPacketPayload message) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(centerEntity, message);
    }

    public static void sendToAllPlayers(CustomPacketPayload message) {
        PacketDistributor.sendToAllPlayers(message);
    }

    public static void sendToTrackingEntity(CustomPacketPayload message, Entity centerEntity) {
        PacketDistributor.sendToPlayersTrackingEntity(centerEntity, message);
    }

    public static void sendToDimension(CustomPacketPayload message, Entity centerEntity) {
        if (centerEntity.level() instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersInDimension(serverLevel, message);
        }
    }
}
