package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.function.Supplier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientMessageSyncBaseTimestamp implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessageSyncBaseTimestamp> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_sync_base_timestamp"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessageSyncBaseTimestamp> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessageSyncBaseTimestamp::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessageSyncBaseTimestamp> type() {
        return TYPE;
    }

    private static final Marker MARKER = MarkerManager.getMarker("SYNC_BASE_TIMESTAMP");

    public ClientMessageSyncBaseTimestamp() { }

    public static void encode(ClientMessageSyncBaseTimestamp message, FriendlyByteBuf buf) { }

    public static ClientMessageSyncBaseTimestamp decode(FriendlyByteBuf buf) {
        return new ClientMessageSyncBaseTimestamp();
    }

    public static void handle(ClientMessageSyncBaseTimestamp message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            long timestamp = System.currentTimeMillis();
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer entity)) {
                    return;
                }
                ShooterDataHolder dataHolder = IGunOperator.fromLivingEntity(entity).getDataHolder();
                dataHolder.baseTimestamp = timestamp;
                GunMod.LOGGER.debug(MARKER, "Update server base timestamp: {}", dataHolder.baseTimestamp);
            });
        }
    }
}
