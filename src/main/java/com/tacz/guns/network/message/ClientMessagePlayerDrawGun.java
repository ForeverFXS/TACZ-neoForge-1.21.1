package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientMessagePlayerDrawGun implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePlayerDrawGun> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_player_draw_gun"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessagePlayerDrawGun> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessagePlayerDrawGun::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessagePlayerDrawGun> type() {
        return TYPE;
    }

    public ClientMessagePlayerDrawGun() {
    }

    public static void encode(ClientMessagePlayerDrawGun message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerDrawGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerDrawGun();
    }

    public static void handle(ClientMessagePlayerDrawGun message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer entity)) {
                    return;
                }
                Inventory inventory = entity.getInventory();
                int selected = inventory.selected;
                IGunOperator.fromLivingEntity(entity).draw(() -> inventory.getItem(selected));
            });
        }
    }
}
