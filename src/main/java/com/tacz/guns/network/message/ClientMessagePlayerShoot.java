package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientMessagePlayerShoot implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePlayerShoot> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_player_shoot"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessagePlayerShoot> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessagePlayerShoot::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessagePlayerShoot> type() {
        return TYPE;
    }

    /**
     * 这里的 timestamp 应该是基于 base timestamp 的相对值
     */
    private long timestamp;
    private float chargeProgress;

    public ClientMessagePlayerShoot() {
    }

    public ClientMessagePlayerShoot(long timestamp) {
        this(timestamp, 0f);
    }

    public ClientMessagePlayerShoot(long timestamp, float chargeProgress) {
        this.timestamp = timestamp;
        this.chargeProgress = chargeProgress;
    }

    public static void encode(ClientMessagePlayerShoot message, FriendlyByteBuf buf) {
        buf.writeLong(message.timestamp);
        buf.writeFloat(message.chargeProgress);
    }

    public static ClientMessagePlayerShoot decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerShoot(buf.readLong(), buf.readFloat());
    }

    public static void handle(ClientMessagePlayerShoot message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer entity)) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).shoot(entity::getXRot, entity::getYRot, message.timestamp, message.chargeProgress);
            });
        }
    }
}
