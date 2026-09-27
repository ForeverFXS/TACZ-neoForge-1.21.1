package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModPainting {
    public static final DeferredRegister<PaintingVariant> PAINTINGS = DeferredRegister.create(Registries.PAINTING_VARIANT, GunMod.MOD_ID);

    public static final DeferredHolder<PaintingVariant, PaintingVariant> BLOOD_STRIKE_1 = PAINTINGS.register("blood_strike_1", () -> new PaintingVariant(32, 32));
//    public static final DeferredHolder<PaintingVariant, PaintingVariant> BLOOD_STRIKE_2 = PAINTINGS.register("blood_strike_2", () -> new PaintingVariant(32, 32));
}
