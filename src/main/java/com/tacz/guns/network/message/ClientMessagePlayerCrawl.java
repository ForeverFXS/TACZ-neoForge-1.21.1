package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.config.sync.SyncConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientMessagePlayerCrawl implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessagePlayerCrawl> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_player_crawl"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessagePlayerCrawl> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessagePlayerCrawl::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessagePlayerCrawl> type() {
        return TYPE;
    }

    private final boolean isCrawl;

    public ClientMessagePlayerCrawl(boolean isCrawl) {
        this.isCrawl = isCrawl;
    }

    public static void encode(ClientMessagePlayerCrawl message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.isCrawl);
    }

    public static ClientMessagePlayerCrawl decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerCrawl(buf.readBoolean());
    }

    public static void handle(ClientMessagePlayerCrawl message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer entity)) {
                    return;
                }
                if (!SyncConfig.ENABLE_CRAWL.get()) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).crawl(message.isCrawl);
            });
        }
    }
}
