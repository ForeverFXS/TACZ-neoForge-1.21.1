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

public class ClientMessagePlayerBoltGun implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePlayerBoltGun> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_player_bolt_gun"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessagePlayerBoltGun> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessagePlayerBoltGun::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessagePlayerBoltGun> type() {
        return TYPE;
    }

    public ClientMessagePlayerBoltGun() {
    }

    public static void encode(ClientMessagePlayerBoltGun message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerBoltGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerBoltGun();
    }

    public static void handle(ClientMessagePlayerBoltGun message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer entity)) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).bolt();
            });
        }
    }
}
