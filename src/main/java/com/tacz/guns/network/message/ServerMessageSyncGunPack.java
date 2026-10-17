package com.tacz.guns.network.message;

import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class ServerMessageSyncGunPack implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageSyncGunPack> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_message_sync_gun_pack"));
    public static final StreamCodec<FriendlyByteBuf, ServerMessageSyncGunPack> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerMessageSyncGunPack::decode);

    @Override
    public CustomPacketPayload.Type<ServerMessageSyncGunPack> type() {
        return TYPE;
    }

    private final Map<DataType, Map<ResourceLocation, String>> cache;

    public ServerMessageSyncGunPack(Map<DataType, Map<ResourceLocation, String>> cache) {
        this.cache = cache;
    }

    public static void encode(ServerMessageSyncGunPack message, FriendlyByteBuf buf) {
        buf.writeMap(message.getCache(),
                (buf1, type) -> buf1.writeEnum(type),
                (buf1, map) -> buf1.writeMap(map,
                        (buf2, id) -> buf2.writeResourceLocation(id),
                        (buf2, value) -> buf2.writeUtf(value)));
    }

    public static ServerMessageSyncGunPack decode(FriendlyByteBuf buf) {
        var map = buf.readMap(
                buf1 -> buf1.readEnum(DataType.class),
                buf2 -> buf2.readMap(buf1 -> buf1.readResourceLocation(), buf1 -> buf1.readUtf()));
        return new ServerMessageSyncGunPack(map);
    }

    public static void handle(ServerMessageSyncGunPack message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            boolean remoteConnection = context.connection() != null && !context.connection().isMemoryConnection();
            context.enqueueWork(() -> doSync(message, remoteConnection));
        }
    }


    public Map<DataType, Map<ResourceLocation, String>> getCache() {
        return cache;
    }

    @OnlyIn(Dist.CLIENT)
    private static void doSync(ServerMessageSyncGunPack message, boolean remoteConnection) {
        if (remoteConnection) {
            CommonAssetsManager.clearInstance();
        }
        CommonNetworkCache.INSTANCE.fromNetwork(message.cache);
        // 通知客户端重新构建ClientIndex
        ClientIndexManager.reload();
    }
}
