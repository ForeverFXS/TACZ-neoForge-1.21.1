package com.tacz.guns.compat.oculus.newly;

import net.minecraft.client.renderer.MultiBufferSource;

import java.lang.reflect.Method;

/**
 * Iris / Oculus 1.7.0+ 兼容实现（包名 {@code net.irisshaders.*}）。
 * <p>
 * 移植说明：1.21.1 的光影模组是 Iris，且 oculus jar 没有进 compile classpath，所以这里不能直接
 * import {@code net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource} /
 * {@code net.irisshaders.iris.shadows.ShadowRenderingState}，改为运行时按类名探测 + 反射。
 * 类/方法探测不到，或反射调用抛任何异常时，{@link #isRenderShadow()} / {@link #endBatch} 一律静默返回 false
 * （调用方会退回原版 {@code bufferSource.endBatch(renderType)} 路径，不会抛异常、不会拖垮启动）。
 */
public final class OculusCompatNewly {
    private static final String SHADOW_RENDERING_STATE_CLASS = "net.irisshaders.iris.shadows.ShadowRenderingState";
    private static final String FULLY_BUFFERED_MULTI_BUFFER_SOURCE_CLASS = "net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource";

    private static final Class<?> SHADOW_RENDERING_STATE = loadClass(SHADOW_RENDERING_STATE_CLASS);
    private static final Method ARE_SHADOWS_CURRENTLY_BEING_RENDERED = findMethod(SHADOW_RENDERING_STATE, "areShadowsCurrentlyBeingRendered");

    private static final Class<?> FULLY_BUFFERED_MULTI_BUFFER_SOURCE = loadClass(FULLY_BUFFERED_MULTI_BUFFER_SOURCE_CLASS);
    /**
     * {@code endBatch()} 是 Minecraft 的方法：1.21.1 NeoForge 运行时用官方名 {@code endBatch}，
     * 1.20.1 Forge 运行时是 SRG 名 {@code m_109911_}（javap 实测 oculus 1.7.0 jar 里只有
     * {@code m_109911_}，没有 {@code endBatch}），所以两个名字都试。
     */
    private static final Method END_BATCH = findMethod(FULLY_BUFFERED_MULTI_BUFFER_SOURCE, "endBatch", "m_109911_");

    private OculusCompatNewly() {
    }

    private static Class<?> loadClass(String className) {
        try {
            // initialize=false：只探测类型，不触发光影模组的静态初始化
            return Class.forName(className, false, OculusCompatNewly.class.getClassLoader());
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

    /** 运行时是否真的存在 Iris/Oculus 1.7+ 的这些类。 */
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
        // 等价于原来的 instanceof FullyBufferedMultiBufferSource：
        // 注意必须用“类”判断，不能用 method.getDeclaringClass() —— endBatch 可能是从
        // MultiBufferSource.BufferSource 继承来的，那样会匹配到任意 BufferSource。
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
