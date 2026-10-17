package com.tacz.guns.network.message;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;
import com.tacz.guns.GunMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientMessageUnloadAttachment implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientMessageUnloadAttachment> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "client_message_unload_attachment"));
    public static final StreamCodec<FriendlyByteBuf, ClientMessageUnloadAttachment> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientMessageUnloadAttachment::decode);

    @Override
    public CustomPacketPayload.Type<ClientMessageUnloadAttachment> type() {
        return TYPE;
    }

    private final int gunSlotIndex;
    private final AttachmentType attachmentType;

    public ClientMessageUnloadAttachment(int gunSlotIndex, AttachmentType attachmentType) {
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentType = attachmentType;
    }

    public static void encode(ClientMessageUnloadAttachment message, FriendlyByteBuf buf) {
        buf.writeInt(message.gunSlotIndex);
        buf.writeEnum(message.attachmentType);
    }

    public static ClientMessageUnloadAttachment decode(FriendlyByteBuf buf) {
        return new ClientMessageUnloadAttachment(buf.readInt(), buf.readEnum(AttachmentType.class));
    }

    public static void handle(ClientMessageUnloadAttachment message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                Inventory inventory = player.getInventory();
                ItemStack gunItem = inventory.getItem(message.gunSlotIndex);
                IGun iGun = IGun.getIGunOrNull(gunItem);
                if (iGun != null) {
                    // 服务端校验配件锁
                    if (iGun.hasAttachmentLock(gunItem)) {
                        return;
                    }
                    ItemStack attachmentItem = iGun.getAttachment(gunItem, message.attachmentType);
                    if (!attachmentItem.isEmpty() && inventory.add(attachmentItem)) {
                        iGun.unloadAttachment(gunItem, message.attachmentType);
                        // 刷新配件数据
                        AttachmentPropertyManager.postChangeEvent(player, gunItem);
                        // 如果卸载的是扩容弹匣，吐出所有子弹
                        if (message.attachmentType == AttachmentType.EXTENDED_MAG) {
                            iGun.dropAllAmmo(player, gunItem);
                        }
                        player.inventoryMenu.broadcastChanges();
                        NetworkHandler.sendToClientPlayer(new ServerMessageRefreshRefitScreen(), player);
                    }
                }
            });
        }
    }

}
