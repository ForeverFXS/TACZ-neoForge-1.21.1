package com.tacz.guns.compat.kubejs.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

public abstract class TimelessItemNbtFactory<T extends Item, S extends TimelessItemNbtFactory<T, S>> {
    protected Item item;
    protected ResourceLocation id;
    protected int count = 1;

    public TimelessItemNbtFactory(@NotNull T item) {
        this.item = item;
    }

    public S setId(ResourceLocation newId) {
        this.id = newId;
        return (S) this;
    }

    public S setCount(int count) {
        this.count = Math.max(count, 1);
        return (S) this;
    }

    public abstract ItemStack build();
}
