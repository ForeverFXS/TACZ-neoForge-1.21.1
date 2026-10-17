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

public class ClientMessagePlayerAim implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePlayerAim> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_player_aim"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessagePlayerAim> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessagePlayerAim::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessagePlayerAim> type() {
        return TYPE;
    }

    private final boolean isAim;

    public ClientMessagePlayerAim(boolean isAim) {
        this.isAim = isAim;
    }

    public static void encode(ClientMessagePlayerAim message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.isAim);
    }

    public static ClientMessagePlayerAim decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerAim(buf.readBoolean());
    }

    public static void handle(ClientMessagePlayerAim message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer entity)) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).aim(message.isAim);
            });
        }
    }
}
