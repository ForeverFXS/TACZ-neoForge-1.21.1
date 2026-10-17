package com.tacz.guns.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 旧版 Forge 网络消息的三段式契约。
 * <p>
 * 1.21 起网络包统一走 {@code CustomPacketPayload} + {@code StreamCodec}，本接口仅作为
 * “编码 / 解码 / 处理”职责划分的说明性契约保留，不再被具体消息类实现。
 */
public interface IMessage<T> {
    void encode(T message, FriendlyByteBuf buffer);

    T decode(FriendlyByteBuf buffer);

    void handle(T message, IPayloadContext context);
}
