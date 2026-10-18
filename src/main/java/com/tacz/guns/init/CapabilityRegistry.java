package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.sync.core.DataHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 1.21 移除了 Forge 的 {@code Capability} / {@code CapabilityToken} / {@code ICapabilityProvider} 体系。
 * <p>
 * 原先挂在实体上的 DataHolder 改为原版附件（Attachment）：
 * {@link DataHolder} 实现 {@code INBTSerializable<ListTag>}，
 * 因此可以直接用 {@code AttachmentType.serializable(...)} 注册，不再需要单独的 provider 类。
 * 取用方式也由 {@code entity.getCapability(...).resolve()} 变为 {@code entity.getData(DATA_HOLDER)}。
 */
public class CapabilityRegistry {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GunMod.MOD_ID);

    public static final Supplier<AttachmentType<DataHolder>> DATA_HOLDER =
            ATTACHMENT_TYPES.register("data_holder", () -> AttachmentType.serializable(DataHolder::new).build());

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
