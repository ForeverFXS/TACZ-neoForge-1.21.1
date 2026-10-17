package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.GunShootEvent;
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

public class ServerMessageGunShoot implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageGunShoot> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_message_gun_shoot"));
    public static final StreamCodec<FriendlyByteBuf, ServerMessageGunShoot> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerMessageGunShoot::decode);

    @Override
    public CustomPacketPayload.Type<ServerMessageGunShoot> type() {
        return TYPE;
    }

    private final int shooterId;
    private final ItemStack gunItemStack;

    public ServerMessageGunShoot(int shooterId, ItemStack gunItemStack) {
        this.shooterId = shooterId;
        this.gunItemStack = gunItemStack;
    }

    public static void encode(ServerMessageGunShoot message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, message.gunItemStack);
    }

    public static ServerMessageGunShoot decode(FriendlyByteBuf buf) {
        int shooterId = buf.readVarInt();
        ItemStack gunItemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        return new ServerMessageGunShoot(shooterId, gunItemStack);
    }

    public static void handle(ServerMessageGunShoot message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> doClientEvent(message));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void doClientEvent(ServerMessageGunShoot message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.shooterId) instanceof LivingEntity shooter) {
            GunShootEvent gunShootEvent = new GunShootEvent(shooter, message.gunItemStack, LogicalSide.CLIENT);
            NeoForge.EVENT_BUS.post(gunShootEvent);
        }
    }
}
