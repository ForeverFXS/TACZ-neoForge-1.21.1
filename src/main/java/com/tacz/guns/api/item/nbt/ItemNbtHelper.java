package com.tacz.guns.api.item.nbt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 1.21 的 {@link ItemStack} 不再提供 {@code getOrCreateTag()} / {@code getTag()} / {@code setTag()}，
 * 物品 NBT 改由数据组件承载。这里用原版 {@link DataComponents#CUSTOM_DATA}（{@link CustomData}）承载任意 NBT。
 * <p>
 * <b>安全说明（关键，勿简化）</b>：
 * 原版 {@code PatchedDataComponentMap.copy()} 只复制 patch 表的引用并置 {@code copyOnWrite}，
 * 因此 {@code ItemStack.copy()} 之后两个栈仍<b>共享同一个 {@link CustomData} 实例</b>。
 * 而 {@link CustomData#getUnsafe()} 直接返回内部 {@link CompoundTag} 字段，绕过 copyOnWrite 保护，
 * 直接修改会串到所有共享该组件的栈上（典型的“改背包里的枪，手上的枪一起变”甚至污染存档）。
 * <p>
 * 因此 {@link #getOrCreateTag(ItemStack)} 每次都重新 {@code set} 一个<b>独立</b>的 CustomData 实例：
 * 返回的引用只属于当前栈，修改不会波及副本。代价是每次调用有一次 NBT 复制，
 * 换取的是与原版强化物品相同的隔离语义。
 */
public final class ItemNbtHelper {
    private ItemNbtHelper() {
    }

    /**
     * 等价于 1.20.1 的 {@code ItemStack#getTag()}：不存在时返回 {@code null}。
     * <p>
     * 返回的是<b>副本</b>，只读使用；需要据其写回时请走 {@link #setTag(ItemStack, CompoundTag)}。
     */
    public static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    /**
     * 等价于 1.20.1 的 {@code ItemStack#getOrCreateTag()}。
     * <p>
     * 返回的引用属于当前栈独占的 CustomData 实例，直接修改立即对当前栈生效，
     * 且不会影响任何通过 {@code copy()} 得到的栈。
     */
    public static CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing == null ? new CompoundTag() : existing.copyTag();
        // CustomData.of 内部再复制一次，得到与原组件完全无关的新实例
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack.get(DataComponents.CUSTOM_DATA).getUnsafe();
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
