package com.tacz.guns.compat.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputePlayerAimStateEventHandler;
import com.github.exopandora.shouldersurfing.api.event.IEventBus;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingRegistrar;
import com.github.exopandora.shouldersurfing.legacy.mixinduck.IShoulderSurfingLegacyPlugin;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * ShoulderSurfing 5.x 把插件入口从 {@code register(IShoulderSurfingRegistrar)} 改成了
 * {@code register(com.github.exopandora.shouldersurfing.api.event.IEventBus)}：
 * "手持自适应物品（枪械）就让肩部视角接管" 这个语义在新 API 里是监听
 * {@code ComputePlayerAimStateEvent} 并 setResult(true)。
 * <p>
 * 注意：本类的加载入口由 {@code shouldersurfing_plugin.json} 决定。
 * 该文件目前仍是旧格式（{@code "entrypoint"} 键），ShoulderSurfing 的 PluginLoader 在旧格式下会把入口
 * checkcast 成 {@code IShoulderSurfingLegacyPlugin}（会打一条 "is loaded in legacy mode!" 警告），
 * 新格式（{@code "entrypoints"} 数组）则 checkcast 成 {@code IShoulderSurfingPlugin}。
 * 这里两个接口都实现，两个入口注册等价行为，因此无论 json 是哪种格式都能工作。
 */
public class ShoulderSurfingPlugin implements IShoulderSurfingPlugin, IShoulderSurfingLegacyPlugin {
	private static boolean isGun(ItemStack stack) {
		return stack.getItem() instanceof IGun;
	}

	private static boolean isGun(LivingEntity entity) {
		// 与旧版 Predicate<ItemStack> 默认适配器的语义保持一致：主手或副手任意一只手拿着枪械即可。
		return isGun(entity.getMainHandItem()) || isGun(entity.getOffhandItem());
	}

	@Override
	public void register(IEventBus eventBus) {
		ComputePlayerAimStateEventHandler handler = event -> {
			if (!event.getResult()) {
				event.setResult(isGun(event.getEntity()));
			}
		};
		eventBus.register(handler);
	}

	/**
	 * 旧版插件入口（ShoulderSurfing 内部通过 LegacyPluginAdapter 桥接到上面的新入口）。
	 */
	@Override
	public void register(IShoulderSurfingRegistrar registrar) {
		registrar.registerAdaptiveItemCallback(itemStack -> itemStack.getItem() instanceof IGun);
	}
}
