package com.tacz.guns.network.message;

import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientMessageCraft implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessageCraft> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_craft"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessageCraft> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessageCraft::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessageCraft> type() {
        return TYPE;
    }

    private final ResourceLocation recipeId;
    private final int menuId;

    public ClientMessageCraft(ResourceLocation recipeId, int menuId) {
        this.recipeId = recipeId;
        this.menuId = menuId;
    }

    public static void encode(ClientMessageCraft message, FriendlyByteBuf buf) {
        buf.writeResourceLocation(message.recipeId);
        buf.writeVarInt(message.menuId);
    }

    public static ClientMessageCraft decode(FriendlyByteBuf buf) {
        return new ClientMessageCraft(buf.readResourceLocation(), buf.readVarInt());
    }

    public static void handle(ClientMessageCraft message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer entity)) {
                    return;
                }
                if (entity.containerMenu.containerId == message.menuId && entity.containerMenu instanceof GunSmithTableMenu menu) {
                    menu.doCraft(message.recipeId, entity);
                }
            });
        }
    }
}
