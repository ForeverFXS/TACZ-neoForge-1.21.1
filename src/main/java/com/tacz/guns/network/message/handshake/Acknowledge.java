package com.tacz.guns.network.message.handshake;

import com.tacz.guns.GunMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * 客户端对实体数据映射的确认包。
 * <p>
 * 原实现依赖 Forge 登录阶段（loginIndex + markAsLoginPacket），1.21 已无自定义 login payload，
 * 现作为 play 阶段 {@code playToServer} 包使用，内容为空。
 */
public class Acknowledge implements CustomPacketPayload {
    public static final Marker ACKNOWLEDGE = MarkerManager.getMarker("HANDSHAKE_ACKNOWLEDGE");

    public static final CustomPacketPayload.Type<Acknowledge> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "acknowledge"));

    public static final StreamCodec<FriendlyByteBuf, Acknowledge> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> {
            }, buf -> new Acknowledge());

    @Override
    public CustomPacketPayload.Type<Acknowledge> type() {
        return TYPE;
    }

    public static void handle(Acknowledge message, IPayloadContext context) {
        GunMod.LOGGER.debug(ACKNOWLEDGE, "Received acknowledgement from client");
    }
}
