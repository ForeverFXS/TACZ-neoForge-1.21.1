package com.tacz.guns.network.message.handshake;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.sync.core.SyncedDataKey;
import com.tacz.guns.entity.sync.core.SyncedEntityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * 服务端 -> 客户端的 SyncedEntityData 键映射同步。
 * <p>
 * 客户端需要拿到服务端注册的每个 {@link SyncedDataKey} 及其内部 id，才能解码后续的实体数据包。
 * 原实现挂在 Forge 的登录阶段；1.21 无自定义 login payload，改为玩家登录后的 play 阶段包，
 * 由 {@code com.tacz.guns.event.NetworkEvent} 下发。
 */
public class ServerMessageSyncedEntityDataMapping implements CustomPacketPayload {
    public static final Marker HANDSHAKE = MarkerManager.getMarker("TACZ_HANDSHAKE");

    public static final CustomPacketPayload.Type<ServerMessageSyncedEntityDataMapping> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "synced_entity_data_mapping"));

    public static final StreamCodec<FriendlyByteBuf, ServerMessageSyncedEntityDataMapping> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), ServerMessageSyncedEntityDataMapping::decode);

    private Map<ResourceLocation, List<Pair<ResourceLocation, Integer>>> keyMap;

    public ServerMessageSyncedEntityDataMapping() {
    }

    private ServerMessageSyncedEntityDataMapping(Map<ResourceLocation, List<Pair<ResourceLocation, Integer>>> keyMap) {
        this.keyMap = keyMap;
    }

    @Override
    public CustomPacketPayload.Type<ServerMessageSyncedEntityDataMapping> type() {
        return TYPE;
    }

    public static void encode(ServerMessageSyncedEntityDataMapping message, FriendlyByteBuf buffer) {
        Set<SyncedDataKey<?, ?>> keys = SyncedEntityData.instance().getKeys();
        buffer.writeInt(keys.size());
        keys.forEach(key -> {
            int id = SyncedEntityData.instance().getInternalId(key);
            buffer.writeResourceLocation(key.classKey().id());
            buffer.writeResourceLocation(key.id());
            buffer.writeVarInt(id);
        });
    }

    public static ServerMessageSyncedEntityDataMapping decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Map<ResourceLocation, List<Pair<ResourceLocation, Integer>>> keyMap = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation classId = buffer.readResourceLocation();
            ResourceLocation keyId = buffer.readResourceLocation();
            int id = buffer.readVarInt();
            keyMap.computeIfAbsent(classId, c -> new ArrayList<>()).add(Pair.of(keyId, id));
        }
        return new ServerMessageSyncedEntityDataMapping(keyMap);
    }

    public static void handle(ServerMessageSyncedEntityDataMapping message, IPayloadContext context) {
        GunMod.LOGGER.debug(HANDSHAKE, "Received synced key mappings from server");
        CountDownLatch block = new CountDownLatch(1);
        context.enqueueWork(() -> {
            if (!SyncedEntityData.instance().updateMappings(message)) {
                context.disconnect(Component.literal("Connection closed - [TacZ] Received unknown synced data keys."));
            }
            block.countDown();
        });
        try {
            block.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        context.reply(new Acknowledge());
    }

    public Map<ResourceLocation, List<Pair<ResourceLocation, Integer>>> getKeyMap() {
        return this.keyMap;
    }
}
