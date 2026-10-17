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

public class ClientMessagePlayerZoom implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePlayerZoom> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_player_zoom"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessagePlayerZoom> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessagePlayerZoom::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessagePlayerZoom> type() {
        return TYPE;
    }

    public static void encode(ClientMessagePlayerZoom message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerZoom decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerZoom();
    }

    public static void handle(ClientMessagePlayerZoom message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer entity)) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).zoom();
            });
        }
    }
}
