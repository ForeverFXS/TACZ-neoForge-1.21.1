package com.tacz.guns.crafting;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 工作台配方序列化器。
 * <p>
 * 1.21 的 {@link RecipeSerializer} 改为 {@link #codec()} + {@link #streamCodec()}：
 * 前者用 {@link MapCodec} 描述数据文件，后者用 {@link StreamCodec} 描述网络同步。
 * <p>
 * 数据文件仍沿用 TACZ 的 GSON POJO（{@link TableRecipe}），
 * 这里通过 {@link JsonOps} 在 MapLike 与 JsonElement 之间转换，避免重写整套 codec。
 * 配方的 {@code id} 在 1.21 由 {@code RecipeHolder} 携带，解码时传 {@code null}，
 * 由调用方（配方列表遍历处）回写。
 */
public class GunSmithTableSerializer implements RecipeSerializer<GunSmithTableRecipe> {
    public static final MapCodec<GunSmithTableRecipe> CODEC = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString("materials"), ops.createString("result"));
        }

        @Override
        public <T> DataResult<GunSmithTableRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
            try {
                JsonElement json = ops.convertTo(JsonOps.INSTANCE, ops.createMap(input.entries()));
                TableRecipe tableRecipe = CommonAssetsManager.GSON.fromJson(json, TableRecipe.class);
                if (tableRecipe == null) {
                    return DataResult.error(() -> "Failed to parse gun smith table recipe");
                }
                return DataResult.success(new GunSmithTableRecipe(null, tableRecipe));
            } catch (Exception e) {
                return DataResult.error(() -> "Failed to parse gun smith table recipe: " + e.getMessage());
            }
        }

        @Override
        public <T> RecordBuilder<T> encode(GunSmithTableRecipe input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            // 配方在本 mod 内只读，不做回写编码
            return prefix;
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, GunSmithTableRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                buf.writeInt(recipe.getInputs().size());
                for (GunSmithTableIngredient ingredient : recipe.getInputs()) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient.getIngredient());
                    buf.writeInt(ingredient.getCount());
                }
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.getResult().getResult());
                buf.writeResourceLocation(recipe.getResult().getGroup());
            },
            buf -> {
                int size = buf.readInt();
                List<GunSmithTableIngredient> ingredients = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                    ingredients.add(new GunSmithTableIngredient(ingredient, buf.readInt()));
                }
                ItemStack resultItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                ResourceLocation group = buf.readResourceLocation();
                GunSmithTableResult result = new GunSmithTableResult(resultItem, group);
                return new GunSmithTableRecipe(null, result, ingredients);
            });

    @Override
    public MapCodec<GunSmithTableRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, GunSmithTableRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
