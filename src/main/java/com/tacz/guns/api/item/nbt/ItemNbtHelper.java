package com.tacz.guns.api.item.nbt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 1.21 的 {@link ItemStack} 不再提供 {@code getOrCreateTag()} / {@code getTag()} / {@code setTag()}，
 * 物品 NBT 改由数据组件承载。
 * <p>
 * 这里用原版 {@link DataComponents#CUSTOM_DATA}（{@link CustomData}）承载任意 NBT。
 * 关键点是 {@link CustomData#getUnsafe()}：它返回内部 {@link CompoundTag} 的引用而不是副本，
 * 因此可以完整保留 1.20.1 时代“拿到 tag 直接改就生效”的语义，调用点无需逐个补写回，
 * 也不会引入 {@code CustomData.of()} / {@code copyTag()} 那样的隐式复制开销。
 * <p>
 * 注意：该方法不触发 {@code ItemStack} 的同步变更标记，与原版行为一致；
 * 需要显式同步时应使用 {@link #setTag(ItemStack, CompoundTag)}。
 */
public final class ItemNbtHelper {
    private ItemNbtHelper() {
    }

    /**
     * 等价于 1.20.1 的 {@code ItemStack#getTag()}：不存在时返回 {@code null}。
     */
    public static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.getUnsafe();
    }

    /**
     * 等价于 1.20.1 的 {@code ItemStack#getOrCreateTag()}：不存在时创建空 tag 并写回。
     * <p>
     * 返回的是内部引用，直接修改立即生效。
     */
    public static CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
            data = stack.get(DataComponents.CUSTOM_DATA);
        }
        return data.getUnsafe();
    }

    public static boolean hasTag(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    /**
     * 等价于 1.20.1 的 {@code ItemStack#setTag(CompoundTag)}。
     */
    public static void setTag(ItemStack stack, CompoundTag tag) {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static void removeTag(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }
}
