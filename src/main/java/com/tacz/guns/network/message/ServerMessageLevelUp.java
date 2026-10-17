package com.tacz.guns.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class ServerMessageLevelUp implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerMessageLevelUp> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "server_message_level_up"));
    public static final StreamCodec<FriendlyByteBuf, ServerMessageLevelUp> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerMessageLevelUp::decode);

    @Override
    public CustomPacketPayload.Type<ServerMessageLevelUp> type() {
        return TYPE;
    }

    private final ItemStack gun;
    private final int level;

    public ServerMessageLevelUp(ItemStack gun, int level) {
        this.gun = gun;
        this.level = level;
    }

    public static void encode(ServerMessageLevelUp message, FriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, message.gun);
        buf.writeInt(message.level);
    }

    public static ServerMessageLevelUp decode(FriendlyByteBuf buf) {
        ItemStack gun = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        int level = buf.readInt();
        return new ServerMessageLevelUp(gun, level);
    }

    public static void handle(ServerMessageLevelUp message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> onLevelUp(message));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void onLevelUp(ServerMessageLevelUp message) {
        int level = message.getLevel();
        ItemStack gun = message.getGun();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        // TODO 在完成了枪械升级逻辑后，解封下面的代码
                /*
                if (GunLevelManager.DAMAGE_UP_LEVELS.contains(level)) {
                    Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                            Component.translatable("toast.tacz.level_up"),
                            Component.translatable("toast.tacz.sub.damage_up")));
                } else if (level >= GunLevelManager.MAX_LEVEL) {
                    Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                            Component.translatable("toast.tacz.level_up"),
                            Component.translatable("toast.tacz.sub.final_level")));
                } else {
                    Minecraft.getInstance().getToasts().addToast(new GunLevelUpToast(gun,
                            Component.translatable("toast.tacz.level_up"),
                            Component.translatable("toast.tacz.sub.level_up")));
                }*/
    }

    public ItemStack getGun() {
        return this.gun;
    }

    public int getLevel() {
        return this.level;
    }
}
