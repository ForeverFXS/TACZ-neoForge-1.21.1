package com.tacz.guns.compat.kubejs.recipe;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.kubejs.util.GunSmithTableResultInfo;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * TACZ 砧台配方（{@code tacz:gun_smith_table_crafting}）的 KubeJS schema。
 * <p>
 * 2001 的写法是 {@code new RecipeSchema(TimelessRecipeJS.class, TimelessRecipeJS::new, RESULT, MATERIALS)}，
 * 2101 的 {@link RecipeSchema} 构造只接受 {@code RecipeKey<?>...}（javap 实证），自定义配方类改由
 * {@link KubeRecipeFactory} 注入（{@code KubeRecipeFactory(ResourceLocation, Class<?>, Supplier)}）。
 */
public class TimelessGunSmithTableRecipeSchema {
    /**
     * recipe type 字符串（注册到 {@code tacz} 命名空间下的名字）。
     * 完整类型 = {@code tacz:gun_smith_table_crafting}，与 datapack/生成 JSON 的 {@code "type"} 一致。
     */
    public static final String RECIPE_TYPE = "gun_smith_table_crafting";

    public static final ResourceLocation RECIPE_TYPE_ID = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, RECIPE_TYPE);

    public static final RecipeKey<GunSmithTableResultInfo> RESULT =
            GunSmithTableResultComponents.RESULT_INFO.outputKey("result");

    /**
     * materials 的元素是 {@code {"item": <原版 Ingredient>, "count": n}}，不是裸 Ingredient，
     * 所以用 TACZ 自己的组件而不是 2101 的 {@code IngredientComponent}。
     */
    public static final RecipeKey<List<GunSmithTableIngredient>> MATERIALS =
            GunSmithTableResultComponents.MATERIALS.inputKey("materials");

    public static final RecipeSchema SCHEMA = new RecipeSchema(RESULT, MATERIALS)
            .factory(new KubeRecipeFactory(RECIPE_TYPE_ID, TimelessRecipeJS.class, TimelessRecipeJS::new));

    private TimelessGunSmithTableRecipeSchema() {
    }
}
