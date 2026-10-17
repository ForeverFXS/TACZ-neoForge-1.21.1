package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerMessageSyncBaseTimestamp implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageSyncBaseTimestamp> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_message_sync_base_timestamp"));
    public static final StreamCodec<FriendlyByteBuf, ServerMessageSyncBaseTimestamp> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerMessageSyncBaseTimestamp::decode);

    @Override
    public CustomPacketPayload.Type<ServerMessageSyncBaseTimestamp> type() {
        return TYPE;
    }

    private static final Marker MARKER = MarkerManager.getMarker("SYNC_BASE_TIMESTAMP");

    public ServerMessageSyncBaseTimestamp() { }

    public static void encode(ServerMessageSyncBaseTimestamp message, FriendlyByteBuf buf) { }

    public static ServerMessageSyncBaseTimestamp decode(FriendlyByteBuf buf) {
        return new ServerMessageSyncBaseTimestamp();
    }

    public static void handle(ServerMessageSyncBaseTimestamp message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            long timestamp = System.currentTimeMillis();
            context.enqueueWork(() -> updateBaseTimestamp(timestamp));
        }
        context.reply(new ClientMessageSyncBaseTimestamp());
    }

    @OnlyIn(Dist.CLIENT)
    private static void updateBaseTimestamp(long timestamp) {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        LocalPlayerDataHolder dataHolder = IClientPlayerGunOperator.fromLocalPlayer(player).getDataHolder();
        dataHolder.clientBaseTimestamp = timestamp;
        GunMod.LOGGER.debug(MARKER, "Update client base timestamp: {}", dataHolder.clientBaseTimestamp);
    }
}
