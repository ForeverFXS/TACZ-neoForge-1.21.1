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

public class ClientMessagePlayerReloadGun implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePlayerReloadGun> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_player_reload_gun"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessagePlayerReloadGun> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessagePlayerReloadGun::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessagePlayerReloadGun> type() {
        return TYPE;
    }

    public ClientMessagePlayerReloadGun() {
    }

    public static void encode(ClientMessagePlayerReloadGun message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerReloadGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerReloadGun();
    }

    public static void handle(ClientMessagePlayerReloadGun message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer entity)) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).reload();
            });
        }
    }
}
