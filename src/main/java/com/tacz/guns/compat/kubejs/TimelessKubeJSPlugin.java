package com.tacz.guns.compat.kubejs;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.compat.kubejs.custom.CustomGunItemBuilder;
import com.tacz.guns.compat.kubejs.events.GunKubeJSEvents;
import com.tacz.guns.compat.kubejs.events.TimelessClientEvents;
import com.tacz.guns.compat.kubejs.events.TimelessCommonEvents;
import com.tacz.guns.compat.kubejs.events.TimelessServerEvents;
import com.tacz.guns.compat.kubejs.recipe.GunSmithTableResultComponents;
import com.tacz.guns.compat.kubejs.recipe.TimelessGunSmithTableRecipeSchema;
import com.tacz.guns.compat.kubejs.util.GunSmithTableResultInfo;
import com.tacz.guns.compat.kubejs.util.TimelessItemWrapper;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RecipeComponentFactoryRegistry;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.TypeWrapperRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * KubeJS 2101 插件入口。
 * <p>
 * 注册方式不变：{@code src/main/resources/kubejs.plugins.txt} 里写着本类全名。
 * 2001 的 {@code dev.latvian.mods.kubejs.KubeJSPlugin} 是抽象类，2101 改成
 * {@code dev.latvian.mods.kubejs.plugin.KubeJSPlugin} <b>接口</b>（javap 实证，所有方法 default），
 * 各注册回调的签名也全部换过：
 * <ul>
 *   <li>{@code init()} 里的 {@code RegistryInfo.ITEM.addType(...)} →
 *       {@link #registerBuilderTypes(BuilderTypeRegistry)}
 *       （{@code registry.of(ResourceKey<Registry<T>>, Consumer<Callback<T>>)} +
 *       {@code Callback.add(String, Class<? extends BuilderBase<? extends T>>, BuilderFactory)}）；</li>
 *   <li>{@code registerEvents()}（无参）→ {@link #registerEvents(EventGroupRegistry)}
 *       （{@code EventGroup} 自己已无 {@code register()} 方法，改由注册表登记）；</li>
 *   <li>{@code registerBindings(BindingsEvent)} → {@link #registerBindings(BindingRegistry)}；</li>
 *   <li>{@code registerTypeWrappers(ScriptType, TypeWrappers)} → {@link #registerTypeWrappers(TypeWrapperRegistry)}
 *       （{@code register(Class<T>, ContextFromFunction<T>)}，{@code ContextFromFunction} 是
 *       2 参 {@code BiFunction<Context,Object,T>}）；</li>
 *   <li>{@code registerRecipeSchemas(RegisterRecipeSchemasEvent)} → {@link #registerRecipeSchemas(RecipeSchemaRegistry)}；</li>
 *   <li>{@code registerRecipeComponents(RecipeComponentFactoryRegistryEvent)} →
 *       {@link #registerRecipeComponents(RecipeComponentFactoryRegistry)}
 *       （{@code register(RecipeComponent)} 的注册键 = {@code component.toString()} = 其 componentType）。</li>
 * </ul>
 * <p>
 * <b>配方 schema 注册链路（Lead 要求实证到调用点）</b>：
 * {@link #registerRecipeSchemas(RecipeSchemaRegistry)} 的形参就是注册表本身，调用
 * {@code registry.namespace("tacz").register("gun_smith_table_crafting", SCHEMA)}
 * （javap：{@code RecipeSchemaRegistry#namespace(String)} 返回 {@code RecipeNamespace}，
 * {@code RecipeNamespace#register(String, RecipeSchema)} 返回自身）。
 * 即：第 1 个参数是方法接收者（{@code RecipeSchemaRegistry}），
 * {@code namespace(...)} 的第 1 参 = 命名空间 {@code "tacz"}，
 * {@code register(...)} 的第 1 参 = 配方名字 {@code "gun_smith_table_crafting"}，第 2 参 = schema 实例。
 * 完整 recipe type = {@code tacz:gun_smith_table_crafting}。
 */
public class TimelessKubeJSPlugin implements KubeJSPlugin {
    public static final String KUBEJS_MODID = "kubejs";
    private static final Map<String, DeferredHolder<Item, ? extends AbstractGunItem>> GUNTYPE_REGISTER_MAP = new HashMap<>();

    @Override
    public void registerBuilderTypes(BuilderTypeRegistry registry) {
        // 2001: RegistryInfo.ITEM.addType("tacz_gun", CustomGunItemBuilder.class, CustomGunItemBuilder::new)
        registry.of(Registries.ITEM,
                callback -> callback.add("tacz_gun", CustomGunItemBuilder.class, CustomGunItemBuilder::new));
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        //提早加载防止出现问题
        TimelessCommonEvents.INSTANCE.init();
        TimelessServerEvents.INSTANCE.init();
        TimelessClientEvents.INSTANCE.init();
        // 2001: GunKubeJSEvents.GROUP.register(); 2101: EventGroup 没有 register()，改由 EventGroupRegistry 登记
        registry.register(GunKubeJSEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry event) {
        event.add("TimelessItem", TimelessItemWrapper.class);
        event.add("GunProperties", GunProperties.class);
        event.add("GunSmithTableResultInfo", GunSmithTableResultInfo.class);
    }

    @Override
    public void registerTypeWrappers(TypeWrapperRegistry registry) {
        // 2101: TypeWrapperRegistry#register(Class<T>, ContextFromFunction<T>)；
        // 必须显式强转，否则与 register(Class<T>, RegistriesFromFunction<T>) 二义（javac 实测）
        registry.register(GunSmithTableResultInfo.class,
                (TypeWrapperRegistry.ContextFromFunction<GunSmithTableResultInfo>) (context, from) -> GunSmithTableResultInfo.of(from));
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        registry.namespace(GunMod.MOD_ID)
                .register(TimelessGunSmithTableRecipeSchema.RECIPE_TYPE, TimelessGunSmithTableRecipeSchema.SCHEMA);
    }

    @Override
    public void registerRecipeComponents(RecipeComponentFactoryRegistry registry) {
        // 2101 的 register(RecipeComponent) 以 component.toString() 为键，
        // 即 GunSmithTableResultComponents 里的 RESULT_COMPONENT_TYPE / MATERIALS_COMPONENT_TYPE。
        registry.register(GunSmithTableResultComponents.RESULT_INFO);
        registry.register(GunSmithTableResultComponents.MATERIALS);
    }

    public static void registerGunType(String typeName, DeferredHolder<Item, ? extends AbstractGunItem> itemHolder) {
        GUNTYPE_REGISTER_MAP.put(typeName, itemHolder);
    }

    @SubscribeEvent
    public void onItemRegister(RegisterEvent event) {
        if (ModList.get().isLoaded(KUBEJS_MODID) && event.getRegistryKey().equals(Registries.ITEM)) {
            for (Map.Entry<String, DeferredHolder<Item, ? extends AbstractGunItem>> entry : GUNTYPE_REGISTER_MAP.entrySet()) {
                GunItemManager.registerGunItem(entry.getKey(), entry.getValue());
            }
        }
    }
}
