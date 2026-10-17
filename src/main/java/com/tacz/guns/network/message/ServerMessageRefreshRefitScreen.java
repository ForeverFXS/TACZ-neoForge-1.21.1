package com.tacz.guns.network.message;

import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
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

public class ServerMessageRefreshRefitScreen implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageRefreshRefitScreen> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_message_refresh_refit_screen"));
    public static final StreamCodec<FriendlyByteBuf, ServerMessageRefreshRefitScreen> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerMessageRefreshRefitScreen::decode);

    @Override
    public CustomPacketPayload.Type<ServerMessageRefreshRefitScreen> type() {
        return TYPE;
    }

    public static void encode(ServerMessageRefreshRefitScreen message, FriendlyByteBuf buf) {
    }

    public static ServerMessageRefreshRefitScreen decode(FriendlyByteBuf buf) {
        return new ServerMessageRefreshRefitScreen();
    }

    public static void handle(ServerMessageRefreshRefitScreen message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(ServerMessageRefreshRefitScreen::updateScreen);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void updateScreen() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Minecraft.getInstance().screen instanceof GunRefitScreen screen) {
            screen.init();
            // 刷新配件数据，客户端的
            AttachmentPropertyManager.postChangeEvent(player, player.getMainHandItem());
        }
    }
}
