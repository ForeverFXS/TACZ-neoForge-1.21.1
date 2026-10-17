package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.GunFireSelectEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.LogicalSide;

import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class ServerMessageGunFireSelect implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageGunFireSelect> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_message_gun_fire_select"));
    public static final StreamCodec<FriendlyByteBuf, ServerMessageGunFireSelect> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerMessageGunFireSelect::decode);

    @Override
    public CustomPacketPayload.Type<ServerMessageGunFireSelect> type() {
        return TYPE;
    }

    private final int shooterId;
    private final ItemStack gunItemStack;

    public ServerMessageGunFireSelect(int shooterId, ItemStack gunItemStack) {
        this.shooterId = shooterId;
        this.gunItemStack = gunItemStack;
    }

    public static void encode(ServerMessageGunFireSelect message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, message.gunItemStack);
    }

    public static ServerMessageGunFireSelect decode(FriendlyByteBuf buf) {
        int shooterId = buf.readVarInt();
        ItemStack gunItemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        return new ServerMessageGunFireSelect(shooterId, gunItemStack);
    }

    public static void handle(ServerMessageGunFireSelect message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> doClientEvent(message));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void doClientEvent(ServerMessageGunFireSelect message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.shooterId) instanceof LivingEntity shooter) {
            GunFireSelectEvent gunFireSelectEvent = new GunFireSelectEvent(shooter, message.gunItemStack, LogicalSide.CLIENT);
            NeoForge.EVENT_BUS.post(gunFireSelectEvent);
        }
    }
}
