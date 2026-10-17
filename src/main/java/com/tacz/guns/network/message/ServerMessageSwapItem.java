package com.tacz.guns.network.message;

import com.tacz.guns.api.client.event.SwapItemWithOffHand;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerMessageSwapItem implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageSwapItem> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_message_swap_item"));
    public static final StreamCodec<FriendlyByteBuf, ServerMessageSwapItem> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerMessageSwapItem::decode);

    @Override
    public CustomPacketPayload.Type<ServerMessageSwapItem> type() {
        return TYPE;
    }

    public ServerMessageSwapItem() {
    }

    public static void encode(ServerMessageSwapItem message, FriendlyByteBuf buf) {
    }

    public static ServerMessageSwapItem decode(FriendlyByteBuf buf) {
        return new ServerMessageSwapItem();
    }

    public static void handle(ServerMessageSwapItem message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                NeoForge.EVENT_BUS.post(new SwapItemWithOffHand());
            });
        }
    }
}
