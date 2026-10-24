package com.tacz.guns.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventTargetType;
import dev.latvian.mods.kubejs.event.TargetedEventHandler;
import dev.latvian.mods.kubejs.script.ScriptTypeHolder;
import dev.latvian.mods.kubejs.script.ScriptTypePredicate;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * KubeJS 2101 事件注册适配。
 * <p>
 * 与 2001（{@code EventJS} + {@code EventHandler#extra(Extra.ID)}）的差异（均经 javap 实证）：
 * <ul>
 *     <li>{@code EventJS} 基类已删除，事件类改为实现 {@code KubeEvent}；</li>
 *     <li>{@code Extra} 已删除，按 ID 过滤改为 {@code EventHandler#supportsTarget(EventTargetType.ID)}，
 *     返回 {@code TargetedEventHandler<ResourceLocation>}，投递方法变为 {@code post(KubeEvent, E)} /
 *     {@code post(ScriptTypeHolder, E, KubeEvent)}；</li>
 *     <li>这里用 {@code supportsTarget}（targetRequired=false）而非 {@code requiredTarget}：
 *     字节码实证 {@code EventHandler#postInternal} 只在 {@code targetRequired} 时才禁止 null target，
 *     而 TACZ 有一部分事件（见 {@code GunKubeJSEvents} 里的 client 渲染事件）没有武器 ID，
 *     旧实现同样是 "声明了 ID extra 但允许投递 null"。用 {@code supportsTarget} 才能同时兼容
 *     “带 ID 的监听器”和“不带 ID 的监听器”（{@code postInternal} 的 eventContainers 分支）。</li>
 * </ul>
 */
public interface TimelessKubeJSEventRegister {
    default void init() {}

    Map<Class<? extends Event>, Consumer<Event>> getEventHandlers();

    ScriptTypePredicate getScriptType();

    default <E extends Event> TargetedEventHandler<ResourceLocation> registerTimelessEvent(
            String id,
            Class<? extends GunKubeJSEvents.GunEventJS<E>> eventJSClass,
            Class<E> eventClass,
            Function<E, ? extends GunKubeJSEvents.GunEventJS<E>> eventJSFactory
    ) {
        return registerTimelessEvent(id, eventJSClass, eventClass, eventJSFactory, false);
    }

    default <E extends Event> TargetedEventHandler<ResourceLocation> registerTimelessEvent(
            String id,
            Class<? extends GunKubeJSEvents.GunEventJS<E>> eventJSClass,
            Class<E> eventClass,
            Function<E, ? extends GunKubeJSEvents.GunEventJS<E>> eventJSFactory,
            boolean hasResult
    ) {
        TargetedEventHandler<ResourceLocation> handler = registerEventJS(id, eventJSClass, hasResult);
        registerEventHandler(eventClass, (event) -> {
            GunKubeJSEvents.GunEventJS<E> eventJS = eventJSFactory.apply((E) event);
            handler.post(eventJS, eventJS.getEventSubId());
        });
        return handler;
    }

    default <E extends Event> TargetedEventHandler<ResourceLocation> registerTimelessCommonEvent(
            String id,
            Class<? extends GunKubeJSEvents.GunEventJS<E>> eventJSClass,
            Class<E> eventClass,
            Function<E, ? extends GunKubeJSEvents.GunEventJS<E>> eventJSFactory
    ) {
        return registerTimelessCommonEvent(id, eventJSClass, eventClass, eventJSFactory, false);
    }

    default <E extends Event> TargetedEventHandler<ResourceLocation> registerTimelessCommonEvent(
            String id,
            Class<? extends GunKubeJSEvents.GunEventJS<E>> eventJSClass,
            Class<E> eventClass,
            Function<E, ? extends GunKubeJSEvents.GunEventJS<E>> eventJSFactory,
            boolean hasResult
    ) {
        TargetedEventHandler<ResourceLocation> handler = registerEventJS(id, eventJSClass, hasResult);
        registerEventHandler(eventClass, (event) -> {
            GunKubeJSEvents.GunEventJS<E> eventJS = eventJSFactory.apply((E) event);
            ScriptTypeHolder holder = eventJS.getTypeHolder();
            if (holder != null) {
                handler.post(holder, eventJS.getEventSubId(), eventJS);
            } else {
                throw new IllegalArgumentException("You must specify which script type to post event to");
            }
        });
        return handler;
    }

    default <E extends Event> TargetedEventHandler<ResourceLocation> registerEventJS(
            String id,
            Class<? extends GunKubeJSEvents.GunEventJS<E>> eventJSClass,
            boolean hasResult
    ) {
        EventHandler handler = GunKubeJSEvents.GROUP.add(id, getScriptType(), () -> eventJSClass);
        TargetedEventHandler<ResourceLocation> targeted = handler.supportsTarget(EventTargetType.ID);
        return hasResult ? targeted.hasResult() : targeted;
    }

    <E extends Event> void registerEventHandler(Class<E> eventClass, Consumer<Event> eventPoster);

    default boolean postKubeJSEvent(Event event) {
        Consumer<Event> eventHandler = getEventHandlers().get(event.getClass());
        if (eventHandler != null) {
            eventHandler.accept(event);
            return false;
        } else {
            return true;
        }
    }
}
