package com.tacz.guns.compat.oculus.legacy;

import net.minecraft.client.renderer.MultiBufferSource;

import java.lang.reflect.Method;

/**
 * Oculus 1.6.x 及更早的兼容实现（包名 {@code net.coderbot.*}）。
 * <p>
 * 移植说明：和 {@code OculusCompatNewly} 一样，oculus jar 不在 compile classpath 里，因此不能直接
 * import {@code net.coderbot.batchedentityrendering.impl.FullyBufferedMultiBufferSource} /
 * {@code net.coderbot.iris.shadows.ShadowRenderingState}，改为运行时按类名探测 + 反射；
 * 探测不到或反射失败时一律静默返回 false（不会抛异常拖垮客户端启动）。
 * <p>
 * 注意：{@code net.coderbot.*} 是 1.20.1 时代 Oculus 的包名。TACZ 的 1.21.1 版本不会和 1.20.1 的
 * Oculus 同时存在，这条路径实际只在“未来有模组把 coderbot 包名移植到 1.21.1”时才有意义，属于兜底分支。
 */
public final class OculusCompatLegacy {
    private static final String SHADOW_RENDERING_STATE_CLASS = "net.coderbot.iris.shadows.ShadowRenderingState";
    private static final String FULLY_BUFFERED_MULTI_BUFFER_SOURCE_CLASS = "net.coderbot.batchedentityrendering.impl.FullyBufferedMultiBufferSource";

    private static final Class<?> SHADOW_RENDERING_STATE = loadClass(SHADOW_RENDERING_STATE_CLASS);
    private static final Method ARE_SHADOWS_CURRENTLY_BEING_RENDERED = findMethod(SHADOW_RENDERING_STATE, "areShadowsCurrentlyBeingRendered");

    private static final Class<?> FULLY_BUFFERED_MULTI_BUFFER_SOURCE = loadClass(FULLY_BUFFERED_MULTI_BUFFER_SOURCE_CLASS);
    /** 同 {@code OculusCompatNewly}：官方名 endBatch / 1.20.1 SRG 名 m_109911_ 都试。 */
    private static final Method END_BATCH = findMethod(FULLY_BUFFERED_MULTI_BUFFER_SOURCE, "endBatch", "m_109911_");

    private OculusCompatLegacy() {
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, OculusCompatLegacy.class.getClassLoader());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String... methodNames) {
        if (type == null) {
            return null;
        }
        for (String methodName : methodNames) {
            try {
                return type.getMethod(methodName);
            } catch (Throwable ignored) {
                // 试下一个名字
            }
        }
        return null;
    }

    /** 运行时是否真的存在 Oculus 1.6.x 的这些类。 */
    public static boolean isAvailable() {
        return SHADOW_RENDERING_STATE != null || FULLY_BUFFERED_MULTI_BUFFER_SOURCE != null;
    }

    public static boolean isRenderShadow() {
        Method method = ARE_SHADOWS_CURRENTLY_BEING_RENDERED;
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean endBatch(MultiBufferSource.BufferSource bufferSource) {
        Method method = END_BATCH;
        // 等价于原来的 instanceof FullyBufferedMultiBufferSource（必须按类判断，不能按方法声明类判断）
        if (method == null || FULLY_BUFFERED_MULTI_BUFFER_SOURCE == null || !FULLY_BUFFERED_MULTI_BUFFER_SOURCE.isInstance(bufferSource)) {
            return false;
        }
        try {
            method.invoke(bufferSource);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
