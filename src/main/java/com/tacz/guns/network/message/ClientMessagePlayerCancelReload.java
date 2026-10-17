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

public class ClientMessagePlayerCancelReload implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePlayerCancelReload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_player_cancel_reload"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessagePlayerCancelReload> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessagePlayerCancelReload::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessagePlayerCancelReload> type() {
        return TYPE;
    }

    public ClientMessagePlayerCancelReload() {
    }

    public static void encode(ClientMessagePlayerCancelReload message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerCancelReload decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerCancelReload();
    }

    public static void handle(ClientMessagePlayerCancelReload message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer entity)) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).cancelReload();
            });
        }
    }
}
