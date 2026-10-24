package com.tacz.guns.compat.oculus;

import com.tacz.guns.compat.oculus.legacy.OculusCompatLegacy;
import com.tacz.guns.compat.oculus.newly.OculusCompatNewly;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 光影模组兼容层：Oculus(1.20.1) / Iris(1.21.1)。
 * <p>
 * 移植说明：{@code libs/} 里只有 1.20.1 的 oculus jar，而且构建脚本没有把它放进 compile classpath
 * （注释「仍待处理: oculus / oculus_legacy」），1.21.1 对应的光影模组是 Iris，包名也从
 * {@code net.coderbot.*} 变成了 {@code net.irisshaders.*}。因此本类不再在编译期引用光影模组的任何类型，
 * 全部改成运行时按类名探测 + 反射；找不到模组 / 找不到类 / 反射失败时一律静默降级为 false。
 */
public final class OculusCompat {
    /** 1.20.1 光影模组 modid（Oculus）。 */
    public static final String OCULUS_MOD_ID = "oculus";
    /** 1.21.1 光影模组 modid（Iris）。 */
    public static final String IRIS_MOD_ID = "iris";

    /** 1.21.1 Iris / Oculus 1.7.0+ 的 API 类名。 */
    private static final String[] IRIS_API_CLASS_NAMES = {
            "net.irisshaders.iris.api.v0.IrisApi",
            "net.coderbot.iris.api.v0.IrisApi"
    };

    private static boolean loaded;
    private static Function<MultiBufferSource.BufferSource, Boolean> endBatchFunction = bufferSource -> false;
    private static Supplier<Boolean> isRenderShadowSupplier = () -> false;

    /** IrisApi 的静态实例获取方法 / isShaderPackInUse 实例方法（探测失败为 null）。 */
    private static Method irisApiGetInstance;
    private static Method isShaderPackInUse;
    private static boolean irisApiProbed;

    private OculusCompat() {
    }

    public static void initCompat() {
        loaded = ModList.get().isLoaded(OCULUS_MOD_ID) || ModList.get().isLoaded(IRIS_MOD_ID);
        if (!loaded) {
            endBatchFunction = bufferSource -> false;
            isRenderShadowSupplier = () -> false;
            return;
        }
        initIrisApi();
        // 用“运行时类是否存在”替代原来对 oculus 模组版本号(>=1.7.0)的比较：
        // 1.21.1 的 iris 版本号与 oculus 1.7.0 没有可比性，而包名区分(coderbot/irisshaders)才是真正的差异点。
        if (OculusCompatNewly.isAvailable()) {
            endBatchFunction = OculusCompatNewly::endBatch;
            isRenderShadowSupplier = OculusCompatNewly::isRenderShadow;
        } else if (OculusCompatLegacy.isAvailable()) {
            endBatchFunction = OculusCompatLegacy::endBatch;
            isRenderShadowSupplier = OculusCompatLegacy::isRenderShadow;
        } else {
            endBatchFunction = bufferSource -> false;
            isRenderShadowSupplier = () -> false;
        }
    }

    private static void initIrisApi() {
        if (irisApiProbed) {
            return;
        }
        irisApiProbed = true;
        for (String className : IRIS_API_CLASS_NAMES) {
            try {
                Class<?> irisApi = Class.forName(className);
                irisApiGetInstance = irisApi.getMethod("getInstance");
                isShaderPackInUse = irisApi.getMethod("isShaderPackInUse");
                return;
            } catch (Throwable ignored) {
                // 换下一个候选包名；都不在就降级
            }
        }
    }

    /** 是否有光影模组（Oculus 或 Iris）在运行。 */
    public static boolean isCompatModLoaded() {
        return loaded;
    }

    public static boolean isRenderShadow() {
        return loaded && isRenderShadowSupplier.get();
    }

    public static boolean isUsingRenderPack() {
        if (!loaded || isShaderPackInUse == null || irisApiGetInstance == null) {
            return false;
        }
        try {
            Object irisApi = irisApiGetInstance.invoke(null);
            return irisApi != null && Boolean.TRUE.equals(isShaderPackInUse.invoke(irisApi));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean endBatch(MultiBufferSource.BufferSource bufferSource) {
        return loaded && bufferSource != null && endBatchFunction.apply(bufferSource);
    }
}
