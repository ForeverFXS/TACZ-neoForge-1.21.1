package com.tacz.guns.network.message;

import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerMessageCraft implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageCraft> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_message_craft"));
    public static final StreamCodec<FriendlyByteBuf, ServerMessageCraft> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerMessageCraft::decode);

    @Override
    public CustomPacketPayload.Type<ServerMessageCraft> type() {
        return TYPE;
    }

    private final int menuId;

    public ServerMessageCraft(int menuId) {
        this.menuId = menuId;
    }

    public static void encode(ServerMessageCraft message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.menuId);
    }

    public static ServerMessageCraft decode(FriendlyByteBuf buf) {
        return new ServerMessageCraft(buf.readVarInt());
    }

    public static void handle(ServerMessageCraft message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> updateScreen(message.menuId));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void updateScreen(int containerId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu.containerId == containerId && Minecraft.getInstance().screen instanceof GunSmithTableScreen screen) {
            screen.updateIngredientCount();
        }
    }
}
