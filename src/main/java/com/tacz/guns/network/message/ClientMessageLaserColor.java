package com.tacz.guns.network.message;

import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientMessageLaserColor implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessageLaserColor> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_laser_color"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessageLaserColor> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessageLaserColor::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessageLaserColor> type() {
        return TYPE;
    }

    private final Map<AttachmentType, Integer> colorMap = new HashMap<>();
    private boolean applyGunColor = false;
    private int gunColor = 0;

    private int gunSlotIndex = -1;

    private ClientMessageLaserColor() {
    }

    public ClientMessageLaserColor(@NotNull ItemStack gun, int gunSlotIndex) {
        if (gun.getItem() instanceof IGun iGun) {
            for (AttachmentType type : AttachmentType.values()) {
                ItemStack attachment = iGun.getAttachment(gun, type);
                if (attachment.getItem() instanceof IAttachment iAttachment) {
                    if (iAttachment.hasCustomLaserColor(attachment)) {
                        colorMap.put(type, iAttachment.getLaserColor(attachment));
                    }
                }
            }
            if (iGun.hasCustomLaserColor(gun)) {
                this.gunColor = iGun.getLaserColor(gun);
                this.applyGunColor = true;
            }
            this.gunSlotIndex = gunSlotIndex;
        }
    }

    public static void encode(ClientMessageLaserColor message, FriendlyByteBuf buf) {
        buf.writeMap(message.colorMap, (buf1, type) -> buf1.writeEnum(type), (buf1, color) -> buf1.writeInt(color));
        buf.writeBoolean(message.applyGunColor);
        buf.writeInt(message.gunColor);
        buf.writeInt(message.gunSlotIndex);
    }

    public static ClientMessageLaserColor decode(FriendlyByteBuf buf) {
        ClientMessageLaserColor message = new ClientMessageLaserColor();
        message.colorMap.putAll(buf.readMap(buf1 -> buf1.readEnum(AttachmentType.class), buf1 -> buf1.readInt()));
        message.applyGunColor = buf.readBoolean();
        message.gunColor = buf.readInt();
        message.gunSlotIndex = buf.readInt();
        return message;
    }

    public static void handle(ClientMessageLaserColor message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player) || message.gunSlotIndex == -1) {
                    return;
                }
                Inventory inventory = player.getInventory();
                ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
                IGun iGun = IGun.getIGunOrNull(gunItem);
                if (iGun != null) {
                    for (var entry : message.colorMap.entrySet()) {
                        AttachmentType type = entry.getKey();
                        int color = entry.getValue();
                        ItemStack attachment = iGun.getAttachment(gunItem, type);
                        if (attachment.getItem() instanceof IAttachment iAttachment) {
                            iAttachment.setLaserColor(attachment, color);
                        }
                    }
                    if (message.applyGunColor) {
                        iGun.setLaserColor(gunItem, message.gunColor);
                    }
                }
            });
        }
    }

}
