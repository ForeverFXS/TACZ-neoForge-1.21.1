package com.tacz.guns.network.message;

import com.tacz.guns.entity.sync.core.DataEntry;
import com.tacz.guns.entity.sync.core.SyncedEntityData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerMessageUpdateEntityData implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageUpdateEntityData> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_message_update_entity_data"));
    public static final StreamCodec<FriendlyByteBuf, ServerMessageUpdateEntityData> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerMessageUpdateEntityData::decode);

    @Override
    public CustomPacketPayload.Type<ServerMessageUpdateEntityData> type() {
        return TYPE;
    }

    private final int entityId;
    private final List<DataEntry<?, ?>> entries;

    public ServerMessageUpdateEntityData(int entityId, List<DataEntry<?, ?>> entries) {
        this.entityId = entityId;
        this.entries = entries;
    }

    public static void encode(ServerMessageUpdateEntityData message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeVarInt(message.entries.size());
        message.entries.forEach(entry -> entry.write(buffer));
    }

    public static ServerMessageUpdateEntityData decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        int size = buffer.readVarInt();
        List<DataEntry<?, ?>> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            entries.add(DataEntry.read(buffer));
        }
        return new ServerMessageUpdateEntityData(entityId, entries);
    }

    public static void handle(ServerMessageUpdateEntityData message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> onHandle(message));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(ServerMessageUpdateEntityData message) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(message.entityId);
        if (entity == null) {
            return;
        }
        SyncedEntityData instance = SyncedEntityData.instance();
        message.entries.forEach(entry -> instance.set(entity, entry.getKey(), entry.getValue()));
    }
}
