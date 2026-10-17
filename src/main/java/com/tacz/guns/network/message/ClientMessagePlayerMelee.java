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

public class ClientMessagePlayerMelee implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePlayerMelee> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_player_melee"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessagePlayerMelee> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessagePlayerMelee::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessagePlayerMelee> type() {
        return TYPE;
    }

    public static void encode(ClientMessagePlayerMelee message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerMelee decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerMelee();
    }

    public static void handle(ClientMessagePlayerMelee message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer entity)) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).melee();
            });
        }
    }
}
