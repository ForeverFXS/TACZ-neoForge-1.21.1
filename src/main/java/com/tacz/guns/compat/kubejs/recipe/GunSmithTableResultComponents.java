package com.tacz.guns.compat.kubejs.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.compat.kubejs.util.GunSmithTableResultInfo;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.SimpleRecipeComponent;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * KubeJS 2101 的配方组件替代实现。
 * <p>
 * 2001 的 {@code RecipeComponent} 是“手写 read/write 方法”的回调式接口
 * （{@code componentType()/componentClass()/role()/write(RecipeJS, T)/read(RecipeJS, Object)}），
 * 2101 改成了 <b>Codec 驱动</b>：抽象方法只剩 {@code codec()} 与 {@code typeInfo()}，
 * 读写由 {@code RecipeComponent} 的 default 方法走 {@code Codec}（javap 实证：
 * {@code writeToJson} = {@code key.codec.encodeStart(recipe.type.event.registries.json(), value)}，
 * {@code readFromJson} = {@code registries.decodeJson(key.codec, json.get(key.name))}）。
 * <p>
 * 因此这里把 TACZ 的 JSON 结构（{@code result}/{@code materials}）实现为<b>原始 JSON 直通</b>的
 * {@link Codec}：解码用 {@code ops.convertTo(JsonOps.INSTANCE, ...)} 取出 {@code JsonElement}，
 * 编码用 {@code JsonOps.INSTANCE.convertTo(ops, json)} 写回。这样无论 KubeJS 传进来的是
 * {@code JsonOps} 还是带注册表访问的 {@code RegistryOps<JsonElement>} 都能工作。
 * <p>
 * <b>组件注册键（运行期字符串，javap 实证）</b>：
 * {@code RecipeComponentFactoryRegistry#register(RecipeComponent)} 的字节码是
 * {@code storage.simpleComponents.put(component.toString(), component)}，
 * 而 {@code SimpleRecipeComponent#toString()} 直接 {@code return this.componentType}，
 * 所以下面两个常量字符串就是运行期注册键，脚本/日志里出现的就是它们。
 */
public class GunSmithTableResultComponents {
    /**
     * {@code result} 组件的运行期注册键。
     * （2001 用的是 {@code event.register("gunSmithTableResultInfo", ...)}，键名随 2101 的
     * {@code SimpleRecipeComponent} 语义变为该字符串。）
     */
    public static final String RESULT_COMPONENT_TYPE = "gun_smith_table_result_info";

    /**
     * {@code materials} 组件的运行期注册键。
     */
    public static final String MATERIALS_COMPONENT_TYPE = "gun_smith_table_materials";

    /**
     * {@code result} 的 JSON 直通 Codec：结构与 TACZ 的
     * {@code GunSmithTableResultSerializer} 完全一致
     * （{@code type/id/count/nbt/item/group}），由 {@link GunSmithTableResultInfo} 自己解释。
     */
    public static final Codec<GunSmithTableResultInfo> RESULT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<GunSmithTableResultInfo, T>> decode(DynamicOps<T> ops, T input) {
            JsonElement element;
            try {
                element = ops.convertTo(JsonOps.INSTANCE, input);
            } catch (RuntimeException e) {
                return DataResult.error(() -> "Failed to read gun smith table result: " + e.getMessage());
            }
            if (element == null || !element.isJsonObject()) {
                return DataResult.error(() -> "Gun smith table result must be a json object, got " + element);
            }
            try {
                return DataResult.success(Pair.of(GunSmithTableResultInfo.createFromJson(element.getAsJsonObject()), input));
            } catch (RuntimeException e) {
                return DataResult.error(() -> "Failed to read gun smith table result: " + e.getMessage());
            }
        }

        @Override
        public <T> DataResult<T> encode(GunSmithTableResultInfo input, DynamicOps<T> ops, T prefix) {
            return DataResult.success(JsonOps.INSTANCE.convertTo(ops, input.toJson()));
        }

        @Override
        public String toString() {
            return RESULT_COMPONENT_TYPE;
        }
    };

    /**
     * 单个材料的直通 Codec。
     * <p>
     * TACZ 的 materials 元素结构是 {@code {"item": <原版 Ingredient 值>, "count": n}}，
     * 不是裸 Ingredient（所以不能用 2101 的 {@code IngredientComponent}）。
     * {@code item} 用 {@link Ingredient#CODEC} 解析（原版 1.21 该 codec 不依赖 RegistryOps：
     * {@code ItemValue} 走 {@code BuiltInRegistries.ITEM.holderByNameCodec()}，
     * {@code TagValue} 走 {@code TagKey.codec(Registries.ITEM)}，javap/字节码实证），
     * 这样单个材料可以独立于 KubeJS 的注册表上下文解码。
     * <p>
     * 缺少 {@code item} 必须报错：静默返回空 Ingredient 会让砧台配方吞材料。
     */
    public static final Codec<GunSmithTableIngredient> INGREDIENT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<GunSmithTableIngredient, T>> decode(DynamicOps<T> ops, T input) {
            JsonElement element;
            try {
                element = ops.convertTo(JsonOps.INSTANCE, input);
            } catch (RuntimeException e) {
                return DataResult.error(() -> "Failed to read gun smith table ingredient: " + e.getMessage());
            }
            if (element == null || !element.isJsonObject()) {
                return DataResult.error(() -> "Gun smith table ingredient must be a json object like {\"item\": ..., \"count\": n}, got " + element);
            }
            JsonObject json = element.getAsJsonObject();
            if (!json.has("item")) {
                return DataResult.error(() -> "Gun smith table ingredient is missing the required member 'item': " + json);
            }
            DataResult<Ingredient> ingredientResult = Ingredient.CODEC.parse(JsonOps.INSTANCE, json.get("item"));
            if (ingredientResult.error().isPresent()) {
                return DataResult.error(() -> "Invalid 'item' of gun smith table ingredient: " + ingredientResult.error().get().message());
            }
            Ingredient ingredient = ingredientResult.result().orElseThrow();
            int count = 1;
            if (json.has("count")) {
                count = Math.max(GsonHelper.getAsInt(json, "count"), 1);
            }
            return DataResult.success(Pair.of(new GunSmithTableIngredient(ingredient, count), input));
        }

        @Override
        public <T> DataResult<T> encode(GunSmithTableIngredient input, DynamicOps<T> ops, T prefix) {
            DataResult<JsonElement> ingredientJson = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, input.getIngredient());
            if (ingredientJson.error().isPresent()) {
                return DataResult.error(() -> "Failed to write 'item' of gun smith table ingredient: " + ingredientJson.error().get().message());
            }
            JsonObject json = new JsonObject();
            json.add("item", ingredientJson.result().orElseThrow());
            if (input.getCount() > 1) {
                json.addProperty("count", input.getCount());
            }
            return DataResult.success(JsonOps.INSTANCE.convertTo(ops, json));
        }

        @Override
        public String toString() {
            return "gun_smith_table_ingredient";
        }
    };

    public static final Codec<List<GunSmithTableIngredient>> MATERIALS_CODEC = INGREDIENT_CODEC.listOf();

    /** 对应 TACZ 的 {@code result} 键；role = OUTPUT，故 schema 里用 {@code outputKey("result")}。 */
    public static final RecipeComponent<GunSmithTableResultInfo> RESULT_INFO =
            new SimpleRecipeComponent<>(RESULT_COMPONENT_TYPE, RESULT_CODEC, TypeInfo.of(GunSmithTableResultInfo.class));

    /** 对应 TACZ 的 {@code materials} 键；role = INPUT，故 schema 里用 {@code inputKey("materials")}。 */
    public static final RecipeComponent<List<GunSmithTableIngredient>> MATERIALS =
            new SimpleRecipeComponent<>(MATERIALS_COMPONENT_TYPE, MATERIALS_CODEC,
                    TypeInfo.RAW_LIST.withParams(TypeInfo.of(GunSmithTableIngredient.class)));

    private GunSmithTableResultComponents() {
    }
}
